import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * 优化的深度搜索AI。
 * 
 * 设计思路：以SmartEvalAI的单步评估为基础，叠加浅层alpha-beta搜索。
 * 搜索的目的不是替代单步评估，而是在单步评估的基础上发现更深层的战术：
 * - 2-3步后的冲四/杀棋
 * - 对手的隐藏威胁（需要提前防守）
 * - 组合攻击的构建路径
 * 
 * 评估函数直接使用SmartEvalAI的评分逻辑（已验证优于Default），
 * 确保搜索的叶节点评估质量不低于单步AI。
 */
public class SmartSearchAI implements AI {

	private final Chess chess;
	private final int W, H;
	private final int[][] nbr;

	private static final int IMPOSSIBLE = 7;
	private static final int DEPTH = 4;
	private static final int RADIUS = 2;
	private static final int INF = 10_000_000;

	public SmartSearchAI(Chess chess) {
		this.chess = chess;
		this.W = chess.width;
		this.H = chess.height;
		this.nbr = new int[W][H];
	}

	@Override
	public Point compute() {
		Player me = chess.next;
		Player opp = me.getOpp();
		int[][] table = chess.getTable();

		initNbr(table);

		int[] cx = new int[W * H], cy = new int[W * H];
		int cn = gather(cx, cy, table);
		if (cn == 0) return new Point(W / 2, H / 2);

		// 即时胜利
		for (int i = 0; i < cn; i++)
			if (canWin(cx[i], cy[i], me)) return new Point(cx[i], cy[i]);

		// 必须防守
		int blockX = -1, blockY = -1, blockCount = 0;
		for (int i = 0; i < cn; i++) {
			if (canWin(cx[i], cy[i], opp)) {
				blockX = cx[i]; blockY = cy[i];
				blockCount++;
			}
		}
		if (blockCount == 1) return new Point(blockX, blockY);

		// 用SmartEvalAI的逻辑计算每个候选点的单步评分
		int[] atkArr = new int[cn], defArr = new int[cn];
		int maxAtk = -1, maxDef = -1;
		for (int i = 0; i < cn; i++) {
			int atk = 0, def = 0;
			int myThrees = 0, myFours = 0;
			int oppThrees = 0, oppFours = 0;
			for (int pid : chess.getPointToPattern(cx[i], cy[i])) {
				switch (chess.getPatternScore(me, pid)) {
					case 1: atk += 5; break;
					case 2: atk += 52; break;
					case 3: atk += 100; myThrees++; break;
					case 4: atk += 10000; myFours++; break;
				}
				switch (chess.getPatternScore(opp, pid)) {
					case 1: def += 5; break;
					case 2: def += 50; break;
					case 3: def += 180; oppThrees++; break;
					case 4: def += 10000; oppFours++; break;
				}
			}
			if (myFours >= 1 && myThrees >= 1) atk += 8000;
			else if (myFours >= 2) atk += 8000;
			else if (myThrees >= 2) atk += 3000;
			if (oppFours >= 1 && oppThrees >= 1) def += 8000;
			else if (oppFours >= 2) def += 8000;
			else if (oppThrees >= 2) def += 3000;

			atkArr[i] = atk;
			defArr[i] = def;
			if (atk > maxAtk) maxAtk = atk;
			if (def > maxDef) maxDef = def;
		}

		// 用SmartEvalAI的决策逻辑选出单步最佳候选集
		boolean attack = maxAtk > maxDef;
		int targetMax = attack ? maxAtk : maxDef;

		// 收集所有达到最大分的候选点，以及次优候选
		// 对这些候选点进行深度搜索来精选
		int[] searchOrder = new int[cn];
		for (int i = 0; i < cn; i++) {
			int primary = attack ? atkArr[i] : defArr[i];
			int combo = atkArr[i] + defArr[i];
			// 排序分：主分 * 大权重 + 综合分作为次要排序
			searchOrder[i] = primary * 1000 + combo;
		}
		sortDesc(cx, cy, searchOrder, cn);

		// 搜索前N个候选
		int limit = Math.min(cn, 10);
		int bestVal = Integer.MIN_VALUE;
		int bx = cx[0], by = cy[0];

		for (int i = 0; i < limit; i++) {
			int[] saved = chess.simMove(cx[i], cy[i], me);
			addNbr(cx[i], cy[i]);

			int val = -search(DEPTH - 1, -INF, -bestVal, opp, me);

			removeNbr(cx[i], cy[i]);
			chess.simUndo(cx[i], cy[i], me, saved);

			if (val > bestVal) {
				bestVal = val;
				bx = cx[i]; by = cy[i];
			}
		}
		return new Point(bx, by);
	}

