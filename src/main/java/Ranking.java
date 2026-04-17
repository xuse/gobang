import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * 人机对局最高分记录与排行榜。
 *
 * 计分规则：
 * - 基础分 = max(1, 200 - 步数)
 * - 难度系数：简单×1, 中等×2, 困难×3
 * - 禁手加成：开启禁手时 ×1.2
 * - 最终得分 = 基础分 × 难度系数 × 禁手加成
 *
 * 排行榜持久化为JSON文件，保存在用户目录下 .gobang/ranking.json
 */
public class Ranking {

	/** 单条记录 */
	static class Record implements Comparable<Record> {
		String name;      // 玩家名
		int score;        // 得分
		String level;     // 难度
		String color;     // 执子 BLACK/WHITE
		int steps;        // 步数
		boolean forbidden;// 是否开启禁手
		long time;        // 时间戳

		Record(String name, int score, String level, String color, int steps, boolean forbidden, long time) {
			this.name = name;
			this.score = score;
			this.level = level;
			this.color = color;
			this.steps = steps;
			this.forbidden = forbidden;
			this.time = time;
		}

		@Override
		public int compareTo(Record o) {
			return Integer.compare(o.score, this.score); // 降序
		}
	}

	private static final int MAX_RECORDS = 20;
	private static final Path RANKING_FILE;
	static {
		Path dir = Paths.get(System.getProperty("user.home"), ".gobang");
		try { Files.createDirectories(dir); } catch (IOException ignored) {}
		RANKING_FILE = dir.resolve("ranking.json");
	}

	private final List<Record> records = new ArrayList<>();

	public Ranking() {
		load();
		if (records.isEmpty()) {
			seedDefaults();
		}
	}

	/** 预置示例记录，让排行榜初始不为空 */
	private void seedDefaults() {
		String[][] seeds = {
			{"棋圣",  "540", "困难", "BLACK", "20", "true"},
			{"高手",  "450", "困难", "WHITE", "25", "false"},
			{"老王",  "380", "困难", "BLACK", "34", "false"},
			{"小明",  "350", "中等", "WHITE", "22", "true"},
			{"棋迷",  "300", "中等", "BLACK", "28", "false"},
			{"阿花",  "260", "中等", "WHITE", "35", "true"},
			{"新手",  "180", "简单", "BLACK", "21", "false"},
			{"路人甲", "150", "简单", "WHITE", "30", "false"},
			{"菜鸟",  "120", "简单", "BLACK", "45", "false"},
			{"围观者", "80",  "简单", "WHITE", "60", "false"},
		};
		long baseTime = System.currentTimeMillis() - 86400000L * 30; // 30天前起
		for (int i = 0; i < seeds.length; i++) {
			String[] s = seeds[i];
			records.add(new Record(s[0], Integer.parseInt(s[1]), s[2], s[3],
					Integer.parseInt(s[4]), Boolean.parseBoolean(s[5]),
					baseTime + i * 86400000L));
		}
		Collections.sort(records);
		save();
	}

	/** 计算得分 */
	public static int calcScore(int steps, String level, boolean forbidden) {
		int base = Math.max(1, 200 - steps);
		int mult;
		switch (level) {
			case "普通": case "NORMAL": mult = 2; break;
			case "困难": case "HARD": mult = 3; break;
			case "简单": case "EASY": mult = 1; break;
			default: mult = 1; break; // 入门/BEGINNER
		}
		double forbiddenBonus = forbidden ? 1.2 : 1.0;
		return (int) (base * mult * forbiddenBonus);
	}

	/** 添加记录，返回排名（1-based），-1表示未上榜 */
	public synchronized int addRecord(String name, int score, String level, String color,
									  int steps, boolean forbidden) {
		Record r = new Record(name, score, level, color, steps, forbidden, System.currentTimeMillis());
		records.add(r);
		Collections.sort(records);
		if (records.size() > MAX_RECORDS) {
			records.subList(MAX_RECORDS, records.size()).clear();
		}
		int rank = records.indexOf(r);
		if (rank < 0) return -1;
		save();
		return rank + 1;
	}

	/** 获取排行榜（只读副本） */
	public synchronized List<Record> getRecords() {
		return new ArrayList<>(records);
	}

	/** 获取最高分 */
	public synchronized int getHighScore() {
		return records.isEmpty() ? 0 : records.get(0).score;
	}

	/** 导出为JSON字符串 */
	public synchronized String toJson() {
		StringBuilder sb = new StringBuilder(2048);
		sb.append("[");
		for (int i = 0; i < records.size(); i++) {
			Record r = records.get(i);
			if (i > 0) sb.append(',');
			sb.append("\n  {\"name\":\"").append(escapeJson(r.name)).append("\"");
			sb.append(",\"score\":").append(r.score);
			sb.append(",\"level\":\"").append(escapeJson(r.level)).append("\"");
			sb.append(",\"color\":\"").append(r.color).append("\"");
			sb.append(",\"steps\":").append(r.steps);
			sb.append(",\"forbidden\":").append(r.forbidden);
			sb.append(",\"time\":").append(r.time).append("}");
		}
		sb.append("\n]");
		return sb.toString();
	}

	// ===== 持久化 =====

	private void save() {
		try {
			Files.write(RANKING_FILE, toJson().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			System.err.println("保存排行榜失败: " + e.getMessage());
		}
	}

	private void load() {
		if (!Files.exists(RANKING_FILE)) return;
		try {
			String json = new String(Files.readAllBytes(RANKING_FILE), StandardCharsets.UTF_8);
			parseJson(json);
		} catch (Exception e) {
			System.err.println("加载排行榜失败: " + e.getMessage());
		}
	}

	private void parseJson(String json) {
		records.clear();
		int i = 0;
		while (i < json.length()) {
			int objStart = json.indexOf('{', i);
			if (objStart < 0) break;
			int objEnd = json.indexOf('}', objStart);
			if (objEnd < 0) break;
			String obj = json.substring(objStart, objEnd + 1);
			try {
				String name = extractStr(obj, "name");
				int score = extractInt(obj, "score");
				String level = extractStr(obj, "level");
				String color = extractStr(obj, "color");
				int steps = extractInt(obj, "steps");
				boolean forbidden = obj.contains("\"forbidden\":true");
				long time = extractLong(obj, "time");
				records.add(new Record(name != null ? name : "???", score, level != null ? level : "",
						color != null ? color : "", steps, forbidden, time));
			} catch (Exception ignored) {}
			i = objEnd + 1;
		}
		Collections.sort(records);
		if (records.size() > MAX_RECORDS) {
			records.subList(MAX_RECORDS, records.size()).clear();
		}
	}

	private static String extractStr(String json, String key) {
		String pat = "\"" + key + "\":\"";
		int idx = json.indexOf(pat);
		if (idx < 0) return null;
		int start = idx + pat.length();
		int end = json.indexOf('"', start);
		return end > start ? json.substring(start, end) : null;
	}

	private static int extractInt(String json, String key) {
		String pat = "\"" + key + "\":";
		int idx = json.indexOf(pat);
		if (idx < 0) return 0;
		int start = idx + pat.length();
		int end = start;
		while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
		try { return Integer.parseInt(json.substring(start, end)); }
		catch (NumberFormatException e) { return 0; }
	}

	private static long extractLong(String json, String key) {
		String pat = "\"" + key + "\":";
		int idx = json.indexOf(pat);
		if (idx < 0) return 0;
		int start = idx + pat.length();
		int end = start;
		while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
		try { return Long.parseLong(json.substring(start, end)); }
		catch (NumberFormatException e) { return 0; }
	}

	private static String escapeJson(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
