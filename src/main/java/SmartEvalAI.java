import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * 优化的单步评估AI。
 * 
 * 基于Default的攻防分离比较框架，做以下改进：
 * 
 * 1. 组合威胁检测：检测"双三"（落子后同时形成两个以上三连）和
 *    "四三"（落子后同时形成四连和三连）等组合威胁，给予极高分值。
 *    这些组合在五子棋中几乎是必杀棋形。
 * 2. 同分打破：在同分候选中选综合分(攻+防)最高的点，
 *    再按距中心距离打破平局，避免随机落子。
 * 3. 冲四分值提升到10000，确保冲四绝对优先。
 * 4. 邻域过滤：只评估已有棋子半径2内的空位。
 */
public class SmartEvalAI implements AI {

	private final Chess chess;
	private final int W, H;

	public SmartEvalAI(Chess chess) {
		this.chess = chess;
		this.W = chess.width;
		this.H = chess.height;
	}

	@Override
	public Point compute() {
		Player me = chess.next;
		Player opp = me.getOpp();
		int[][] table = chess.getTable();
		int centerX = W / 2;
		int centerY = H / 2;
		boolean checkForbidden = chess.forbiddenMoveRule && me == Player.BLACK;

		int capacity = W * H;
		int[] px = new int[capacity], py = new int[capacity];
		int[] atkArr = new int[capacity], defArr = new int[capacity];
		int count = 0;
		int maxAtk = -1, maxDef = -1;

		for (int i = 0; i < W; i++) {
			for (int j = 0; j < H; j++) {
				if (table[i][j] != 0) continue;
				if (!hasNeighbor(table, i, j)) continue;
				if (checkForbidden && chess.isForbidden(i, j)) continue; // 跳过禁手点

				int atk = 0, def = 0;
				int myThrees = 0, myFours = 0;   // 我方三连和四连计数
				int oppThrees = 0, oppFours = 0;  // 对手三连和四连计数

				for (int pid : chess.getPointToPattern(i, j)) {
					int myP = chess.getPatternScore(me, pid);
					switch (myP) {
						case 1: atk += 5; break;
						case 2: atk += 52; break;
						case 3: atk += 100; myThrees++; break;
						case 4: atk += 10000; myFours++; break;
					}
					int oppP = chess.getPatternScore(opp, pid);
					switch (oppP) {
						case 1: def += 5; break;
						case 2: def += 50; break;
						case 3: def += 180; oppThrees++; break;
						case 4: def += 10000; oppFours++; break;
					}
				}

				// 组合威胁加分
				// 我方：四三（冲四+活三）或双四 -> 必杀
				if (myFours >= 1 && myThrees >= 1) atk += 8000;
				else if (myFours >= 2) atk += 8000;
				// 我方：双三 -> 对手无法同时防守两个三连
				else if (myThrees >= 2) atk += 3000;

				// 对手：同样的组合威胁需要防守
				if (oppFours >= 1 && oppThrees >= 1) def += 8000;
				else if (oppFours >= 2) def += 8000;
				else if (oppThrees >= 2) def += 3000;

				px[count] = i;
				py[count] = j;
				atkArr[count] = atk;
				defArr[count] = def;
				count++;

				if (atk > maxAtk) maxAtk = atk;
				if (def > maxDef) maxDef = def;
			}
		}

		if (count == 0) return new Point(centerX, centerY);

		boolean attack = maxAtk > maxDef;
		int targetMax = attack ? maxAtk : maxDef;

		int bestCombo = -1;
		int bestDist = Integer.MAX_VALUE;
		List<Point> bestPoints = new ArrayList<>();

		for (int k = 0; k < count; k++) {
			int primary = attack ? atkArr[k] : defArr[k];
			if (primary != targetMax) continue;

			int combo = atkArr[k] + defArr[k];
			int dist = Math.abs(px[k] - centerX) + Math.abs(py[k] - centerY);

			if (combo > bestCombo || (combo == bestCombo && dist < bestDist)) {
				bestCombo = combo;
				bestDist = dist;
				bestPoints.clear();
				bestPoints.add(new Point(px[k], py[k]));
			} else if (combo == bestCombo && dist == bestDist) {
				bestPoints.add(new Point(px[k], py[k]));
			}
		}

		return Util.random(bestPoints);
	}

	private boolean hasNeighbor(int[][] table, int x, int y) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				if (dx == 0 && dy == 0) continue;
				int nx = x + dx, ny = y + dy;
				if (nx >= 0 && nx < W && ny >= 0 && ny < H && table[nx][ny] != 0) {
					return true;
				}
			}
		}
		return false;
	}
}