	private int search(int depth, int alpha, int beta, Player current, Player opponent) {
		int[][] table = chess.getTable();
		int[] cx = new int[W * H], cy = new int[W * H];
		int cn = gather(cx, cy, table);
		if (cn == 0) return 0;

		// 即时胜利
		for (int i = 0; i < cn; i++)
			if (canWin(cx[i], cy[i], current))
				return INF + depth;

		// 对手冲四必防
		int threatX = -1, threatY = -1, threatCount = 0;
		for (int i = 0; i < cn; i++) {
			if (canWin(cx[i], cy[i], opponent)) {
				threatX = cx[i]; threatY = cy[i];
				threatCount++;
			}
		}
		if (threatCount > 1) return -(INF + depth);
		if (threatCount == 1) {
			if (depth <= 0) {
				int[] saved = chess.simMove(threatX, threatY, current);
				int val = -evalLeaf(opponent, current);
				chess.simUndo(threatX, threatY, current, saved);
				return val;
			}
			int[] saved = chess.simMove(threatX, threatY, current);
			addNbr(threatX, threatY);
			int val = -search(depth - 1, -beta, -alpha, opponent, current);
			removeNbr(threatX, threatY);
			chess.simUndo(threatX, threatY, current, saved);
			return val;
		}

		if (depth <= 0)
			return evalLeaf(current, opponent);

		// 排序
		int[] sc = new int[cn];
		for (int i = 0; i < cn; i++)
			sc[i] = quickScore(cx[i], cy[i], current, opponent);
		sortDesc(cx, cy, sc, cn);

		int limit = Math.min(cn, depth >= 3 ? 8 : 6);

		for (int i = 0; i < limit; i++) {
			int[] saved = chess.simMove(cx[i], cy[i], current);
			addNbr(cx[i], cy[i]);

			int val = -search(depth - 1, -beta, -alpha, opponent, current);

			removeNbr(cx[i], cy[i]);
			chess.simUndo(cx[i], cy[i], current, saved);

			if (val >= beta) return beta;
			if (val > alpha) alpha = val;
		}
		return alpha;
	}

	/**
	 * 叶节点评估：轻量版。
	 * 只遍历候选点（nbr > 0的空位），计算攻防分。
	 * 使用与SmartEvalAI相同的权重和组合威胁检测。
	 */
	private int evalLeaf(Player current, Player opponent) {
		int[][] table = chess.getTable();

		int curMax = 0, oppMax = 0;
		int curThreePoints = 0, oppThreePoints = 0;

		for (int i = 0; i < W; i++) {
			for (int j = 0; j < H; j++) {
				if (table[i][j] != 0 || nbr[i][j] <= 0) continue;

				int cAtk = 0, cDef = 0;
				int cThrees = 0, cFours = 0;
				int oAtk = 0, oDef = 0;
				int oThrees = 0, oFours = 0;

				for (int pid : chess.getPointToPattern(i, j)) {
					int cp = chess.getPatternScore(current, pid);
					int op = chess.getPatternScore(opponent, pid);

					switch (cp) {
						case 1: cAtk += 5; oDef += 5; break;
						case 2: cAtk += 52; oDef += 50; break;
						case 3: cAtk += 100; oDef += 180; cThrees++; break;
						case 4: cAtk += 10000; oDef += 10000; cFours++; break;
					}
					switch (op) {
						case 1: oAtk += 5; cDef += 5; break;
						case 2: oAtk += 52; cDef += 50; break;
						case 3: oAtk += 100; cDef += 180; oThrees++; break;
						case 4: oAtk += 10000; cDef += 10000; oFours++; break;
					}
				}

				// 组合威胁
				if (cFours >= 1 && cThrees >= 1) cAtk += 8000;
				else if (cFours >= 2) cAtk += 8000;
				else if (cThrees >= 2) cAtk += 3000;

				if (oFours >= 1 && oThrees >= 1) oAtk += 8000;
				else if (oFours >= 2) oAtk += 8000;
				else if (oThrees >= 2) oAtk += 3000;

				int cBest = Math.max(cAtk, cDef);
				int oBest = Math.max(oAtk, oDef);

				if (cBest > curMax) curMax = cBest;
				if (oBest > oppMax) oppMax = oBest;
				if (cThrees >= 1) curThreePoints++;
				if (oThrees >= 1) oppThreePoints++;
			}
		}

		int score = curMax - oppMax;
		if (curThreePoints >= 3) score += 1500;
		else if (curThreePoints >= 2) score += 500;
		if (oppThreePoints >= 3) score -= 1500;
		else if (oppThreePoints >= 2) score -= 500;
		return score;
	}

