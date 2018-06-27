package com.example.pjw.lib;

import com.sun.media.sound.JavaSoundAudioClip;
import com.sun.media.sound.Toolkit;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.awt.Color;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Font;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;


class MyDialog extends JDialog{

    public MyDialog(JFrame frame, String title){
        super(frame, title);
        //setLayout(null);

        setSize(2000,600);


    }
}


class screen extends JFrame{
    public static JTextArea t1, t2, first;
    public static JPanel pa, pb ;

    public static JTextPane firstpush;



    screen(){
        setSize(2000,1300);
        setTitle("퀴즈부저");
        makeUI();
        setVisible(true);


    }
//경고음 출력
    public static void bsound() throws Exception{

    String sfile = "/Users/sbt/Desktop/buzzer.au";
    InputStream in = new FileInputStream(sfile);
        AudioStream audioStream = new AudioStream(in);
        AudioPlayer.player.start(audioStream);

    }


   // public static JTextArea first;
     static Timer timer;
     static MyDialog dialog;


     public static void dialogTimer(String string){
         firstpush = new JTextPane();
         first = new JTextArea();

        JFrame dt = new JFrame();
        dialog = new MyDialog( dt , "1등은 바로");

         firstpush.setText(string);
         firstpush.setFont(new Font("굴림체",Font.BOLD,300));
         firstpush.setBackground(Color.YELLOW);
         //텍스트 중앙정렬
         StyledDocument doc = firstpush.getStyledDocument();
         SimpleAttributeSet center = new SimpleAttributeSet();
         StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
         doc.setParagraphAttributes(0, doc.getLength(), center, false);
        // firstpush.setMargin(new Insets(0,100,0,0));

         dialog.add(firstpush);

        dialog.setVisible(true);

        try {
            Thread.sleep(3000);
            dialog.setVisible(false);
            ServerThread.firstlist.clear();
           // ServerThread.pw.println("delete");
            ServerThread.broadcast();
           // ServerThread.pw.flush();
        }catch (InterruptedException e ){}
/*
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                dialog.setVisible(false);
                ServerThread.firstlist.clear();
                ServerThread.pw.println("delete");
                ServerThread.pw.flush();
            }
        }, 3000, 100000);

        //timer.cancel();
 */
    }

    private void makeUI(){

       // pa = new JPanel();
        pb = new JPanel();
        JPanel pc = new JPanel();

       // add(pa, BorderLayout.NORTH);
        add(pb, BorderLayout.CENTER);
        add(pc, BorderLayout.SOUTH);

       // pa.setBorder(BorderFactory.createTitledBorder("당첨"));
       // t1 = new JTextArea(30,150);
      //  pa.add(t1);

        pb.setBorder(BorderFactory.createTitledBorder("참가자"));
        t2 = new JTextArea(40,70);
        t2.setFont(new Font("굴림체",Font.BOLD,30));

//        t2.append(line);
        pb.add(t2);

        pc.setBorder(BorderFactory.createTitledBorder("IP주소 입력"));
        JTextPane t3 = new JTextPane();
        StyledDocument doc = (StyledDocument) t3.getDocument();
        Style style = doc.addStyle("StyleName", null);
        StyleConstants.setFontSize(style, 100);
        try {
            InetAddress local = InetAddress.getLocalHost();
            doc.insertString(doc.getLength(), "IP주소 :  "+local.getHostAddress(), style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        pc.add(t3);

    }

}

public class buzzerSever {


    public  static void main(String[] args)
    {
        screen s = new screen();


        try{
            //서버소켓() 객체 생성시키는 스레드를 생성하고 실행
            ServerThread echothread = new ServerThread(6000, 10);
            echothread.start();

            //서버를 종료하고 자 한다면 'exit'의 입력을 받는다.
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter \"exit\" to quit.");

            //콘솔로부터 데이터가 입력될 때까지 대기
            String host;
            do{
                host = in.readLine();
            }while (! host.equalsIgnoreCase("exit"));

            //서버소켓을 닫고 스레드를 종료
            echothread.shutdown();
            echothread.join();
            System.out.println("Completed shutdown.");
        }catch (Exception e){
            //에러 발생시 작업종료
            System.err.println("Interruped before accept thread completed."+ e.toString());
            System.exit(1);
        }



    }//main


}

//부저버튼 눌렀을 때 브로드캐스트 해주는 쓰레드 생성

class BroadcastThread extends Thread{
    private PrintWriter pw2;
    private Socket sc;
    private BufferedReader br2;



