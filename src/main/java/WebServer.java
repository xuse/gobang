import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Point;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内嵌HTTP服务器，支持网页远程对局。
 * 使用JDK自带的 com.sun.net.httpserver，零外部依赖。
 *
 * 对局模式：
 * - PVE_BLACK: 人执黑 vs AI
 * - PVE_WHITE: 人执白 vs AI
 * - AUTO: AI vs AI 自动对局
 * - PVP: 双人远程对局（邀请码机制）
 *
 * 双人对局流程：
 * 1. 发起者调用 /api/start 选择 PVP 模式和执黑/白，生成邀请码
 * 2. 对方调用 /api/join 输入邀请码加入为对手
 * 3. 不输入邀请码的 /api/join 为观众
 */
public class WebServer {

	private HttpServer server;
	final Chess chess;
	private final Runnable repaintCallback;
	private int port;

	// 对局状态
	enum GameMode { NONE, PVE_BLACK, PVE_WHITE, AUTO, PVP }
	enum Phase { IDLE, WAITING, PLAYING, OVER }

	volatile GameMode gameMode = GameMode.NONE;
	volatile Phase phase = Phase.IDLE;
	private String inviteCode = null;

	// 角色管理
	private String blackToken = null;
	private String whiteToken = null;
	private final Map<String, String> tokenToRole = new ConcurrentHashMap<>();

	// SSE客户端列表
	private final List<SseClient> sseClients = new CopyOnWriteArrayList<>();

	// AI难度（与Swing菜单同步）
	private Class<? extends AI> aiClass = SmartSearchAI.class;
	private String aiLevelName = "困难";

	public WebServer(Chess chess, Runnable repaintCallback) {
		this.chess = chess;
		this.repaintCallback = repaintCallback;
	}

	public int getPort() { return port; }

	public synchronized void start(int port) throws IOException {
		if (server != null) return;
		this.port = port;
		server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/", new PageHandler());
		server.createContext("/api/state", new StateHandler());
		server.createContext("/api/move", new MoveHandler());
		server.createContext("/api/join", new JoinHandler());
		server.createContext("/api/events", new SseHandler());
		server.createContext("/api/start", new StartHandler());
		server.createContext("/api/ranking", new RankingHandler());
		server.setExecutor(null);
		server.start();
		resetSession();
		System.out.println("网页服务已启动: http://localhost:" + port);
	}

	public synchronized void stop() {
		if (server == null) return;
		for (SseClient c : sseClients) c.close();
		sseClients.clear();
		server.stop(0);
		server = null;
		resetSession();
		System.out.println("网页服务已停止");
	}

	public boolean isRunning() { return server != null; }

	private void resetSession() {
		gameMode = GameMode.NONE;
		phase = Phase.IDLE;
		inviteCode = null;
		blackToken = null;
		whiteToken = null;
		tokenToRole.clear();
	}

	public void setAiClass(Class<? extends AI> clz) { this.aiClass = clz; }

	/** 推送SSE事件给所有客户端 */
	public void broadcastState() {
		String json = buildStateJson();
		String event = "data: " + json + "\n\n";
		for (SseClient c : sseClients) {
			if (!c.send(event)) sseClients.remove(c);
		}
	}


