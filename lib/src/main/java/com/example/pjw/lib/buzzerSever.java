package com.example.pjw.lib;

import java.awt.BorderLayout;
import java.io.BufferedReader;
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
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Handler;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;


class screen extends JFrame{
    public static JTextArea t2;
    public static JPanel pb;




    screen(){
        setSize(2000,1300);
        setTitle("퀴즈부저");
        makeUI();
        setVisible(true);
    }
    private void makeUI(){

        JPanel pa = new JPanel();
        pb = new JPanel();
        JPanel pc = new JPanel();
        add(pa, BorderLayout.NORTH);
        add(pb, BorderLayout.CENTER);
        add(pc, BorderLayout.SOUTH);

        pa.setBorder(BorderFactory.createTitledBorder("당첨"));
        pa.add(new JTextArea(30,150));

        pb.setBorder(BorderFactory.createTitledBorder("참가자"));
        t2 = new JTextArea(40,70);
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



//ServerThread 클래스는 쓰레드 클래스를 상속받아 별도의 클래스로 생성

class ServerThread extends Thread{
    private Boolean loop;
    private final ServerSocket server;
    private final ExecutorService pool;

    //List 는 핸들러로 실시간 클라이언트와 연결상태 관리
    //리스트 요소는 개별 스레드로부터 언제든 삭제 추가가 가능하므로 Lock 기능이 필요
    private final List<Handler> threadList;
    private final ReentrantLock lock;
    private  final Vector<String> list;
    public ServerThread(int port, int poolSize) throws IOException{
        super();
        server = new ServerSocket(port);
        server.setSoTimeout(3000); // 공간이 교실 하나정도 크기면 3초면 적당
         list = new Vector<String>();
        //실행되는 스레드 수를 제한하기 위해 스레드 풀 사용
        pool = Executors.newFixedThreadPool(poolSize);

        //ExcutorService 객체를 삭제한다고 해서 현재 실행중인 개별 태스크는 삭제되지 않음
        //따라서 개별 태스크를 삭제 시키기 위해 리스트 객체를 생성
        threadList = new ArrayList<Handler>();
        lock = new ReentrantLock();
        loop = true;
    }// 생성자

    @Override
    public void run(){
        System.out.println("접속을 기다립니다.");
        while (loop){
            try{
                //타임아웃이 발생하면 InterruptedIOException 예외가 발생
                Handler h = new Handler(server.accept());
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
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));

                //윈도우에서 인코딩은 MS949를 사용한다

                BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String line = null;

                try{
                    //클라이언트로부터 메시지가 올 때까지 대기한다.
                    while ((line = br.readLine()) != null){
                        if(list.contains(line)){
                            list.removeElement(line);

                            screen.t2.setText("");
                            screen.t2.append(list+"\n");
                            screen.pb.add(screen.t2);
                        }
                        else {
                            list.addElement(line);
                            screen.t2.setText("");
                            screen.t2.append(list + "\n");
                            screen.pb.add(screen.t2);
                        }
                        System.out.println(" 클라이언트로부터 전송받은 문자열: "+line);

                        //클라이언트로부터 'quit'메세지가 온다면 작업을 종료시킨다.
                        //소켓을 닫는다면 readLine() 메소드에서 IOException 예외가 발생한다.
                        if (line.equalsIgnoreCase("quit")) quit();

                        pw.println(line);
                        pw.flush();
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


                        sock.close();
                    }
                }
            }catch (Exception ex){
                System.out.println(ex);
            }

        }// run
    } // Handler 내부 클래스



}// ServerThread 클래스