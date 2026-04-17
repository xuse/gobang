import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

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
}
