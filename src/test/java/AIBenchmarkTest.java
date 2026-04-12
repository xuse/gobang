import org.junit.jupiter.api.Test;

/**
 * AI胜率基准测试。
 * 通过大量自动对局统计胜率、平均手数等指标来评估AI强度。
 */
public class AIBenchmarkTest {

	private static final int BOARD_SIZE = 19;
	private static final int GAMES = 500;
	// Minimax对局数少一些，因为搜索耗时更长
	private static final int MINIMAX_GAMES = 100;

	private BenchmarkResult runBenchmark(String label, Class<? extends AI> blackType, Class<? extends AI> whiteType, int games) {
		Chess chess = new Chess(null, BOARD_SIZE, BOARD_SIZE);
		chess.printStep = false;

		int blackWin = 0, whiteWin = 0, draw = 0;
		int totalSteps = 0, minSteps = Integer.MAX_VALUE, maxSteps = 0;

		long start = System.currentTimeMillis();
		for (int i = 0; i < games; i++) {
			chess.initGameWithAI(createAI(blackType, chess), createAI(whiteType, chess));
			while (chess.next != null) {
				chess.computerMove();
			}
			int steps = chess.his.count();
			if (chess.winner == Player.BLACK) blackWin++;
			else if (chess.winner == Player.WHITE) whiteWin++;
			else draw++;
			totalSteps += steps;
			if (steps < minSteps) minSteps = steps;
			if (steps > maxSteps) maxSteps = steps;
		}
		long cost = System.currentTimeMillis() - start;

		BenchmarkResult result = new BenchmarkResult(label, games, blackWin, whiteWin, draw,
				totalSteps, minSteps, maxSteps, cost);
		System.out.println(result);
		return result;
	}

	private AI createAI(Class<? extends AI> clazz, Chess chess) {
		try {
			return clazz.getConstructor(Chess.class).newInstance(chess);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// ========== 旧AI基线 ==========

	@Test
	void baseline_DefaultVsDefault() {
		runBenchmark("Default(黑) vs Default(白)", AI.Default.class, AI.Default.class, GAMES);
	}


	// ========== SmartEvalAI 单步优化测试 ==========

	@Test
	void smartEval_vs_Default_asBlack() {
		runBenchmark("SmartEval(黑) vs Default(白)", SmartEvalAI.class, AI.Default.class, GAMES);
	}

	@Test
	void smartEval_vs_Default_asWhite() {
		runBenchmark("Default(黑) vs SmartEval(白)", AI.Default.class, SmartEvalAI.class, GAMES);
	}

	// ========== SmartSearchAI 搜索优化测试 ==========

	@Test
	void smartSearch_vs_Default_asBlack() {
		runBenchmark("SmartSearch(黑) vs Default(白)", SmartSearchAI.class, AI.Default.class, MINIMAX_GAMES);
	}

	@Test
	void smartSearch_vs_Default_asWhite() {
		runBenchmark("Default(黑) vs SmartSearch(白)", AI.Default.class, SmartSearchAI.class, MINIMAX_GAMES);
	}

	@Test
	void smartSearch_vs_SmartEval() {
		runBenchmark("SmartSearch(黑) vs SmartEval(白)", SmartSearchAI.class, SmartEvalAI.class, MINIMAX_GAMES);
	}

	@Test
	void smartEval_vs_SmartSearch() {
		runBenchmark("SmartEval(黑) vs SmartSearch(白)", SmartEvalAI.class, SmartSearchAI.class, MINIMAX_GAMES);
	}
	
	@Test
	void smartSearch_vs_smartSearch() {
		runBenchmark("SmartSearch(黑) vs SmartSearch(白)", SmartSearchAI.class, SmartSearchAI.class, MINIMAX_GAMES);
	}


	// ========== 结果封装 ==========

	static class BenchmarkResult {
		final String label;
		final int games, blackWin, whiteWin, draw;
		final int totalSteps, minSteps, maxSteps;
		final long costMs;

		BenchmarkResult(String label, int games, int blackWin, int whiteWin, int draw,
						int totalSteps, int minSteps, int maxSteps, long costMs) {
			this.label = label;
			this.games = games;
			this.blackWin = blackWin;
			this.whiteWin = whiteWin;
			this.draw = draw;
			this.totalSteps = totalSteps;
			this.minSteps = minSteps;
			this.maxSteps = maxSteps;
			this.costMs = costMs;
		}

		@Override
		public String toString() {
			return String.format(
				"\n===== %s =====\n" +
				"总局数: %d | 黑胜: %d (%.1f%%) | 白胜: %d (%.1f%%) | 平局: %d (%.1f%%)\n" +
				"手数: 最小=%d, 最大=%d, 平均=%d\n" +
				"耗时: %dms, 每局平均%.1fms\n",
				label, games,
				blackWin, 100.0 * blackWin / games,
				whiteWin, 100.0 * whiteWin / games,
				draw, 100.0 * draw / games,
				minSteps, maxSteps, totalSteps / games,
				costMs, (float) costMs / games
			);
		}
	}
}
