import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
	private static final String GAME_VERSION_STR = "五子棋游戏 版本1.15";

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}
		GameFrame game = new GameFrame();
		game.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		game.setVisible(true);
	}

	static class GameFrame extends JFrame {
		private static final long serialVersionUID = 1L;

		public GameFrame() {
			int width = 13;
			int height = 13;
			Container contentPane = getContentPane();

			final ChessPanel panel = new ChessPanel(width, height);

			panel.setBackground(new Color(222, 184, 135)); // 木色棋盘
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
			// AI难度选项
			final String[] diffNames = {"简单", "中等", "困难"};
			final Class<?>[] diffClasses = {AI.Default.class, SmartEvalAI.class, SmartSearchAI.class};
			final int[] selectedDiff = {2}; // 默认困难

			JMenu m_diff = new JMenu("AI难度");
			javax.swing.ButtonGroup diffGroup = new javax.swing.ButtonGroup();
			for (int d = 0; d < diffNames.length; d++) {
				final int idx = d;
				javax.swing.JRadioButtonMenuItem item = new javax.swing.JRadioButtonMenuItem(diffNames[d], d == selectedDiff[0]);
				item.addActionListener(new ActionListener() {
					@SuppressWarnings("unchecked")
					public void actionPerformed(ActionEvent e) {
						selectedDiff[0] = idx;
						panel.aiDiffName = diffNames[idx];
						// 实时替换对局中电脑方的AI
						Class<? extends AI> clz = (Class<? extends AI>) diffClasses[idx];
						for (Player p : Player.values()) {
							if (!p.isHuman() && p.getAi() != null) {
								p.setAi(panel.chess.createAI(clz));
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
				@SuppressWarnings("unchecked")
				public void actionPerformed(ActionEvent e) {
					panel.aiDiffName = diffNames[selectedDiff[0]];
					panel.chess.initGame(true, false, (Class<? extends AI>) diffClasses[selectedDiff[0]]);
					panel.resetIdleTimer();
					panel.repaint();
				}
			});

			m_main.add(new JMenuItem("开始游戏(执白)")).addActionListener(new ActionListener() {
				@SuppressWarnings("unchecked")
				public void actionPerformed(ActionEvent e) {
					panel.aiDiffName = diffNames[selectedDiff[0]];
					panel.chess.initGame(false, true, (Class<? extends AI>) diffClasses[selectedDiff[0]]);
					panel.resetIdleTimer();
					panel.repaint();
				}
			});

			m_main.add(new JMenuItem("开始游戏(自动)")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.aiDiffName = null;
					panel.chess.initGame(false, false);
					panel.stopIdleTimer();
					panel.repaint();
					panel.chess.startAuto(100);
				}
			});
			m_main.add(new JMenuItem("开始游戏(双人对局)")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.aiDiffName = null;
					panel.chess.initGame(true, true);
					panel.resetIdleTimer();
					panel.repaint();
				}
			});
			m_main.add(new JMenuItem("重定义棋盘大小")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String str = JOptionPane.showInputDialog(panel, "请输入 '宽x高'");
					int index = str == null ? 0 : str.indexOf('x');
					if (index > 0) {
						int x = Integer.parseInt(str.substring(0, index));
						int y = Integer.parseInt(str.substring(index + 1));
						int size = x * y;
						if (x < 0 || y < 0) {
							JOptionPane.showMessageDialog(panel, "请不要输入负数。");
							return;
						} else if (size < 25) {
							JOptionPane.showMessageDialog(panel, "棋盘太小，无意义。");
							return;
						} else if (size > 10000) {
							JOptionPane.showMessageDialog(panel, "棋盘太大，不支持。");
							return;
						}
						panel.chess.resetSize(x, y);
						int widthPx = (x - 1) * 30 + 100;
						int heightPx = (y - 1) * 30 + 130;
						setSize(widthPx, heightPx);
						panel.chess.initGame(true, false);
						panel.resetIdleTimer();
						panel.repaint();
					}
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

			// 音效开关
			JCheckBoxMenuItem soundToggle = new JCheckBoxMenuItem("音效", SoundManager.isEnabled());
			soundToggle.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					SoundManager.setEnabled(soundToggle.isSelected());
				}
			});

			m_help.add(soundToggle);
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
	}
}


