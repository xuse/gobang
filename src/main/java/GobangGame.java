import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * 五子棋游戏主界面
 *
 * @author Joey
 */
public class GobangGame {
	private static final String GAME_VERSION_STR = "五子棋游戏 版本2.0";

	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}
		GameFrame game = new GameFrame();
		game.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		game.setLocationRelativeTo(null);
		game.setVisible(true);
	}

	/** 全局排行榜 */
	static final Ranking ranking = new Ranking();

	static class GameFrame extends JFrame {
		private static final long serialVersionUID = 1L;

		public GameFrame() {
			int width = 13;
			int height = 13;
			Container contentPane = getContentPane();

			final ChessPanel panel = new ChessPanel(width, height);

			panel.setBackground(new Color(222, 184, 135));
			contentPane.setBackground(new Color(180, 140, 100));
			contentPane.add(panel);

			int widthPx = (width - 1) * 30 + 100;
			int heightPx = (height - 1) * 30 + 130;

			setSize(widthPx, heightPx);
			setTitle(GAME_VERSION_STR);
			setResizable(false);
			panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
			JMenuBar menuBar = new JMenuBar();
			JMenu m_main = new JMenu("游戏");
			JMenu m_auto = new JMenu("自动");
			JMenu m_his = new JMenu("历史");
			JMenu m_review = new JMenu("复盘");
			JMenu m_help = new JMenu("帮助");
			panel.reviewMenu = m_review;

		
			JMenu m_diff = new JMenu("AI难度");
			javax.swing.ButtonGroup diffGroup = new javax.swing.ButtonGroup();
			for (Level diff:Level.values()) {
				javax.swing.JRadioButtonMenuItem item = new javax.swing.JRadioButtonMenuItem(diff.name, diff == panel.chess.defaultAiLevel);
				item.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						panel.chess.defaultAiLevel = diff;
						for (Player p : Player.values()) {
							if (!p.isHuman() && p.getAi() != null) {
								p.setAi(panel.chess.createAI(diff));
								p.human = false;
							}
						}
						panel.repaint();
					}
				});
				diffGroup.add(item);
				m_diff.add(item);
			}

			m_main.add(new JMenuItem("开始游戏(执黑)")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (!panel.confirmNewGame()) return;
					panel.chess.initGame(true, false);
					SoundManager.playGameStart();
					panel.resetIdleTimer();
					panel.repaint();
				}
			});

			m_main.add(new JMenuItem("开始游戏(执白)")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (!panel.confirmNewGame()) return;
					panel.chess.initGame(false, true);
					SoundManager.playGameStart();
					panel.resetIdleTimer();
					panel.repaint();
				}
			});

			m_main.add(new JMenuItem("开始游戏(自动)")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (!panel.confirmNewGame()) return;
					panel.chess.initGame(false, false);
					SoundManager.playGameStart();
					panel.stopIdleTimer();
					panel.repaint();
					panel.chess.startAuto(100);
				}
			});
			m_main.add(new JMenuItem("开始游戏(双人对局)")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (!panel.confirmNewGame()) return;
					panel.chess.initGame(true, true);
					SoundManager.playGameStart();
					panel.resetIdleTimer();
					panel.repaint();
				}
			});
			m_main.add(new JMenuItem("重定义棋盘大小")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					javax.swing.JSpinner spW = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(panel.chess.width, 5, 36, 1));
					javax.swing.JSpinner spH = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(panel.chess.height, 5, 36, 1));
					JPanel p = new JPanel(new java.awt.GridLayout(2, 2, 8, 6));
					p.add(new javax.swing.JLabel("宽 (5-36)："));
					p.add(spW);
					p.add(new javax.swing.JLabel("高 (5-36)："));
					p.add(spH);
					int result = JOptionPane.showConfirmDialog(panel, p, "重定义棋盘大小", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
					if (result != JOptionPane.OK_OPTION) return;
					int x = (Integer) spW.getValue();
					int y = (Integer) spH.getValue();
					panel.chess.resetSize(x, y);
					int widthPx = (x - 1) * 30 + 100;
					int heightPx = (y - 1) * 30 + 130;
					setSize(widthPx, heightPx);
					panel.chess.initGame(true, false);
					SoundManager.playGameStart();
					panel.resetIdleTimer();
					panel.repaint();
				}
			});
			m_main.addSeparator();
			m_main.add(new JMenuItem("退出")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});

			m_auto.add(new JMenuItem("自动下一子")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (panel.chess.isReviewMode())
						return;
					panel.chess.computerMove();
					SoundManager.playPlace();
					Player next = panel.chess.getNext();
					if (next != null && !next.isHuman()) {
						panel.chess.computerMove();
						SoundManager.playPlace();
					}
					panel.resetIdleTimer();
					panel.repaint();
					panel.checkGameOver();
				}
			});
			m_auto.add(new JMenuItem("电脑托管")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (panel.chess.isReviewMode() || panel.chess.getNext() == null)
						return;
					int result = JOptionPane.showConfirmDialog(panel,
							"确定要将" + panel.chess.getNext() + "交给电脑操作吗？", "确认",
							JOptionPane.YES_NO_OPTION);
					if (result == JOptionPane.YES_OPTION) {
						panel.stopIdleTimer();
						panel.chess.changeToAi();
					}
				}
			});

			m_his.add(new JMenuItem("悔棋")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Player p = panel.chess.rollback();
					if (p != null && !p.isHuman()) {
						if (p.getOpp().isHuman()) {
							panel.chess.rollback();
						}
					}
					panel.resetIdleTimer();
					panel.repaint();
				}
			});
			m_his.add(new JMenuItem("保存棋局")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					File file = Util.fileSaveDialog("Save as...", JFileChooser.FILES_ONLY, null);
					if (file != null) {
						panel.chess.save(file);
					}
				}
			});
			m_his.add(new JMenuItem("加载棋局")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					File file = Util.fileOpenDialog("Open...", JFileChooser.FILES_ONLY, null);
					if (file != null) {
						panel.chess.load(file);
						panel.resetIdleTimer();
						panel.repaint();
					}
				}
			});

			m_review.add(new JMenuItem("从头观看过程")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.review(0);
					panel.stopIdleTimer();
					panel.repaint();
				}
			});
			m_review.add(new JMenuItem("退出复盘状态")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.exitReview(false);
					panel.repaint();
				}
			});
			m_review.add(new JMenuItem("前进")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.reviewNext();
					panel.repaint();
				}
			});
			m_review.add(new JMenuItem("后退")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.reviewPrev();
					panel.repaint();
				}
			});
			m_review.add(new JMenuItem("从当前局面开始下棋")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.exitReview(true);
					panel.resetIdleTimer();
					panel.repaint();
				}
			});

			JCheckBoxMenuItem soundToggle = new JCheckBoxMenuItem("音效", SoundManager.isEnabled());
			soundToggle.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					SoundManager.setEnabled(soundToggle.isSelected());
				}
			});

			JCheckBoxMenuItem forbiddenToggle = new JCheckBoxMenuItem("黑方禁手", panel.chess.forbiddenMoveRule);
			forbiddenToggle.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.forbiddenMoveRule = forbiddenToggle.isSelected();
				}
			});

			// 网页对局服务
			final WebServer webServer = new WebServer(panel.chess, () -> {
				javax.swing.SwingUtilities.invokeLater(() -> panel.repaint());
			});
			JCheckBoxMenuItem webToggle = new JCheckBoxMenuItem("网页对局", false);
			// 收集需要在网页模式下变灰的菜单
			final JMenu[] greyMenus = {m_main, m_diff, m_auto, m_his, m_review};
			webToggle.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (webToggle.isSelected()) {
						try {
							webServer.start(8080);
							panel.webMode = true;
							panel.stopIdleTimer();
							// 菜单变灰
							for (JMenu m : greyMenus) m.setEnabled(false);
							setTitle(GAME_VERSION_STR + " [网页模式 http://localhost:8080]");
							panel.repaint();
							JOptionPane.showMessageDialog(panel,
									"网页对局已开启！\n\n"
									+ "地址: http://localhost:8080\n\n"
									+ "在浏览器中选择对局模式开始游戏\n"
									+ "Swing界面已暂停操作",
									"网页对局", JOptionPane.INFORMATION_MESSAGE);
						} catch (Exception ex) {
							webToggle.setSelected(false);
							JOptionPane.showMessageDialog(panel,
									"启动失败: " + ex.getMessage(),
									"错误", JOptionPane.ERROR_MESSAGE);
						}
					} else {
						webServer.stop();
						panel.webMode = false;
						// 恢复菜单
						for (JMenu m : greyMenus) m.setEnabled(true);
						panel.resetIdleTimer();
						setTitle(GAME_VERSION_STR);
						// 棋盘保持网页模式下的最终状态，不重置
						panel.repaint();
					}
				}
			});

			m_help.add(soundToggle);
			m_help.add(forbiddenToggle);
			m_help.add(webToggle);
			m_help.addSeparator();
			m_help.add(new JMenuItem("排行榜")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					showRankingDialog(panel);
				}
			});
			m_help.addSeparator();
			m_help.add(new JMenuItem("关于")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(panel, GAME_VERSION_STR + "\n\nhzjiyi@gmail.com");
				}
			});
			menuBar.add(m_main);
			menuBar.add(m_diff);
			menuBar.add(m_auto);
			menuBar.add(m_his);
			menuBar.add(m_review);
			menuBar.add(m_help);
			this.setJMenuBar(menuBar);
			panel.setReviewMenu(false);
		}

		private void showRankingDialog(ChessPanel panel) {
			java.util.List<Ranking.Record> list = GobangGame.ranking.getRecords();
			StringBuilder sb = new StringBuilder();
			sb.append("<html><body style='font-family:微软雅黑; margin:6px;'>");
			sb.append("<div style='text-align:center; font-size:14px; font-weight:bold; padding:4px 0 8px 0'>🏆 人机对局排行榜</div>");
			if (list.isEmpty()) {
				sb.append("<p style='text-align:center;color:gray'>暂无记录，赢一局人机对局即可上榜</p>");
			} else {
				sb.append("<table border='0' cellpadding='5' cellspacing='0' width='100%'>");
				sb.append("<tr style='background:#4a4a4a;color:#fff;font-size:11px'>");
				sb.append("<th width='45'>名次</th><th>玩家</th><th width='50'>得分</th>");
				sb.append("<th width='45'>难度</th><th width='40'>执子</th><th width='40'>步数</th><th width='38'>禁手</th></tr>");
				for (int i = 0; i < list.size(); i++) {
					Ranking.Record r = list.get(i);
					String bg = i < 3 ? "#fff8e1" : (i % 2 == 0) ? "#f8f8f8" : "#eeeeee";
					String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : String.valueOf(i + 1);
					String colorStr = "BLACK".equals(r.color) ? "●黑" : "○白";
					String scoreStyle = i < 3 ? "font-size:11px;color:#c62828" : "font-size:10px";
					sb.append("<tr style='background:").append(bg).append(";font-size:12px'>");
					sb.append("<td align='center'>").append(medal).append("</td>");
					sb.append("<td>").append(r.name).append("</td>");
					sb.append("<td align='right' style='").append(scoreStyle).append("'><b>").append(r.score).append("</b></td>");
					sb.append("<td align='center'>").append(r.level).append("</td>");
					sb.append("<td align='center'>").append(colorStr).append("</td>");
					sb.append("<td align='center'>").append(r.steps).append("</td>");
					sb.append("<td align='center'>").append(r.forbidden ? "✓" : "—").append("</td>");
					sb.append("</tr>");
				}
				sb.append("</table>");
			}
			sb.append("</body></html>");
			javax.swing.JEditorPane editorPane = new javax.swing.JEditorPane("text/html", sb.toString());
			editorPane.setEditable(false);
			editorPane.setBackground(Color.WHITE);
			javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(editorPane);
			// 根据记录数动态调整高度，最多显示到满榜
			int rowH = 32;
			int headerH = 74;
			int contentH = headerH + Math.min(list.size(), 10) * rowH;
			int height = Math.min(contentH + 10, 560);
			scroll.setPreferredSize(new java.awt.Dimension(420, height));
			scroll.setBorder(javax.swing.BorderFactory.createLineBorder(new Color(200, 200, 200)));
			scroll.getViewport().setBackground(Color.WHITE);
			JOptionPane.showMessageDialog(panel, scroll, "排行榜", JOptionPane.PLAIN_MESSAGE);
		}
	}
}


