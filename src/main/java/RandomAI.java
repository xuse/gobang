import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 入门级AI：有基本棋感但会犯错。
 * 
 * 基于Default AI的评分逻辑计算每个空位的攻防分，
 * 但不总是选最优点——以一定概率选择次优着法，模拟新手的不稳定性。
 * 
 * 具体策略：
 * - 冲四必杀/必防（不会犯这种低级错误）
 * - 其他情况下，将候选点按分数排序，用加权随机从前N个中选择，
 *   排名越靠前被选中的概率越高
 */
public class RandomAI implements AI {
	private final Chess chess;
	private final Random rng = new Random();

	public RandomAI(Chess chess) {
		this.chess = chess;
	}

	@Override
	public Point compute() {
		Player me = chess.getNext();
		Player opp = me.getOpp();
		int[][] board = chess.getTable();
		int W = chess.width, H = chess.height;
		boolean checkForbidden = chess.isForbiddenMoveRule() && me == Player.BLACK;

		List<Point> candidates = new ArrayList<>();
		List<Integer> scores = new ArrayList<>();

		for (int i = 0; i < W; i++) {
			for (int j = 0; j < H; j++) {
				if (board[i][j] != 0) continue;
				if (checkForbidden && chess.isForbidden(i, j)) continue;
				if (!hasNeighbor(board, i, j, W, H)) continue;

				int atk = 0, def = 0;
				for (int pid : chess.getPointToPattern(i, j)) {
					switch (chess.getPatternScore(me, pid)) {
						case 1: atk += 5; break;
						case 2: atk += 52; break;
						case 3: atk += 180; break;
						case 4: atk += 10000; break;
					}
					switch (chess.getPatternScore(opp, pid)) {
						case 1: def += 5; break;
						case 2: def += 50; break;
						case 3: def += 200; break;
						case 4: def += 10000; break;
					}
				}
				int score = Math.max(atk, def);
				candidates.add(new Point(i, j));
				scores.add(score);
			}
		}

		if (candidates.isEmpty()) {
			// 棋盘空或全满，回退到中心
			return new Point(W / 2, H / 2);
		}

		// 冲四必杀/必防：分数>=10000的点必须选
		int maxScore = 0;
		for (int s : scores) if (s > maxScore) maxScore = s;
		if (maxScore >= 10000) {
			List<Point> urgent = new ArrayList<>();
			for (int i = 0; i < candidates.size(); i++) {
				if (scores.get(i) >= 10000) urgent.add(candidates.get(i));
			}
			return Util.random(urgent);
		}

		// 加权随机：从前8个候选中按权重选择
		// 先按分数降序排序
		int n = candidates.size();
		for (int i = 0; i < n - 1; i++) {
			int best = i;
			for (int j = i + 1; j < n; j++) {
				if (scores.get(j) > scores.get(best)) best = j;
			}
			if (best != i) {
				Point tp = candidates.get(i); candidates.set(i, candidates.get(best)); candidates.set(best, tp);
				int ts = scores.get(i); scores.set(i, scores.get(best)); scores.set(best, ts);
			}
		}

		// 70%概率选最优点，30%概率从前5个中随机选
		int pick = Math.min(n, 5);
		if (rng.nextInt(100) < 70) {
			// 选最优（可能有多个同分）
			int topScore = scores.get(0);
			List<Point> tops = new ArrayList<>();
			for (int i = 0; i < pick; i++) {
				if (scores.get(i) == topScore) tops.add(candidates.get(i));
			}
			return Util.random(tops);
		} else {
			// 随机选前5个中的一个
			return candidates.get(rng.nextInt(pick));
		}
	}

	private boolean hasNeighbor(int[][] board, int x, int y, int W, int H) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				if (dx == 0 && dy == 0) continue;
				int nx = x + dx, ny = y + dy;
				if (nx >= 0 && nx < W && ny >= 0 && ny < H && board[nx][ny] != 0) return true;
			}
		}
		return false;
	}
}