class ChessPanel extends JPanel {
	private static final long serialVersionUID = -4677980500938864107L;

	// 棋子图片
	private final ImageIcon blackChess;
	private final ImageIcon whiteChess;
	private final ImageIcon whiteCurrent;
	private final ImageIcon blackCurrent;

	// 状态栏配色
	private static final Color STATUS_BG_PLAYING = new Color(50, 50, 50);
	private static final Color STATUS_BG_WIN = new Color(0, 120, 60);
	private static final Color STATUS_BG_LOSE = new Color(160, 40, 40);
	private static final Color STATUS_BG_DRAW = new Color(100, 100, 100);
	private static final Color STATUS_BG_REVIEW = new Color(40, 80, 140);
	private static final Color STATUS_TEXT = new Color(255, 255, 255);
	private static final Color STATUS_TEXT_DIM = new Color(180, 180, 180);
	private static final Color BOARD_LINE = new Color(60, 40, 20);
	private static final Color COORD_COLOR = new Color(100, 70, 40);
	private static final Font STATUS_FONT = new Font("微软雅黑", Font.BOLD, 14);
	private static final Font COORD_FONT = new Font("Consolas", Font.PLAIN, 11);
	private static final int STATUS_H = 32;

	JMenu reviewMenu;
	Chess chess;
	String aiDiffName = "困难"; // 当前AI难度名称，用于状态栏显示

	// 等待提醒计时器：30秒无操作提醒一次
	private Timer idleTimer;
	private static final int IDLE_TIMEOUT = 30_000;

