import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public interface AI {
	Point compute();

	final class Default implements AI {
		private Chess chess;
		private int[][] oppGrades;
		private int[][] myGrades;

		public Default(Chess chess) {
			this.chess = chess;
			oppGrades = new int[chess.width][chess.height]; // 对手预计得分
			myGrades = new int[chess.width][chess.height]; // AI预计得分
		}

		@Override
		public Point compute() {
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
						continue; // 跳过禁手点
					for (int k : chess.getPointToPattern(i, j))
						switch (chess.getPatternScore(opp, k)) {
						case 1: // 一连子
							this.oppGrades[i][j] += 5;
							break;
						case 2: // 两连子
							this.oppGrades[i][j] += 50;
							break;
						case 3: // 三连子
							this.oppGrades[i][j] += 180;
							break;
						case 4: // 四连子
							this.oppGrades[i][j] += 400;
							break;
						}

					for (int k : chess.getPointToPattern(i, j))
						switch (chess.getPatternScore(myId, k)) {
						case 1:
							this.myGrades[i][j] += 5;
							break;
						case 2:
							this.myGrades[i][j] += 52;
							break;
						case 3:
							this.myGrades[i][j] += 100;
							break;
						case 4:
							this.myGrades[i][j] += 400;
							break;
						}
				}
			int maxPlayerGrades = -1, maxComputerGrades = -1;
			List<Point> maxAttPoints = new ArrayList<>();
			List<Point> maxDefPoints = new ArrayList<>();
			// 寻找最大分数即最优策略
			for (int i = 0; i < chess.width; i++)
				for (int j = 0; j < chess.height; j++) {
					if (chessTable[i][j] == 0) {
						if (myGrades[i][j] > maxComputerGrades) {
							maxAttPoints.clear();
							maxAttPoints.add(new Point(i, j));
							maxComputerGrades = myGrades[i][j];
						} else if (myGrades[i][j] == maxComputerGrades) {
							maxAttPoints.add(new Point(i, j));
						}
						if (oppGrades[i][j] > maxPlayerGrades) {
							maxPlayerGrades = oppGrades[i][j];
							maxDefPoints.clear();
							maxDefPoints.add(new Point(i, j));
						} else if (oppGrades[i][j] == maxPlayerGrades) {
							maxDefPoints.add(new Point(i, j));
						}
					}
				}
			return (maxComputerGrades > maxPlayerGrades) ? Util.random(maxAttPoints) : Util.random(maxDefPoints);
		}
	}
}