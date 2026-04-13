import org.junit.jupiter.api.Test;

public class ForbiddenBenchmarkTest {

    private static final int BOARD_SIZE = 19;
    private static final int GAMES = 100;

    @Test
    void smartSearch_black_vs_default_white_withForbidden() {
        run("SmartSearch(黑) vs Default(白) [禁手]", SmartSearchAI.class, AI.Default.class);
    }

    @Test
    void default_black_vs_smartSearch_white_withForbidden() {
        run("Default(黑) vs SmartSearch(白) [禁手]", AI.Default.class, SmartSearchAI.class);
    }

    private void run(String label, Class<? extends AI> blackType, Class<? extends AI> whiteType) {
        Chess chess = new Chess(null, BOARD_SIZE, BOARD_SIZE);
        chess.printStep = false;
        chess.forbiddenMoveRule = true;

        int blackWin = 0, whiteWin = 0, draw = 0, forbiddenLoss = 0;
        int totalSteps = 0, minSteps = Integer.MAX_VALUE, maxSteps = 0;

        long start = System.currentTimeMillis();
        for (int i = 0; i < GAMES; i++) {
            AI blackAI = createAI(blackType, chess);
            AI whiteAI = createAI(whiteType, chess);
            chess.initGameWithAI(blackAI, whiteAI);
            while (chess.next != null) {
                chess.computerMove();
            }
            int steps = chess.his.count();
            if (chess.winner == Player.BLACK) blackWin++;
            else if (chess.winner == Player.WHITE) {
                whiteWin++;
                if (steps % 2 == 1) forbiddenLoss++;
            } else draw++;
            totalSteps += steps;
            if (steps < minSteps) minSteps = steps;
            if (steps > maxSteps) maxSteps = steps;
        }
        long cost = System.currentTimeMillis() - start;

        System.out.printf(
            "%n===== %s =====%n" +
            "总局数: %d | 黑胜: %d (%.1f%%) | 白胜: %d (%.1f%%) | 平局: %d (%.1f%%)%n" +
            "黑方因禁手判负: %d%n" +
            "手数: 最小=%d, 最大=%d, 平均=%d%n" +
            "耗时: %dms, 每局平均%.1fms%n",
            label, GAMES,
            blackWin, 100.0 * blackWin / GAMES,
            whiteWin, 100.0 * whiteWin / GAMES,
            draw, 100.0 * draw / GAMES,
            forbiddenLoss,
            minSteps, maxSteps, totalSteps / GAMES,
            cost, (float) cost / GAMES
        );
    }

    private AI createAI(Class<? extends AI> clazz, Chess chess) {
        try {
            return clazz.getConstructor(Chess.class).newInstance(chess);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