class ChessPanel extends JPanel {
	private static final long serialVersionUID = -4677980500938864107L;

	private final ImageIcon blackChess;
	private final ImageIcon whiteChess;

	private static final Color STATUS_BG_PLAYING = new Color(50, 50, 50);
	private static final Color STATUS_BG_WIN = new Color(0, 120, 60);
	private static final Color STATUS_BG_LOSE = new Color(160, 40, 40);
	private static final Color STATUS_BG_DRAW = new Color(100, 100, 100);
	private static final Color STATUS_BG_REVIEW = new Color(40, 80, 140);
	private static final Color STATUS_TEXT = new Color(255, 255, 255);
	private static final Color STATUS_TEXT_DIM = new Color(180, 180, 180);
	private static final Color BOARD_LINE = new Color(60, 40, 20);
	private static final Color COORD_COLOR = new Color(100, 70, 40);
	private static final Color WIN_LINE_COLOR = new Color(220, 40, 40, 200);
	private static final Font STATUS_FONT = new Font("微软雅黑", Font.BOLD, 14);
	private static final Font COORD_FONT = new Font("Consolas", Font.PLAIN, 11);
	private static final int STATUS_H = 32;

	JMenu reviewMenu;
	Chess chess;
	/** 网页模式开启时，Swing棋盘暂停交互 */
	boolean webMode = false;