	public ChessPanel(int x, int y) {
		blackChess = loadIcon("/black.gif");
		whiteChess = loadIcon("/white.gif");
		whiteCurrent = loadIcon("/white_new.gif");
		blackCurrent = loadIcon("/black_new.gif");

		chess = new Chess(this, x, y);
		chess.initGame(true, false);

		// 等待提醒计时器
		idleTimer = new Timer(IDLE_TIMEOUT, e -> {
			if (chess.getNext() != null && chess.getNext().isHuman() && !chess.isReviewMode()) {
				SoundManager.playReminder();
			}
		});
		idleTimer.setRepeats(true);
		idleTimer.start();

		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (chess.isAutoRunning()) {
					chess.next.human = true;
				} else if (chess.isReviewMode()) {
					if (e.getButton() == MouseEvent.BUTTON3) {
						chess.reviewPrev();
					} else {
						chess.reviewNext();
					}
					ChessPanel.this.repaint();
				} else if (chess.next != null) {
					int oldx = e.getX();
					int oldy = e.getY();
					int bx = (oldx - 33) / 30;
					int by = (oldy - 33) / 30;
					if (bx >= 0 && bx < chess.width && by >= 0 && by < chess.height)
						if (chess.getTable()[bx][by] == 0) {
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

	/** 检查游戏是否结束并播放对应音效 */
	void checkGameOver() {
		if (chess.getNext() == null) {
			stopIdleTimer();
			if (chess.winner == null) {
				SoundManager.playDraw();
			} else if (chess.winner.isHuman()) {
				SoundManager.playVictory();
			} else {
				SoundManager.playDefeat();
			}
		}
	}

	void resetIdleTimer() {
		idleTimer.restart();
	}

	void stopIdleTimer() {
		idleTimer.stop();
	}

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

		// 绘制棋盘线
		g.setColor(BOARD_LINE);
		for (int j = 0; j < bh; j++) {
			g.drawLine(50, 50 + j * 30, widthPx, 50 + j * 30);
		}
		for (int j = 0; j < bw; j++) {
			g.drawLine(50 + j * 30, 50, 50 + j * 30, heightPx);
		}

		// 星位点（如果棋盘够大）
		if (bw >= 13 && bh >= 13) {
			int cx = bw / 2, cy = bh / 2;
			int d3 = 3;
			int[][] stars = {{cx, cy}, {d3, d3}, {bw - 1 - d3, d3}, {d3, bh - 1 - d3}, {bw - 1 - d3, bh - 1 - d3}};
			for (int[] s : stars) {
				if (s[0] >= 0 && s[0] < bw && s[1] >= 0 && s[1] < bh) {
					g.fillOval(50 + s[0] * 30 - 3, 50 + s[1] * 30 - 3, 7, 7);
				}
			}
		}

		// 坐标标注
		g.setFont(COORD_FONT);
		g.setColor(COORD_COLOR);
		for (int i = 0; i < bw; i++) {
			String num = Integer.toString(i);
			FontMetrics fm = g.getFontMetrics();
			int tw = fm.stringWidth(num);
			g.drawString(num, 50 + 30 * i - tw / 2, 44);
		}
		for (int i = 1; i < bh; i++) {
			String num = Integer.toString(i);
			g.drawString(num, 33, 54 + 30 * i);
		}

		// 绘制棋子
		drawPieces(g);

		// 绘制状态栏
		drawStatusBar(g);
	}

	private void drawPieces(Graphics2D g) {
		int bw = chess.width;
		int bh = chess.height;
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

		// 最后一手高亮
		Point p = chess.his.getLast();
		if (p != null) {
			ImageIcon icon = chess.his.getLastPlayer() == Player.WHITE ? whiteCurrent : blackCurrent;
			g.drawImage(icon.getImage(), p.x * 30 + 31, p.y * 30 + 31,
					icon.getIconWidth() - 4, icon.getIconHeight() - 4, this);
		}

		if (chess.printStep && p != null) {
			Util.print(board, p, null);
		}
	}

	private void drawStatusBar(Graphics2D g) {
		int panelW = getWidth();

		// 背景
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

		// 渐变背景
		g.setPaint(new GradientPaint(0, 0, bgColor, 0, STATUS_H, bgColor.darker()));
		g.fillRoundRect(4, 2, panelW - 8, STATUS_H, 8, 8);

		g.setFont(STATUS_FONT);
		FontMetrics fm = g.getFontMetrics();

		if (chess.getNext() == null) {
			// 游戏结束
			String msg = getEndMessage();
			g.setColor(STATUS_TEXT);
			g.drawString(msg, 14, 2 + (STATUS_H + fm.getAscent() - fm.getDescent()) / 2);
		} else {
			// 游戏进行中 / 复盘中
			Player next = chess.getNext();
			String label = "下一手";
			int textY = 2 + (STATUS_H + fm.getAscent() - fm.getDescent()) / 2;

			g.setColor(STATUS_TEXT);
			g.drawString(label, 14, textY);

			// 棋子指示圆
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

			// 右侧信息
			String info = "第 " + chess.his.count() + " 手";
			if (chess.isReviewMode()) {
				info += "  ▶ 复盘中";
			} else if (aiDiffName != null) {
				info += "  [" + aiDiffName + "]";
			}
			g.setColor(STATUS_TEXT_DIM);
			int infoW = fm.stringWidth(info);
			g.drawString(info, panelW - 14 - infoW, textY);
		}
	}

	private String getEndMessage() {
		if (chess.winner == null) {
			return "⚔ 不分胜负，棋逢对手！请重新来过";
		}
		if (chess.winner.isHuman()) {
			return "🏆 恭喜获胜！武艺高强，甘拜下风";
		}
		if (chess.winner.getOpp().isHuman()) {
			return "💀 电脑获胜，大侠一时失手，请再接再厉";
		}
		// 电脑 vs 电脑
		return "⚡ " + (chess.winner == Player.BLACK ? "黑方" : "白方") + "获胜！";
	}

	void setReviewMenu(boolean enable) {
		reviewMenu.getMenuComponent(0).setEnabled(!enable);
		for (int i = 1; i < reviewMenu.getMenuComponentCount(); i++) {
			reviewMenu.getMenuComponent(i).setEnabled(enable);
		}
	}
}