    public BroadcastThread(Socket sc) throws IOException{

            this.sc = sc;

            //this.br2 = ServerThread.br;
           // this.pw2 = ServerThread.pw;
            InputStream ins = sc.getInputStream();
            OutputStream os = sc.getOutputStream();
            br2 = new BufferedReader(new InputStreamReader(ins));
            pw2 = new PrintWriter(new OutputStreamWriter(os),true);
          //  pw2.println("delete");
    }

    public void send() {

        pw2.println("delete");
        pw2.flush();
        System.out.println("Send OK"+pw2);
    }
}

//ServerThread 클래스는 쓰레드 클래스를 상속받아 별도의 클래스로 생성

class ServerThread extends Thread{
    public  BufferedReader br;
    private Boolean loop;
    public  ServerSocket server;
    private final ExecutorService pool;
    public   PrintWriter pw;
    //List 는 핸들러로 실시간 클라이언트와 연결상태 관리
    //리스트 요소는 개별 스레드로부터 언제든 삭제 추가가 가능하므로 Lock 기능이 필요
    private final List<Handler> threadList;
    private final ReentrantLock lock;
    private  final Vector<String> list;
    public  static Vector v;
    private  Socket sc;
    public  static List<String> firstlist;

   public static void broadcast(){
       System.out.println("broadcast test"+v);
       for(int i=0;i<v.size();i++){
           System.out.println("broadcast test"+v.elementAt(i));
           BroadcastThread st = (BroadcastThread) v.elementAt(i);
           st.send();

       }//for
   }//broadcast






    public ServerThread(int port, int poolSize) throws IOException{
        super();



        server = new ServerSocket(port);



        server.setSoTimeout(3000); // 공간이 교실 하나정도 크기면 3초면 적당

         list = new Vector<String>();
        //실행되는 스레드 수를 제한하기 위해 스레드 풀 사용
        pool = Executors.newFixedThreadPool(poolSize);
        v= new Vector();
        //ExcutorService 객체를 삭제한다고 해서 현재 실행중인 개별 태스크는 삭제되지 않음
        //따라서 개별 태스크를 삭제 시키기 위해 리스트 객체를 생성
        threadList = new ArrayList<Handler>();
        lock = new ReentrantLock();
        loop = true;
        firstlist = new ArrayList<String>();


    }// 생성자

