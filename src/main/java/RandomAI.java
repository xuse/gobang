import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 入门级AI：继承Default的评分逻辑，重写选点策略。
 * 
 * 冲四必杀/必防时走最优（不犯低级错误），
 * 其他情况70%走最优、30%从前5候选中随机选，模拟新手的不稳定性。
 */
public class RandomAI extends AI.Default {
	private final Random rng = new Random();

	public RandomAI(Chess chess) {
		super(chess);
	}

	@Override
	protected Point selectMove(int[][] board, int[][] atkGrades, int[][] defGrades) {
		int W = chess.width, H = chess.height;

		// 收集候选点和综合分
		List<Point> candidates = new ArrayList<>();
		List<Integer> scores = new ArrayList<>();
		for (int i = 0; i < W; i++) {
			for (int j = 0; j < H; j++) {
				if (board[i][j] != 0) continue;
				int score = Math.max(atkGrades[i][j], defGrades[i][j]);
				if (score <= 0) continue;
				candidates.add(new Point(i, j));
				scores.add(score);
			}
		}

		if (candidates.isEmpty()) {
			return super.selectMove(board, atkGrades, defGrades);
		}

		// 冲四必杀/必防（分数>=400）：直接走最优，不犯错
		int maxScore = 0;
		for (int s : scores) if (s > maxScore) maxScore = s;
		if (maxScore >= 400) {
			List<Point> urgent = new ArrayList<>();
			for (int i = 0; i < candidates.size(); i++) {
				if (scores.get(i) == maxScore) urgent.add(candidates.get(i));
			}
			return Util.random(urgent);
		}

		// 按分数降序排序
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

		// 70%选最优，30%从前5个随机
		int pick = Math.min(n, 5);
		if (rng.nextInt(100) < 70) {
			int topScore = scores.get(0);
			List<Point> tops = new ArrayList<>();
			for (int i = 0; i < pick; i++) {
				if (scores.get(i) == topScore) tops.add(candidates.get(i));
			}
			return Util.random(tops);
		} else {
			return candidates.get(rng.nextInt(pick));
		}
	}
}