	private String buildStateJson() {
		int[][] board = chess.getTable();
		StringBuilder sb = new StringBuilder(1024);
		sb.append("{\"width\":").append(chess.width);
		sb.append(",\"height\":").append(chess.height);
		sb.append(",\"board\":[");
		for (int j = 0; j < chess.height; j++) {
			if (j > 0) sb.append(',');
			sb.append('[');
			for (int i = 0; i < chess.width; i++) {
				if (i > 0) sb.append(',');
				sb.append(board[i][j]);
			}
			sb.append(']');
		}
		sb.append(']');
		Player next = chess.getNext();
		sb.append(",\"next\":").append(next == null ? "null" : ("\"" + next.name() + "\""));
		sb.append(",\"winner\":");
		if (chess.winner == null) sb.append("null");
		else sb.append("\"").append(chess.winner.name()).append("\"");
		sb.append(",\"steps\":").append(chess.his.count());
		sb.append(",\"forbidden\":").append(chess.isForbiddenMoveRule());
		sb.append(",\"gameMode\":\"").append(gameMode.name()).append("\"");
		sb.append(",\"aiLevel\":\"").append(aiLevelName).append("\"");
		sb.append(",\"phase\":\"").append(phase.name()).append("\"");

		Point last = chess.his.getLast();
		if (last != null) {
			sb.append(",\"lastMove\":{\"x\":").append(last.x).append(",\"y\":").append(last.y).append('}');
		} else {
			sb.append(",\"lastMove\":null");
		}

		Point[] winPts = chess.getWinPoints();
		if (winPts != null) {
			sb.append(",\"winPoints\":[");
			for (int i = 0; i < winPts.length; i++) {
				if (i > 0) sb.append(',');
				sb.append("{\"x\":").append(winPts[i].x).append(",\"y\":").append(winPts[i].y).append('}');
			}
			sb.append(']');
		} else {
			sb.append(",\"winPoints\":null");
		}
		sb.append('}');
		return sb.toString();
	}

	/** AI自动落子（在后台线程执行） */
	private void aiMoveAsync() {
		Thread t = new Thread(() -> {
			try { Thread.sleep(200); } catch (InterruptedException ignored) {}
			synchronized (chess) {
				if (chess.getNext() == null) return;
				chess.computerMove();
			}
			phase = chess.getNext() == null ? Phase.OVER : Phase.PLAYING;
			broadcastState();
			if (repaintCallback != null) repaintCallback.run();
			// AUTO模式下持续下棋
			if (gameMode == GameMode.AUTO && chess.getNext() != null) {
				aiMoveAsync();
			}
		});
		t.setDaemon(true);
		t.start();
	}

	/** 人类落子后，如果对手是AI则自动应答 */
	private void checkAiResponse() {
		if (gameMode == GameMode.PVE_BLACK && chess.getNext() == Player.WHITE && chess.getNext() != null) {
			aiMoveAsync();
		} else if (gameMode == GameMode.PVE_WHITE && chess.getNext() == Player.BLACK && chess.getNext() != null) {
			aiMoveAsync();
		}
	}

	private void respond(HttpExchange ex, int code, String contentType, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().set("Content-Type", contentType);
		ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
		ex.sendResponseHeaders(code, bytes.length);
		try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
	}

	private String readBody(HttpExchange ex) throws IOException {
		return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
	}

	private String generateInviteCode() {
		Random r = new Random();
		return String.format("%04d", r.nextInt(10000));
	}


	// ========== HTTP Handlers ==========