	// 鼠标悬停位置（棋盘坐标），-1表示无效
	private int hoverX = -1, hoverY = -1;

	private Timer idleTimer;
	private static final int IDLE_TIMEOUT = 30_000;

	public ChessPanel(int x, int y) {
		blackChess = loadIcon("/black.gif");
		whiteChess = loadIcon("/white.gif");

		chess = new Chess(this, x, y);
		chess.initGame(true, false);

		idleTimer = new Timer(IDLE_TIMEOUT, e -> {
			if (chess.getNext() != null && chess.getNext().isHuman() && !chess.isReviewMode()) {
				SoundManager.playReminder();
			}
		});
		idleTimer.setRepeats(true);
		idleTimer.start();

		// 鼠标悬停追踪
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				int newX = Math.round((e.getX() - 50) / 30.0f);
				int newY = Math.round((e.getY() - 50) / 30.0f);
				if (newX < 0 || newX >= chess.width || newY < 0 || newY >= chess.height) {
					newX = -1;
					newY = -1;
				}
				if (newX != hoverX || newY != hoverY) {
					hoverX = newX;
					hoverY = newY;
					repaint();
				}
			}
		});

		addMouseListener(new MouseAdapter() {
			// 记录按下位置，用于判断是否为短距离拖拽（视为点击）
			private int pressX, pressY;
			private static final int CLICK_TOLERANCE = 10; // 像素

			public void mousePressed(MouseEvent e) {
				pressX = e.getX();
				pressY = e.getY();
			}

			public void mouseExited(MouseEvent e) {
				if (hoverX != -1) {
					hoverX = -1;
					hoverY = -1;
					repaint();
				}
			}

			public void mouseReleased(MouseEvent e) {
				// 网页模式下暂停Swing棋盘交互
				if (webMode) return;

				// 移动距离超过阈值视为拖拽，忽略
				int dx = e.getX() - pressX;
				int dy = e.getY() - pressY;
				if (dx * dx + dy * dy > CLICK_TOLERANCE * CLICK_TOLERANCE) return;

				if (chess.isAutoRunning()) {
					chess.next.human = true;
					return;
				}

				// 游戏结束后点击：提示重新开始
				if (chess.getNext() == null && !chess.isReviewMode()) {
					int result = JOptionPane.showConfirmDialog(ChessPanel.this,
							"游戏已结束，是否重新开始？", "重新开始",
							JOptionPane.YES_NO_OPTION);
					if (result == JOptionPane.YES_OPTION) {
						chess.initGame(true, false);
						SoundManager.playGameStart();
						resetIdleTimer();
						repaint();
					}
					return;
				}

				if (chess.isReviewMode()) {
					if (e.getButton() == MouseEvent.BUTTON3) {
						chess.reviewPrev();
					} else {
						chess.reviewNext();
					}
					ChessPanel.this.repaint();
				} else if (e.getButton() == MouseEvent.BUTTON1 && chess.next != null) {
					int bx = Math.round((e.getX() - 50) / 30.0f);
					int by = Math.round((e.getY() - 50) / 30.0f);
					if (bx >= 0 && bx < chess.width && by >= 0 && by < chess.height)
						if (chess.getTable()[bx][by] == 0) {
							// 禁手检查：人类执黑时提示而非直接判负
							if (chess.forbiddenMoveRule && chess.next == Player.BLACK
									&& chess.isForbidden(bx, by)) {
								SoundManager.playReminder();
								JOptionPane.showMessageDialog(ChessPanel.this,
										"此处为禁手（三三/四四/长连），黑方不能落子！",
										"禁手", JOptionPane.WARNING_MESSAGE);
								return;
							}
							chess.doMove(new Point(bx, by));
							SoundManager.playPlace();
							if (chess.next != null && !chess.next.isHuman()) {
								chess.computerMove();
								SoundManager.playPlace();
							}
							resetIdleTimer();
							ChessPanel.this.repaint();
							checkGameOver();
						}
				}
			}
		});
	}

	/** 对局进行中时，开始新游戏前确认 */
	boolean confirmNewGame() {
		if (chess.getNext() != null && chess.his.count() > 0 && !chess.isReviewMode()) {
			int result = JOptionPane.showConfirmDialog(this,
					"当前对局尚未结束，确定要开始新游戏吗？", "确认",
					JOptionPane.YES_NO_OPTION);
			return result == JOptionPane.YES_OPTION;
		}
		return true;
	}

	void checkGameOver() {
		if (chess.getNext() == null) {
			stopIdleTimer();
			if (chess.winner == null) {
				SoundManager.playDraw();
			} else if (chess.winner.isHuman()) {
				SoundManager.playVictory();
				// 人机对局胜利时记录得分
				if (chess.defaultAiLevel != null) {
					String levelName = chess.defaultAiLevel.name;
					String color = chess.winner == Player.BLACK ? "BLACK" : "WHITE";
					int steps = chess.his.count();
					boolean forbidden = chess.forbiddenMoveRule;
					int score = Ranking.calcScore(steps, levelName, forbidden);
					String name = JOptionPane.showInputDialog(this,
							"恭喜获胜！得分: " + score + "\n请输入你的名字：",
							"上榜", JOptionPane.PLAIN_MESSAGE);
					if (name != null && !name.trim().isEmpty()) {
						int rank = GobangGame.ranking.addRecord(name.trim(), score, levelName, color, steps, forbidden);
						if (rank > 0) {
							JOptionPane.showMessageDialog(this,
									"得分 " + score + "，排名第 " + rank + " 名！",
									"排行榜", JOptionPane.INFORMATION_MESSAGE);
						}
					}
				}
			} else {
				SoundManager.playDefeat();
			}
		}
	}

	void resetIdleTimer() { idleTimer.restart(); }
	void stopIdleTimer() { idleTimer.stop(); }

	private ImageIcon loadIcon(String path) {
		URL url = GobangGame.class.getResource(path);
		return url != null ? new ImageIcon(url) : new ImageIcon();
	}

	public void paintComponent(Graphics g0) {
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

		int bw = chess.width;
		int bh = chess.height;
		int widthPx = (bw - 1) * 30 + 50;
		int heightPx = (bh - 1) * 30 + 50;

		// 棋盘线
		g.setColor(BOARD_LINE);
		for (int j = 0; j < bh; j++)
			g.drawLine(50, 50 + j * 30, widthPx, 50 + j * 30);
		for (int j = 0; j < bw; j++)
			g.drawLine(50 + j * 30, 50, 50 + j * 30, heightPx);

		// 星位点
		if (bw >= 13 && bh >= 13) {
			int cx = bw / 2, cy = bh / 2, d3 = 3;
			int[][] stars = {{cx, cy}, {d3, d3}, {bw-1-d3, d3}, {d3, bh-1-d3}, {bw-1-d3, bh-1-d3}};
			for (int[] s : stars) {
				if (s[0] >= 0 && s[0] < bw && s[1] >= 0 && s[1] < bh)
					g.fillOval(50 + s[0] * 30 - 3, 50 + s[1] * 30 - 3, 7, 7);
			}
		}

		// 坐标
		g.setFont(COORD_FONT);
		g.setColor(COORD_COLOR);
		FontMetrics cfm = g.getFontMetrics();
		for (int i = 0; i < bw; i++) {
			String num = Integer.toString(i);
			g.drawString(num, 50 + 30 * i - cfm.stringWidth(num) / 2, 44);
		}
		for (int i = 1; i < bh; i++)
			g.drawString(Integer.toString(i), 33, 54 + 30 * i);

		drawPieces(g);
		drawHoverPreview(g);
		drawWinLine(g);
		drawStatusBar(g);

		// 网页模式下覆盖半透明遮罩和提示
		if (webMode) {
			Composite oldComp = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
			g.setColor(new Color(40, 40, 40));
			g.fillRect(0, STATUS_H + 4, getWidth(), getHeight() - STATUS_H - 4);
			g.setComposite(oldComp);

			g.setColor(new Color(200, 200, 200));
			g.setFont(new Font("微软雅黑", Font.BOLD, 20));
			FontMetrics fm2 = g.getFontMetrics();
			String hint = "网页对局模式已开启";
			g.drawString(hint, (getWidth() - fm2.stringWidth(hint)) / 2, getHeight() / 2 - 10);
			g.setFont(new Font("微软雅黑", Font.PLAIN, 14));
			fm2 = g.getFontMetrics();
			String hint2 = "请在浏览器中操作  http://localhost:8080";
			g.drawString(hint2, (getWidth() - fm2.stringWidth(hint2)) / 2, getHeight() / 2 + 20);
		}
	}

	private void drawPieces(Graphics2D g) {
		int bw = chess.width, bh = chess.height;
		int[][] board = chess.getTable();

		for (int i = 0; i < bw; i++) {
			for (int j = 0; j < bh; j++) {
				ImageIcon icon = null;
				if (board[i][j] == 1) icon = blackChess;
				else if (board[i][j] == 2) icon = whiteChess;
				if (icon != null) {
					g.drawImage(icon.getImage(), i * 30 + 31, j * 30 + 31,
							icon.getIconWidth() - 3, icon.getIconHeight() - 3, this);
				}
			}
		}

		// 最新落子标记：在棋子中心画一个对比色小圆点
		// 标记最近两步（人机对局时人类和AI各一步都可见）
		Point last = chess.his.getLast();
		Point prev = chess.his.getPrevLast();
		if (last != null) {
			drawMoveMarker(g, last, chess.his.getLastPlayer() == Player.BLACK);
		}
		if (prev != null) {
			drawMoveMarker(g, prev, chess.his.getLastPlayer() != Player.BLACK);
		}

		if (chess.printStep && last != null) {
			Util.print(board, last, null);
		}
	}

	/** 在棋子中心画对比色小圆点标记 */
	private void drawMoveMarker(Graphics2D g, Point p, boolean isBlack) {
		int cx = p.x * 30 + 50, cy = p.y * 30 + 50;
		g.setColor(isBlack ? Color.WHITE : Color.BLACK);
		g.fillOval(cx - 4, cy - 4, 8, 8);
	}

	/** 鼠标悬停时绘制半透明预影棋子，禁手点显示红色X标记 */
	private void drawHoverPreview(Graphics2D g) {
		if (hoverX < 0 || chess.getNext() == null || chess.isReviewMode()) return;
		if (!chess.getNext().isHuman()) return;
		if (chess.getTable()[hoverX][hoverY] != 0) return;

		int px = hoverX * 30 + 31, py = hoverY * 30 + 31;

		// 禁手点标记
		if (chess.forbiddenMoveRule && chess.getNext() == Player.BLACK
				&& chess.isForbidden(hoverX, hoverY)) {
			Composite oldComp = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
			Stroke oldStroke = g.getStroke();
			g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(new Color(220, 40, 40));
			int cx = hoverX * 30 + 50, cy = hoverY * 30 + 50;
			g.drawLine(cx - 8, cy - 8, cx + 8, cy + 8);
			g.drawLine(cx - 8, cy + 8, cx + 8, cy - 8);
			g.setStroke(oldStroke);
			g.setComposite(oldComp);
			return;
		}

		Composite oldComp = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));

		ImageIcon icon = chess.getNext() == Player.BLACK ? blackChess : whiteChess;
		g.drawImage(icon.getImage(), px, py,
				icon.getIconWidth() - 3, icon.getIconHeight() - 3, this);

		g.setComposite(oldComp);
	}

	/** 胜利时绘制连线高亮 */
	private void drawWinLine(Graphics2D g) {
		Point[] winPts = chess.getWinPoints();
		if (winPts == null) return;

		// 以交叉点坐标 (i*30+50, j*30+50) 为棋子视觉中心
		int r = 14; // 高亮圆半径
		Composite oldComp = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
		g.setColor(WIN_LINE_COLOR);
		for (Point p : winPts) {
			g.fillOval(p.x * 30 + 50 - r, p.y * 30 + 50 - r, r * 2, r * 2);
		}
		g.setComposite(oldComp);

		// 连线贯穿首尾棋子中心
		Stroke oldStroke = g.getStroke();
		g.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(WIN_LINE_COLOR);
		Point first = winPts[0], last = winPts[4];
		g.drawLine(first.x * 30 + 50, first.y * 30 + 50,
				last.x * 30 + 50, last.y * 30 + 50);
		g.setStroke(oldStroke);
	}

	private void drawStatusBar(Graphics2D g) {
		int panelW = getWidth();

		Color bgColor;
		if (chess.getNext() == null) {
			if (chess.winner == null) bgColor = STATUS_BG_DRAW;
			else if (chess.winner.isHuman()) bgColor = STATUS_BG_WIN;
			else bgColor = STATUS_BG_LOSE;
		} else if (chess.isReviewMode()) {
			bgColor = STATUS_BG_REVIEW;
		} else {
			bgColor = STATUS_BG_PLAYING;
		}

		g.setPaint(new GradientPaint(0, 0, bgColor, 0, STATUS_H, bgColor.darker()));
		g.fillRoundRect(4, 2, panelW - 8, STATUS_H, 8, 8);

		g.setFont(STATUS_FONT);
		FontMetrics fm = g.getFontMetrics();

		if (chess.getNext() == null) {
			String msg = getEndMessage();
			g.setColor(STATUS_TEXT);
			g.drawString(msg, 14, 2 + (STATUS_H + fm.getAscent() - fm.getDescent()) / 2);
		} else {
			Player next = chess.getNext();
			String label = "下一手";
			int textY = 2 + (STATUS_H + fm.getAscent() - fm.getDescent()) / 2;

			g.setColor(STATUS_TEXT);
			g.drawString(label, 14, textY);

			int circleX = 14 + fm.stringWidth(label) + 8;
			int circleY = 2 + (STATUS_H - 18) / 2;
			if (next == Player.BLACK) {
				g.setColor(Color.BLACK);
				g.fillOval(circleX, circleY, 18, 18);
				g.setColor(new Color(80, 80, 80));
				g.drawOval(circleX, circleY, 18, 18);
			} else {
				g.setColor(Color.WHITE);
				g.fillOval(circleX, circleY, 18, 18);
				g.setColor(new Color(160, 160, 160));
				g.drawOval(circleX, circleY, 18, 18);
			}

			String info = "第 " + chess.his.count() + " 手";
			if (chess.isReviewMode()) {
				info += "  ▶ 复盘中";
			} else if (chess.defaultAiLevel != null) {
				info += "  [" + chess.defaultAiLevel.name + "]";
			}
			if (chess.forbiddenMoveRule) {
				info += "  [禁手]";
			}
			if (webMode) {
				info += "  [网页模式]";
			}
			g.setColor(STATUS_TEXT_DIM);
			int infoW = fm.stringWidth(info);
			g.drawString(info, panelW - 14 - infoW, textY);
		}
	}

	private String getEndMessage() {
		if (chess.winner == null)
			return "⚔ 不分胜负，棋逢对手！点击棋盘重新开始";
		if (chess.winner.isHuman())
			return "🏆 恭喜获胜！点击棋盘重新开始";
		if (chess.winner.getOpp().isHuman())
			return "💀 电脑获胜，点击棋盘再来一局";
		return "⚡ " + (chess.winner == Player.BLACK ? "黑方" : "白方") + "获胜！";
	}

	void setReviewMenu(boolean enable) {
		reviewMenu.getMenuComponent(0).setEnabled(!enable);
		for (int i = 1; i < reviewMenu.getMenuComponentCount(); i++)
			reviewMenu.getMenuComponent(i).setEnabled(enable);
	}
}
