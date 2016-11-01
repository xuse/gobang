import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * 代码重构，算法不变，可读性增强
 * 
 * @author jiyi
 * 
 */
public class GobangGame {
	private static final String GAME_VERSION_STR = "五子棋游戏 版本1.13";

	public static void main(String[] args) {
		GameFrame game = new GameFrame();
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}
		game.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		game.setVisible(true);
	}

	static class GameFrame extends JFrame {
		public GameFrame() {
			int width = 13;
			int height = 13;
			Container contentPane = getContentPane();

			final ChessPanel panel = new ChessPanel(width, height);

			panel.setBackground(new Color(188, 190, 170));
			contentPane.setBackground(new Color(148, 80, 126));
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
			m_main.add(new JMenuItem("开始游戏(执黑)")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.initGame(true, false);
					panel.repaint();
				}
			});

			m_main.add(new JMenuItem("开始游戏(执白)")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.initGame(false, true);
					panel.repaint();
				}
			});

			m_main.add(new JMenuItem("开始游戏(自动)")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.initGame(false, false);
					panel.repaint();
					panel.chess.startAuto(100);
				}
			});
			m_main.add(new JMenuItem("开始游戏(双人对局)")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.initGame(true, true);
					panel.repaint();
				}
			});
			m_main.add(new JMenuItem("重定义棋盘大小")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String str = JOptionPane.showInputDialog(panel,"请输入 '宽x高'");
					int index = str == null ? 0 : str.indexOf('x');
					if (index > 0) {
						int x = Integer.parseInt(str.substring(0, index));
						int y = Integer.parseInt(str.substring(index + 1));
						int size = x * y;
						if (x < 0 || y < 0) {
							JOptionPane.showMessageDialog(panel, "请不要输入负数。");
							return;
						} else if (size < 25) {
							JOptionPane.showMessageDialog(panel,"棋盘太小，无意义。");
							return;
						} else if (size > 10000) {
							JOptionPane.showMessageDialog(panel,"棋盘太大，不支持。");
							return;
						}
						panel.chess.resetSize(x, y);
						int widthPx = (x - 1) * 30 + 100;
						int heightPx = (y - 1) * 30 + 130;
						setSize(widthPx, heightPx);
						panel.chess.initGame(true, false);
						panel.repaint();
					}
				}
			});
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
					Player next = panel.chess.getNext();
					if (next != null && !next.isHuman()) {
						panel.chess.computerMove(); // 如果下一步是电脑下，那么就先下子然后再重新绘制
					}
					panel.repaint();

				}
			});
			m_auto.add(new JMenuItem("电脑托管")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (panel.chess.isReviewMode() || panel.chess.getNext() == null)
						return;
					int result = JOptionPane.showConfirmDialog(panel, "确定要将" + panel.chess.getNext() + "交给电脑操作吗？", "确认", JOptionPane.YES_NO_OPTION);
					if (result == JOptionPane.YES_OPTION) {
						panel.chess.changeToAi();
					}
				}
			});

			m_his.add(new JMenuItem("悔棋")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Player p = panel.chess.rollback();
					if (p != null && !p.isHuman()) {// 如果回退的是电脑下的子，……
						if (p.getOpp().isHuman()) { // 并且另外一方是人脑的话，那么将人脑下的子一并回退。
							panel.chess.rollback();
						}
					}
					panel.repaint();
				}
			});
			// 保存文件，注意可以保存尚未结束的棋局，以备重新开始
			m_his.add(new JMenuItem("保存棋局")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					File file = Util.fileSaveDialog("Save as...", JFileChooser.FILES_ONLY, null);
					if (file != null) {
						panel.chess.save(file);
					}

				}
			});
			// 加载文件，恢复到当前状态
			m_his.add(new JMenuItem("加载棋局")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					File file = Util.fileOpenDialog("Save as...", JFileChooser.FILES_ONLY, null);
					if (file != null) {
						panel.chess.load(file);
						panel.repaint();
					}
				}
			});
			// 将棋盘和状态回滚到开头第一子的状态，复盘模式
			m_review.add(new JMenuItem("从头观看过程")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.review(0);
					panel.repaint();
				}
			});

			// 将回到复盘前的状态
			m_review.add(new JMenuItem("退出复盘状态")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.exitReview(false);
					panel.repaint();
				}
			});

			// 在复盘模式下进一步
			m_review.add(new JMenuItem("前进")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.reviewNext();
					panel.repaint();
				}
			});
			// 在复盘模式下退一步
			m_review.add(new JMenuItem("后退")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.reviewPrev();
					panel.repaint();
				}
			});
			// 从当前盘面开始对弈
			m_review.add(new JMenuItem("从当前局面开始下棋")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					panel.chess.exitReview(true);
					panel.repaint();
				}
			});
			m_help.add(new JMenuItem("关于")).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(panel, GAME_VERSION_STR + "\n\n\rhzjiyi@gmail.com");
				}
			});
			menuBar.add(m_main);
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
	private URL blackImgURL = GobangGame.class.getResource("black.gif");
	private ImageIcon blackChess = new ImageIcon(blackImgURL);
	private URL whiteImgURL = GobangGame.class.getResource("white.gif");
	private ImageIcon whiteChess = new ImageIcon(whiteImgURL);
	private URL currentImgURL = GobangGame.class.getResource("current.gif");
	private ImageIcon whiteCurrent = new ImageIcon(currentImgURL);
	private URL currentBImgURL = GobangGame.class.getResource("black1.gif");
	private ImageIcon blackCurrent = new ImageIcon(currentBImgURL);
	JMenu reviewMenu;
	Chess chess;

	public ChessPanel(int x, int y) {
		chess = new Chess(this, x, y);
		chess.initGame(true, false);
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (chess.isAutoRunning()) {
					chess.next.human = true;
				}else if(chess.isReviewMode()){
					if(e.getButton()==MouseEvent.BUTTON3){
						chess.reviewPrev();
					}else{
						chess.reviewNext();
					}
					ChessPanel.this.repaint();
				}else if (chess.next != null) {
					int oldx = e.getX();
					int oldy = e.getY();
					int x, y;
					x = (oldx - 33) / 30;
					y = (oldy - 33) / 30;
					if (x >= 0 && x < chess.width && y >= 0 && y < chess.height)
						if (chess.getTable()[x][y] == 0) {
							chess.doMove(new Point(x, y));
							if (chess.next != null && !chess.next.isHuman()) {
								// 如果下一步是电脑下，那么就先下子然后再重新绘制
								chess.computerMove();
							}
							ChessPanel.this.repaint();
						}
				}
			}
		});
	}
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		int x = chess.width;
		int y = chess.height;
		int widthPx = (x - 1) * 30 + 50;
		int heightPx = (y - 1) * 30 + 50;
		// 绘制棋盘
		for (int j = 0; j < y; j++) {
			g.drawLine(50, 50 + j * 30, widthPx, 50 + j * 30);
		}
		for (int j = 0; j < x; j++) {
			g.drawLine(50 + j * 30, 50, 50 + j * 30, heightPx);
		}
		for (int i = 0; i < x; i++) {
			String number = Integer.toString(i);
			g.drawString(number, 46 + 30 * i, 45);
		}
		for (int i = 1; i < y; i++) {
			String number = Integer.toString(i);
			g.drawString(number, 33, 53 + 30 * i);
		}
		// 绘制棋子
		updatePaint(g);
		showMessage(g);
	}

	private void showMessage(Graphics g) {
		if (chess.getNext() == null) {
			String message = getEndMessage();
			g.drawString(message, 12, 25);
		}
	}

	private String getEndMessage() {
		String message;
		if (chess.winner == null) {
			message = "不分胜负，再战500回合!请重新来过..";
		} else {
			StringBuilder sb = new StringBuilder(chess.winner.toString());
			sb.append("胜利!");
			if (chess.winner.isHuman()) {
				sb.append("您武艺高强，小生甘拜！重新开始继续切磋..");
			} else if (chess.winner.getOpp().isHuman()) {
				sb.append("大侠一时失手，小生胜之不武!请重新来过..");
			}
			message = sb.toString();
		}
		return message;
	}

	private void updatePaint(Graphics g) {
		g.setFont(new Font("宋体", Font.BOLD, 18));
		g.setColor(Color.BLUE);
		int x = chess.width;
		int y = chess.height;
		int[][] chessBoard = chess.getTable();
		for (int i = 0; i < x; i++) {
			for (int j = 0; j < y; j++) {
				if (chessBoard[i][j] == 1) {
					g.drawImage(blackChess.getImage(), i * 30 + 31, j * 30 + 31, blackChess.getImage().getWidth(blackChess.getImageObserver()) - 3, blackChess.getImage().getHeight(blackChess.getImageObserver()) - 3, blackChess.getImageObserver());
				}
				if (chessBoard[i][j] == 2) {
					g.drawImage(whiteChess.getImage(), i * 30 + 31, j * 30 + 31, whiteChess.getImage().getWidth(whiteChess.getImageObserver()) - 3, whiteChess.getImage().getHeight(whiteChess.getImageObserver()) - 3, whiteChess.getImageObserver());
				}
			}
		}
		Point p = chess.his.getLast();
		if (p != null) {
			ImageIcon icon = chess.his.getLastPlayer() == Player.WHITE ? whiteCurrent : blackCurrent;
			g.drawImage(icon.getImage(), p.x * 30 + 31, p.y * 30 + 31, icon.getImage().getWidth(icon.getImageObserver()) - 4, icon.getImage().getHeight(icon.getImageObserver()) - 4, icon.getImageObserver());
		}
		if (chess.printStep) {
			Util.print(chessBoard, p, null);
		}
		if (chess.getNext() != null) {
			g.drawString("下一手      总手数 " + chess.his.count() + (chess.isReviewMode() ? "  复盘中" : ""), 14, 25);
			Color c = chess.getNext() == Player.BLACK ? Color.BLACK : Color.WHITE;
			g.setColor(c);
			g.fillOval(80, 5, 22, 22);
		}
	}
	void setReviewMenu(boolean enable) {
		reviewMenu.getMenuComponent(0).setEnabled(!enable);
		for (int i = 1; i < reviewMenu.getMenuComponentCount(); i++) {
			reviewMenu.getMenuComponent(i).setEnabled(enable);
		}
	}
}