	/**
	 * POST /api/start — 开始新游戏
	 * body: {"mode":"PVE_BLACK"|"PVE_WHITE"|"AUTO"|"PVP", "color":"BLACK"|"WHITE"}
	 * PVP模式需要color参数（发起者选择执黑还是执白）
	 * 返回: {"ok":true, "token":"xxx", "role":"BLACK", "inviteCode":"1234"}
	 */
	class StartHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange ex) throws IOException {
			if ("OPTIONS".equals(ex.getRequestMethod())) {
				ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
				ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
				ex.sendResponseHeaders(204, -1);
				return;
			}
			if (!"POST".equals(ex.getRequestMethod())) {
				respond(ex, 405, "application/json", "{\"error\":\"Method not allowed\"}");
				return;
			}
			String body = readBody(ex);
			String mode = extractJsonString(body, "mode");
			String color = extractJsonString(body, "color");
			String level = extractJsonString(body, "level");
			int forbiddenVal = extractJsonInt(body, "forbidden");

			if (mode == null) {
				respond(ex, 400, "application/json", "{\"error\":\"缺少mode参数\"}");
				return;
			}

			// 解析AI难度
			Class<? extends AI> selectedAiClass = aiClass;
			String selectedLevelName = aiLevelName;
			if (level != null) {
				switch (level) {
					case "BEGINNER": selectedAiClass = RandomAI.class; selectedLevelName = "入门"; break;
					case "EASY": selectedAiClass = AI.Default.class; selectedLevelName = "简单"; break;
					case "NORMAL": selectedAiClass = SmartEvalAI.class; selectedLevelName = "普通"; break;
					case "HARD": selectedAiClass = SmartSearchAI.class; selectedLevelName = "困难"; break;
				}
			}

			// 解析禁手规则
			boolean forbidden = (forbiddenVal < 0) ? chess.forbiddenMoveRule : (forbiddenVal == 1);

			synchronized (WebServer.this) {
				resetSession();
				aiLevelName = selectedLevelName;
				String token = UUID.randomUUID().toString().substring(0, 8);
				String role;
				String code = null;

				// 设置禁手规则
				chess.forbiddenMoveRule = forbidden;

				switch (mode) {
					case "PVE_BLACK":
						gameMode = GameMode.PVE_BLACK;
						Player.BLACK.setAi(null);
						Player.WHITE.setAi(chess.createAI(selectedAiClass));
						Player.WHITE.human = false;
						chess.initGame(true, false, selectedAiClass);
						blackToken = token;
						role = "BLACK";
						tokenToRole.put(token, role);
						phase = Phase.PLAYING;
						break;
					case "PVE_WHITE":
						gameMode = GameMode.PVE_WHITE;
						Player.BLACK.setAi(chess.createAI(selectedAiClass));
						Player.BLACK.human = false;
						Player.WHITE.setAi(null);
						chess.initGame(false, true, selectedAiClass);
						whiteToken = token;
						role = "WHITE";
						tokenToRole.put(token, role);
						phase = Phase.PLAYING;
						break;
					case "AUTO":
						gameMode = GameMode.AUTO;
						Player.BLACK.setAi(chess.createAI(selectedAiClass));
						Player.WHITE.setAi(chess.createAI(selectedAiClass));
						chess.initGame(false, false);
						role = "SPECTATOR";
						tokenToRole.put(token, role);
						phase = Phase.PLAYING;
						// 启动自动对局
						broadcastState();
						if (repaintCallback != null) repaintCallback.run();
						aiMoveAsync();
						respond(ex, 200, "application/json",
								"{\"ok\":true,\"token\":\"" + token + "\",\"role\":\"" + role + "\"}");
						return;
					case "PVP":
						gameMode = GameMode.PVP;
						chess.initGame(true, true);
						code = generateInviteCode();
						inviteCode = code;
						if ("WHITE".equals(color)) {
							whiteToken = token;
							role = "WHITE";
						} else {
							blackToken = token;
							role = "BLACK";
						}
						tokenToRole.put(token, role);
						phase = Phase.WAITING;
						break;
					default:
						respond(ex, 400, "application/json", "{\"error\":\"未知模式: " + mode + "\"}");
						return;
				}

				broadcastState();
				if (repaintCallback != null) repaintCallback.run();

				StringBuilder resp = new StringBuilder();
				resp.append("{\"ok\":true,\"token\":\"").append(token)
					.append("\",\"role\":\"").append(role).append("\"");
				if (code != null) {
					resp.append(",\"inviteCode\":\"").append(code).append("\"");
				}
				resp.append('}');
				respond(ex, 200, "application/json", resp.toString());
			}
		}
	}

	/**
	 * POST /api/join — 加入对局
	 * body: {"inviteCode":"1234"} 或 {} (观众)
	 * 返回: {"token":"xxx", "role":"BLACK"|"WHITE"|"SPECTATOR"}
	 */
	class JoinHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange ex) throws IOException {
			if ("OPTIONS".equals(ex.getRequestMethod())) {
				ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
				ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
				ex.sendResponseHeaders(204, -1);
				return;
			}
			if (!"POST".equals(ex.getRequestMethod())) {
				respond(ex, 405, "application/json", "{\"error\":\"Method not allowed\"}");
				return;
			}
			String body = readBody(ex);
			String code = extractJsonString(body, "inviteCode");
			String token = UUID.randomUUID().toString().substring(0, 8);
			String role;

			synchronized (WebServer.this) {
				if (gameMode == GameMode.PVP && phase == Phase.WAITING && code != null
						&& code.equals(inviteCode)) {
					// 邀请码匹配，分配对手角色
					if (blackToken == null) {
						blackToken = token;
						role = "BLACK";
					} else if (whiteToken == null) {
						whiteToken = token;
						role = "WHITE";
					} else {
						role = "SPECTATOR";
					}
					tokenToRole.put(token, role);
					if (blackToken != null && whiteToken != null) {
						phase = Phase.PLAYING;
					}
					broadcastState();
					if (repaintCallback != null) repaintCallback.run();
				} else {
					// 无邀请码或不匹配 -> 观众
					role = "SPECTATOR";
					tokenToRole.put(token, role);
				}
			}

			respond(ex, 200, "application/json",
					"{\"token\":\"" + token + "\",\"role\":\"" + role + "\"}");
		}
	}

	class StateHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange ex) throws IOException {
			respond(ex, 200, "application/json", buildStateJson());
		}
	}

	class MoveHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange ex) throws IOException {
			if ("OPTIONS".equals(ex.getRequestMethod())) {
				ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
				ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
				ex.sendResponseHeaders(204, -1);
				return;
			}
			if (!"POST".equals(ex.getRequestMethod())) {
				respond(ex, 405, "application/json", "{\"error\":\"Method not allowed\"}");
				return;
			}
			String body = readBody(ex);
			String token = extractJsonString(body, "token");
			int x = extractJsonInt(body, "x");
			int y = extractJsonInt(body, "y");

			if (token == null || x < 0 || y < 0) {
				respond(ex, 400, "application/json", "{\"error\":\"参数无效\"}");
				return;
			}

			String role = tokenToRole.get(token);
			if (role == null || "SPECTATOR".equals(role)) {
				respond(ex, 403, "application/json", "{\"error\":\"观众不能落子\"}");
				return;
			}

			if (phase != Phase.PLAYING) {
				respond(ex, 400, "application/json", "{\"error\":\"当前不在对局中\"}");
				return;
			}

			Player next = chess.getNext();
			if (next == null) {
				respond(ex, 400, "application/json", "{\"error\":\"游戏已结束\"}");
				return;
			}

			if (("BLACK".equals(role) && next != Player.BLACK) ||
				("WHITE".equals(role) && next != Player.WHITE)) {
				respond(ex, 400, "application/json", "{\"error\":\"还没轮到你\"}");
				return;
			}

			if (x >= chess.width || y >= chess.height || chess.getTable()[x][y] != 0) {
				respond(ex, 400, "application/json", "{\"error\":\"无效落子位置\"}");
				return;
			}

			if (chess.isForbiddenMoveRule() && next == Player.BLACK && chess.isForbidden(x, y)) {
				respond(ex, 400, "application/json", "{\"error\":\"此处为禁手（三三/四四/长连）\"}");
				return;
			}

			chess.doMove(new Point(x, y));
			if (chess.getNext() == null) phase = Phase.OVER;
			broadcastState();
			if (repaintCallback != null) repaintCallback.run();

			// 人机模式下AI自动应答
			checkAiResponse();

			respond(ex, 200, "application/json", "{\"ok\":true}");
		}
	}

	class SseHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange ex) throws IOException {
			ex.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
			ex.getResponseHeaders().set("Cache-Control", "no-cache");
			ex.getResponseHeaders().set("Connection", "keep-alive");
			ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
			ex.sendResponseHeaders(200, 0);

			OutputStream os = ex.getResponseBody();
			SseClient client = new SseClient(os);
			sseClients.add(client);
			client.send("data: " + buildStateJson() + "\n\n");

			Thread keepAlive = new Thread(() -> {
				try {
					while (client.isOpen()) {
						Thread.sleep(15000);
						if (!client.send(": heartbeat\n\n")) break;
					}
				} catch (InterruptedException ignored) {
				} finally {
					sseClients.remove(client);
					client.close();
				}
			});
			keepAlive.setDaemon(true);
			keepAlive.start();
		}
	}

	class PageHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange ex) throws IOException {
			respond(ex, 200, "text/html; charset=utf-8", getPageHtml());
		}
	}


	// ========== 工具方法 ==========

	static String extractJsonString(String json, String key) {
		String pattern = "\"" + key + "\":\"";
		int idx = json.indexOf(pattern);
		if (idx < 0) return null;
		int start = idx + pattern.length();
		int end = json.indexOf('"', start);
		return end > start ? json.substring(start, end) : null;
	}

	static int extractJsonInt(String json, String key) {
		String pattern = "\"" + key + "\":";
		int idx = json.indexOf(pattern);
		if (idx < 0) return -1;
		int start = idx + pattern.length();
		int end = start;
		while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
		try { return Integer.parseInt(json.substring(start, end)); }
		catch (NumberFormatException e) { return -1; }
	}

	// ========== SSE客户端封装 ==========

	static class SseClient {
		private final OutputStream os;
		private volatile boolean open = true;

		SseClient(OutputStream os) { this.os = os; }

		boolean send(String data) {
			if (!open) return false;
			try {
				synchronized (os) {
					os.write(data.getBytes(StandardCharsets.UTF_8));
					os.flush();
				}
				return true;
			} catch (IOException e) { open = false; return false; }
		}

		boolean isOpen() { return open; }

		void close() {
			open = false;
			try { os.close(); } catch (IOException ignored) {}
		}
	}


	// ========== 前端页面 ==========

	private String getPageHtml() {
		try {
			java.io.InputStream is = WebServer.class.getResourceAsStream("/web/index.html");
			if (is != null) {
				byte[] bytes = is.readAllBytes();
				is.close();
				return new String(bytes, StandardCharsets.UTF_8);
			}
		} catch (IOException ignored) {}
		return "<html><body><h1>页面加载失败</h1></body></html>";
	}

	// ========== 排行榜API ==========

	class RankingHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange ex) throws IOException {
			if ("OPTIONS".equals(ex.getRequestMethod())) {
				ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
				ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
				ex.sendResponseHeaders(204, -1);
				return;
			}
			if ("GET".equals(ex.getRequestMethod())) {
				// 获取排行榜
				respond(ex, 200, "application/json", GobangGame.ranking.toJson());
				return;
			}
			if ("POST".equals(ex.getRequestMethod())) {
				// 提交得分
				String body = readBody(ex);
				String name = extractJsonString(body, "name");
				int score = extractJsonInt(body, "score");
				String level = extractJsonString(body, "level");
				String color = extractJsonString(body, "color");
				int steps = extractJsonInt(body, "steps");
				int forbiddenVal = extractJsonInt(body, "forbidden");
				boolean forbidden = forbiddenVal == 1;
				if (name == null || name.isEmpty() || score <= 0) {
					respond(ex, 400, "application/json", "{\"error\":\"参数无效\"}");
					return;
				}
				int rank = GobangGame.ranking.addRecord(name, score, level != null ? level : "",
						color != null ? color : "", steps, forbidden);
				respond(ex, 200, "application/json", "{\"rank\":" + rank + "}");
				return;
			}
			respond(ex, 405, "application/json", "{\"error\":\"Method not allowed\"}");
		}
	}
}