	/** 着法排序用的快速评分 */
	private int quickScore(int x, int y, Player cur, Player opponent) {
		int atk = 0, def = 0;
		int myThrees = 0, oppThrees = 0;
		for (int pid : chess.getPointToPattern(x, y)) {
			int mp = chess.getPatternScore(cur, pid);
			if (mp != IMPOSSIBLE && mp < 5) {
				switch (mp) {
					case 1: atk += 8; break;
					case 2: atk += 56; break;
					case 3: atk += 300; myThrees++; break;
					case 4: atk += INF; break;
					default: atk += 1; break;
				}
			}
			int op = chess.getPatternScore(opponent, pid);
			if (op != IMPOSSIBLE && op < 5) {
				switch (op) {
					case 1: def += 4; break;
					case 2: def += 28; break;
					case 3: def += 160; oppThrees++; break;
					case 4: def += 800; break;
				}
			}
		}
		if (myThrees >= 2) atk += 2000;
		if (oppThrees >= 2) def += 2000;
		return Math.max(atk, def) * 2 + atk + def;
	}

	private boolean canWin(int x, int y, Player player) {
		for (int pid : chess.getPointToPattern(x, y))
			if (chess.getPatternScore(player, pid) == 4) return true;
		return false;
	}

	private void initNbr(int[][] table) {
		for (int i = 0; i < W; i++)
			for (int j = 0; j < H; j++) nbr[i][j] = 0;
		for (int i = 0; i < W; i++)
			for (int j = 0; j < H; j++)
				if (table[i][j] != 0) addNbr(i, j);
	}

	private int gather(int[] cx, int[] cy, int[][] table) {
		int n = 0;
		for (int i = 0; i < W; i++)
			for (int j = 0; j < H; j++)
				if (table[i][j] == 0 && nbr[i][j] > 0) {
					cx[n] = i; cy[n] = j; n++;
				}
		return n;
	}

	private void addNbr(int x, int y) {
		for (int dx = -RADIUS; dx <= RADIUS; dx++)
			for (int dy = -RADIUS; dy <= RADIUS; dy++) {
				if (dx == 0 && dy == 0) continue;
				int nx = x + dx, ny = y + dy;
				if (nx >= 0 && nx < W && ny >= 0 && ny < H) nbr[nx][ny]++;
			}
	}

	private void removeNbr(int x, int y) {
		for (int dx = -RADIUS; dx <= RADIUS; dx++)
			for (int dy = -RADIUS; dy <= RADIUS; dy++) {
				if (dx == 0 && dy == 0) continue;
				int nx = x + dx, ny = y + dy;
				if (nx >= 0 && nx < W && ny >= 0 && ny < H) nbr[nx][ny]--;
			}
	}

	private void sortDesc(int[] cx, int[] cy, int[] scores, int n) {
		for (int i = 0; i < n - 1; i++) {
			int best = i;
			for (int j = i + 1; j < n; j++)
				if (scores[j] > scores[best]) best = j;
			if (best != i) {
				int t;
				t = cx[i]; cx[i] = cx[best]; cx[best] = t;
				t = cy[i]; cy[i] = cy[best]; cy[best] = t;
				t = scores[i]; scores[i] = scores[best]; scores[best] = t;
			}
		}
	}
}
