import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public interface AI {
	Point compute();

	/**
	 * 基线AI：单步攻防评估。
	 * 分别计算每个空位的攻击分和防守分，取较大方向的最优点。
	 * 子类可重写 {@link #selectMove} 来改变选点策略。
	 */
	class Default implements AI {
		protected final Chess chess;
		private int[][] oppGrades;
		private int[][] myGrades;

		public Default(Chess chess) {
			this.chess = chess;
			oppGrades = new int[chess.width][chess.height];
			myGrades = new int[chess.width][chess.height];
		}

		@Override
		public final Point compute() {
			Player myId = chess.next;
			Player opp = myId.getOpp();
			int[][] chessTable = chess.getTable();
			boolean checkForbidden = chess.forbiddenMoveRule && myId == Player.BLACK;
			for (int i = 0; i < chess.width; i++)
				for (int j = 0; j < chess.height; j++) {
					this.oppGrades[i][j] = 0;
					this.myGrades[i][j] = 0;
					if (chessTable[i][j] != 0)
						continue;
					if (checkForbidden && chess.isForbidden(i, j))
						continue;
					for (int k : chess.getPointToPattern(i, j))
						switch (chess.getPatternScore(opp, k)) {
						case 1: this.oppGrades[i][j] += 5; break;
						case 2: this.oppGrades[i][j] += 50; break;
						case 3: this.oppGrades[i][j] += 180; break;
						case 4: this.oppGrades[i][j] += 400; break;
						}
					for (int k : chess.getPointToPattern(i, j))
						switch (chess.getPatternScore(myId, k)) {
						case 1: this.myGrades[i][j] += 5; break;
						case 2: this.myGrades[i][j] += 52; break;
						case 3: this.myGrades[i][j] += 100; break;
						case 4: this.myGrades[i][j] += 400; break;
						}
				}
			return selectMove(chessTable, myGrades, oppGrades);
		}

		/**
		 * 根据评分结果选择落子点。子类可重写此方法改变选点策略。
		 * @param board 棋盘
		 * @param atkGrades 攻击分（我方pattern评分）
		 * @param defGrades 防守分（对手pattern评分）
		 */
		protected Point selectMove(int[][] board, int[][] atkGrades, int[][] defGrades) {
			int maxPlayerGrades = -1, maxComputerGrades = -1;
			List<Point> maxAttPoints = new ArrayList<>();
			List<Point> maxDefPoints = new ArrayList<>();
			for (int i = 0; i < chess.width; i++)
				for (int j = 0; j < chess.height; j++) {
					if (board[i][j] == 0) {
						if (atkGrades[i][j] > maxComputerGrades) {
							maxAttPoints.clear();
							maxAttPoints.add(new Point(i, j));
							maxComputerGrades = atkGrades[i][j];
						} else if (atkGrades[i][j] == maxComputerGrades) {
							maxAttPoints.add(new Point(i, j));
						}
						if (defGrades[i][j] > maxPlayerGrades) {
							maxPlayerGrades = defGrades[i][j];
							maxDefPoints.clear();
							maxDefPoints.add(new Point(i, j));
						} else if (defGrades[i][j] == maxPlayerGrades) {
							maxDefPoints.add(new Point(i, j));
						}
					}
				}
			return (maxComputerGrades > maxPlayerGrades) ? Util.random(maxAttPoints) : Util.random(maxDefPoints);
		}
	}
	class Beginner extends Default {
		private final Random rng = new Random();

		public Beginner(Chess chess) {
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
			if (maxScore >= 410) {
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

			// 50%选最优，50%从前5个随机
			int pick = Math.min(n, 5);
			if (rng.nextInt(100) < 50) {
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
}
