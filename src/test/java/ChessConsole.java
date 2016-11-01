import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;


public class ChessConsole {
	private BufferedReader reader;
	private Chess chess;
	public static void main(String[] args) throws IOException {
		ChessConsole c=new ChessConsole();
		c.start();
	}

	private void start() throws IOException {
		reader=new BufferedReader(new InputStreamReader(System.in));
		chess=new Chess(null,13,13);
		initNew();
		state();
		while(true){
			String str=reader.readLine().trim();
			if(str.length()==0)continue;
			if("quit".equals(str)||"exit".equals(str)){
				System.out.println("Bye");
				break;
			}
			try{
				perform(str);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	private void perform(String str) {
		if("state".equals(str)||"st".equals(str)){
			state();
		}else if("new".equals(str)||"n".equals(str)){//新开
			initNew();
		}else if("cmove".equals(str)||"c".equals(str)){//自动下一步
			cmove();
		}else if("comp".equals(str)){//托管
			comp();
		}else if("roll".equals(str)|| "r".equals(str)){//回退
			rollback();
		}else if("test".equals(str)){//打印棋形
			test();
		}else if("review".equals(str)){//复盘
			chess.review(0);
			state();
		}else if("resume".equals(str)){//退出复盘
			chess.exitReview(false);
			state();
		}else if("play".equals(str)){//退出复盘，并按当前状态开始残局
			chess.exitReview(true);
			state();
		}else if(str.startsWith("load ")){//载入
			String filename=str.substring(5);
			chess.load(new File(filename));
		}else if(str.startsWith("save ")){//保存
			String filename=str.substring(5);
			chess.save(new File(filename));
			
			
		}else if(str.startsWith("chess ")){//重新定义大小
			str=str.substring(6).trim();
			int index=str.indexOf(',');
			int x=Integer.parseInt(str.substring(0,index));
			int y=Integer.parseInt(str.substring(index+1));
			this.chess=new Chess(null,x,y);
			initNew();
			state();
		}else if(str.indexOf(',')>-1){//落子
			if(chess.getNext()!=null){
				int index=str.indexOf(',');
				int x=Integer.parseInt(str.substring(0,index));
				int y=Integer.parseInt(str.substring(index+1));
				move(x,y);
			}else{
				System.out.println("棋局已经结束，请重新开局。");
			}
		}else{
			System.out.println("请输入可理解的命令");
		}
		
	}
	private void rollback() {
		chess.rollback();
		state();
	}

	private void comp() {
		Player next=chess.next;
		if(next==null)return;
		
		next.setAi(chess.createAI(next));
		if(next.getOpp().isHuman()){
			chess.computerMove();
		}else{
			try {
				chess.startAuto(0).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		state();
	}
	
	private void cmove() {
		if(chess.isReviewMode()){
			chess.reviewNext();
		}else if(chess.getNext()!=null){
			chess.computerMove();
			if(!chess.next.isHuman()){
				chess.computerMove();
			}
		}
		state();
	}

	private void move(int x, int y) {
		if(chess.isReviewMode()){
			chess.reviewNext();
		}else{
			chess.doMove(new Point(x,y));
			if(!chess.next.isHuman()){
				chess.computerMove();
			}	
		}
		state();
	}

	private void initNew() {
		chess.initGame(true, false);
		state();
	}

	private void state() {
		Util.print(chess.getTable(),chess.his.getLast(),chess.his.getPrevLast());
		if(chess.getNext()==null){
			System.out.println("已落子:"+ chess.his.count()+"  结束:"+(chess.winner==null?"平局":chess.winner+"胜利"));
		}else{
			System.out.println("已落子:"+ chess.his.count()+(chess.isReviewMode()?"(复盘模式)":"")+"\t下一手:"+chess.getNext());
		}
	}
	private void test(){
		int[][] chessBoard=new int[chess.width][chess.height];
		Pattern[] ps=chess.getPatterns();
		for(int i=0;i<ps.length;i++){
			Pattern p=ps[i];
			for(Point pt:p.points){
				chessBoard[pt.x][pt.y]=1;
			}
			Util.print(chessBoard,null,null);
			for(Point pt:p.points){
				chessBoard[pt.x][pt.y]=0;
			}
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("共计棋形"+ps.length+"个。");
	}
	
	
}