    @Override
    public void run(){
        System.out.println("접속을 기다립니다.");
        while (loop){
            try{
                //타임아웃이 발생하면 InterruptedIOException 예외가 발생
                sc=server.accept(); //사용자마다 소켓을 배정
                BroadcastThread st = new BroadcastThread(sc);
                v.add(st);
                st.start();
                Handler h = new Handler(sc);


                lock.lock(); //요소 추가를 위해 잠금을 설정
                threadList.add(h);
                lock.unlock();

                pool.execute(h); //스레드풀 내부에서 태스크를 실행
            }catch (InterruptedIOException e){
                e.printStackTrace();
            }catch (IOException e){
                //타임아웃 이외의 예외 발생시 스페드풀 닫고 모든 작업 종료
                pool.shutdown();
                System.out.println("스레드에서 예외가 발생하였습니다."+e.toString());
                break;
            }
        }

        //서버소켓을 닫는다.
        try{
            server.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void shutdown() {
        pool.shutdown(); //스레드 풀을 닫는다.
        try {
            //작업이 완료될 때까지 0.5초 대기
            if (!pool.awaitTermination(500L, TimeUnit.MILLISECONDS)) {
                pool.shutdownNow(); //스레드 풀을 즉시 닫기
                if (!pool.awaitTermination(500L, TimeUnit.MILLISECONDS)) {
                    System.err.println("Pool did not terminate");
                    //스레드 풀이 해제되지 않는다는 것은 아직 클라이언트와 접속되어 있다는 것
                    //따라서 강제로 접속을 해제
                    clearlist();
                }
            }
        }catch (InterruptedException ie){
            pool.shutdownNow();
            clearlist();
        }
        //소켓서버의 모든 작업을 종료
        loop = false;
    }
    private void clearlist() {
        //리스트의 요소들을 찾아 강제 해제
        if (!threadList.isEmpty()){
            v.clear();
            lock.lock();
            for (int index = 0; index < threadList.size(); ++index){
                //태스크 내 quit() 메소드를 호출하여 작업 종료
                threadList.get(index).quit();
            }
            lock.unlock();
        }
    }


    // Handler 내부 클래스
class  Handler implements Runnable{

        private Socket sock;

    //외부에서 생성된 객체는 생성자의 매개변수로 전달받아 내부 로컬 변수에 입력하여 사용
        public Handler(Socket sock){
            this.sock = sock;


        }

        public void quit() {
            if(sock != null){
                try{
                    //소켓을 닫는다면 readLine() 메소드에서 IOException 예외가 발생함
                    sock.close();
                    sock = null;
                }catch (IOException e){
                    e.printStackTrace();
                }
                }
            }

            @Override
        public void run(){
            try{

                InetAddress inetaddr = sock.getInetAddress();
                System.out.println(inetaddr.getHostAddress() + "이 접속하였습니다.");

                OutputStream out = sock.getOutputStream();
                InputStream in = sock.getInputStream();
                pw = new PrintWriter(new OutputStreamWriter(out));

                //윈도우에서 인코딩은 MS949를 사용한다

                br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String line = null;
                int sNum = 0;
                try{

                    //클라이언트로부터 메시지가 올 때까지 대기한다.
                    while ((line = br.readLine()) != null){

                        if(line.contains("button")) {

                            System.out.println("buzzer push"+line);
                            firstlist.add(line);

                            if(line == firstlist.get(0)) {
                            screen.bsound();
                            screen.dialogTimer(firstlist.get(0).replace("button", ""));
                                firstlist.clear();
                            //screen.firstpush.getToolkit().beep();

                              }
                        }
                        else if(line.contains("connect")){

                                list.addElement(line.replace("connect",""));
                                screen.t2.setText("");
                                screen.t2.append(String.valueOf(list));
                                screen.pb.add(screen.t2);
                                System.out.println("list = "+list);
                            }

                            //screen.dialogTimer(line.replace("!!",""));



                        else if(line.contains("finish")){

                           //if (list.contains(line)) {
                                list.removeElement(line.replace("finish",""));

                                screen.t2.setText("");
                                screen.t2.append(String.valueOf(list));
                                screen.pb.add(screen.t2);


                          //  } else {
                          //      list.addElement(line);

                          /*  if(list.contains(line+confime)){

                                screen.first.setText(line);
                                screen.t1.append(line);
                                screen.pa.add(screen.t1);
                                System.out.println("받은메세지"+line);
                               // s.dialogTimer();

                            }
*/

                           //     screen.t2.setText("");
                          //      screen.t2.append(list + "\n");
                          //      screen.pb.add(screen.t2);


                        }

                        System.out.println(" 클라이언트로부터 전송받은 문자열: "+line);


                        //클라이언트로부터 'quit'메세지가 온다면 작업을 종료시킨다.
                        //소켓을 닫는다면 readLine() 메소드에서 IOException 예외가 발생한다.
                        if (line.equalsIgnoreCase("quit")) quit();

                       // pw.println(line);
                       // pw.flush();
                    }
                }catch (IOException e){

                    // quit() 메소드를 호출하여 소켓을 닫거나 또는 네트워크 내 문제가 발생시 예외처리
                }finally {

                    //객체 종료와 함께 리스트 객체 내 보관중인 요소에서 객체를 찾아 삭제
                    lock.lock();
                    threadList.remove(this);
                    lock.unlock();

                    System.out.println(inetaddr.getHostAddress()+"이 접속을 종료하였습니다.");
                    if(pw !=null) pw.close();
                    if(br !=null) br.close();
                    if(sock !=null){

                        v.clear();
                        sock.close();
                    }
                }
            }catch (Exception ex){
                System.out.println(ex);
            }

        }// run
    } // Handler 내부 클래스



}// ServerThread 클래스