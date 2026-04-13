import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * 最简单的AI：随机落子。
 * 从所有空位中随机选择一个（跳过禁手点）。
 */
public class RandomAI implements AI {
	private final Chess chess;

	public RandomAI(Chess chess) {
		this.chess = chess;
	}

	@Override
	public Point compute() {
		int[][] board = chess.getTable();
		boolean checkForbidden = chess.isForbiddenMoveRule() && chess.getNext() == Player.BLACK;
		List<Point> candidates = new ArrayList<>();
		for (int i = 0; i < chess.width; i++) {
			for (int j = 0; j < chess.height; j++) {
				if (board[i][j] == 0) {
					if (checkForbidden && chess.isForbidden(i, j)) continue;
					candidates.add(new Point(i, j));
				}
			}
		}
		return candidates.isEmpty() ? new Point(chess.width / 2, chess.height / 2) : Util.random(candidates);
	}
}
