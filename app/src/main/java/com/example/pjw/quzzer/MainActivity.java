package com.example.pjw.quzzer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class MainActivity extends AppCompatActivity {
    Dialog buzzer;
    public static Activity Main_Activity;
    public static ImageButton buzzerbtn;
    Context context;
    //일반 스레드로 실행되는 작업의 결과를 화면에 출력시키기 위해 메인 핸들러 사용
    private Handler mMainHandler;

    //데이터를 서버에 전송  또는 일반 스레드를 종료시키는 기능을 수행하는 루퍼 변수와 핸들러스레드 변수선언
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private HandlerThread thread;

    //서버로부터 데이터 수신받는 기능을 수행하는 일반스레드 변수선언
    private TCPClient client = null;
    private Socket socket;
    private BufferedReader networkReader = null;
    private BufferedWriter networkWriter = null;

    //화면의 버튼과 텍스트 변수 선언
    private Button connect;
    private Button finish;
    private Button start;
    private TextView text;

    private String ip;
    private String name;
    private int port = 6000;
    private String TAG = "MainActivity";

    //핸들러가 수행하는 기능은 아래 상수에 의해 결정
    public static final int MSG_CONNECT=1;
    public static final int MSG_STOP=2;
    public static final int MSG_CLIENT_STOP=3;
    public static final int MSG_SERVER_STOP=4;
    public static final int MSG_START=5;
    public static final int MSG_READY=6;
    public static final int MSG_ERROR=999;

    @Override
    public void onStop(){
        Message msg = mServiceHandler.obtainMessage();
        msg.what = MSG_START;
        String finishname = name + "finish";
        msg.obj = finishname;

        //핸들러스레드를 통해 문자를 서버에 전달
        mServiceHandler.sendMessage(msg);

        super.onStop();
    }



    @Override
    public void onDestroy(){



        //화면을 닫으면 소케과 스레드 모두 종료
        super.onDestroy();
        //소켓 객체를 닫는 close() 메소드는 일반 스레드로 실행해야함-메인스레드에서 실행시 오류-4웨이 핸드쉐이크 때문
        //따라서 핸들러 호출
        if (client != null){



            mServiceHandler.sendEmptyMessage(MSG_STOP);
        }
        //핸들러스레드 종료
        thread.quit();
        SystemClock.sleep(100); //작업이 완료될 때까지 0.1초 작업 대기
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText eip= (EditText) findViewById(R.id.editText1);
        final EditText eport= (EditText) findViewById(R.id.editText2);
        final EditText et= (EditText) findViewById(R.id.editText3);
        final EditText ename= (EditText) findViewById(R.id.editText4);


        Main_Activity = MainActivity.this;
        connect = (Button)findViewById(R.id.button1);
        finish =  (Button)findViewById(R.id.button2);
        start = (Button)findViewById(R.id.button3);
        text = (TextView)findViewById(R.id.textView1);
        context = this;
        connect.setEnabled(true);
        finish.setEnabled(false);
        start.setEnabled(false);

        //핸들러스레드 생성하고 실행
        thread = new HandlerThread("HandlerThread");
        thread.start();

        //핸들러스레드로 부터 루퍼 얻기
        mServiceLooper = thread.getLooper();

        //핸들러스레드가 제공한 루퍼객체를 매개변수로 해서 핸들러 객체를 생성
        mServiceHandler = new ServiceHandler(mServiceLooper);

        //메인 스레드에서 사용하는 핸들러
        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String m;
                switch (msg.what) {
                    case MSG_CONNECT:
                        m = "정상적으로 서버에 접속하였습니다.";

                        //종료와 전달버튼 활서화 & 연결버튼 비활성화-연결버튼 누를때 마다 스레드 계속 생성되기 방지
                        connect.setEnabled(false);
                        finish.setEnabled(true);
                        start.setEnabled(true);
                        text.setText(m);

                        //다이얼로그 생성
                        buzzer = new Dialog(MainActivity.this);
                        buzzer.setContentView(R.layout.buzzerdialog);
                          buzzerbtn = (ImageButton)buzzer.findViewById(R.id.buzzerbtn);

                            buzzerbtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    Message msg = mServiceHandler.obtainMessage();
                                    msg.what = MSG_START;
                                    String name2 = name+"button";
                                    msg.obj = name2;

                                    //핸들러스레드를 통해 문자를 서버에 전달
                                     mServiceHandler.sendMessage(msg);
                                    Log.d(TAG, "부저 누를때 보내는 메세지  "+name2);
                                    buzzerbtn.setEnabled(false);
                                /*     try{

                                         Thread.sleep(3000);
                                         buzzerbtn.setEnabled(true);
                                     }catch (InterruptedException e){
                                         e.printStackTrace();
                                     }

*/
                                    Toast.makeText(getApplicationContext(), "버튼 눌림" + name2, Toast.LENGTH_SHORT).show();
                                }
                            });

                        buzzer.show();
//다이얼로그 크기 변경
                        Display display = getWindowManager().getDefaultDisplay();  //디스클레이 해상도가져오기
                        Point size = new Point();
                        display.getSize(size);
                        Window window = buzzer.getWindow();  //비율에 맞게 다이얼로그 크기 지정
                        int x = (int)(size.x*0.99f);  //가로길이의 99%로 늘리기
                        int y = (int)(size.y*0.99f);
                        window.setLayout(x,y);

                        break;

                    case MSG_READY:
                        //당첨 다이얼로그 사라졌을 때 메세지 수신
                        buzzerbtn.setEnabled(true);
                        m = "버튼 누를 준비 완료!";
                        break;

                    case MSG_CLIENT_STOP:
                        //사용자가 연결 작업 종료시
                        text.setText((String) msg.obj);
                        connect.setEnabled(true);
                        finish.setEnabled(false);
                        start.setEnabled(false);
                        m="클라이언트가 접속을 종료하였습니다.";


                        break;

                    case MSG_SERVER_STOP:
                        //서버에 의해 연결이 종료될 때
                        text.setText((String) msg.obj);
                        connect.setEnabled(true);
                        finish.setEnabled(false);
                        start.setEnabled(false);
                        m="서버가 접속을 종료하였습니다.";

                        //서브액티비티를 지칭하는 객체생성후 종료
                       // subActivity SA = (subActivity)subActivity.sub_Activity;
                       // SA.finish();
                        buzzer.dismiss();
                        break;

                    case MSG_START:
                        //메세지를 전송하였을 때 호출
                        m = "메세지 전송 완료!";
                        text.setText((String) msg.obj);
                        break;

                    default:
                        //에러 발생시 호출
                        m="에러 발생!";
                        text.setText((String) msg.obj);
                        break;
                }
                Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show();
                super.handleMessage(msg);
            }
        };

        //연결버튼 누를 때 실행
        connect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View V){


                ip = eip.getText().toString();
                try{
                    //포트번호는 양수여야 함
                    port = Integer.parseInt(eport.getText().toString());
                    name = ename.getText().toString();
                }catch (NumberFormatException e){
                    port = 6000;
                    Log.d(TAG, "포트번호", e);
                }
                if (client == null) {
                    try{
                        //스레드 생성하고 서버IP 주소와 포트번호 넘겨주기
                        client = new TCPClient(ip, port);
                        client.start();
                        Log.d(TAG, "통신스레드시작");



                    }catch (RuntimeException e){
                        text.setText("IP주소나 포트번호가 잘못되었습니다..");
                        Log.d(TAG, "에러발생", e);
                    }
                }
            }
        });

        finish.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View V){
                if (ename.getText().toString() != null){
                    //핸들러로부터 메시지 하나 반환받는다(new로 생성하는 것이 아닌 핸들러에 있는 msg 가져오기)
                    Message msg = mServiceHandler.obtainMessage();
                    msg.what = MSG_START;
                    String finishname = ename.getText().toString() + "finish";
                    msg.obj = finishname;

                    //핸들러스레드를 통해 문자를 서버에 전달
                    mServiceHandler.sendMessage(msg);
                }
                if (client !=null){


                    //핸들러스레드를 사용하여 스레드 종료
                    mServiceHandler.sendEmptyMessage(MSG_CLIENT_STOP);

                }
            }
        });

        //데이터를 서버에 전송

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                if (et.getText().toString() != null){
                    //핸들러로부터 메시지 하나 반환받는다(new로 생성하는 것이 아닌 핸들러에 있는 msg 가져오기)
                    Message msg = mServiceHandler.obtainMessage();
                    msg.what = MSG_START;
                    msg.obj = et.getText().toString();

                    //핸들러스레드를 통해 문자를 서버에 전달
                    mServiceHandler.sendMessage(msg);
                }
            }
        });



    }

    //핸들러스레드에서 사용하는 핸들러
    private final class ServiceHandler extends Handler{
        public ServiceHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case MSG_START:
                    Message toMain = mMainHandler.obtainMessage();
                    try{
                        networkWriter.write((String) msg.obj);
                        networkWriter.newLine();
                        networkWriter.flush();

                        //메시지가 성공적으로 전송되었다면 전송한 문자열을 화면에 출력
                        toMain.what = MSG_START;
                    }catch (IOException e){
                        toMain.what = MSG_ERROR;
                        Log.d(TAG, "에러발생",e);
                    }
                    toMain.obj = msg.obj;
                    mMainHandler.sendMessage(toMain);
                    break;
                case MSG_STOP:
                case MSG_CLIENT_STOP:
                case MSG_SERVER_STOP:
                    client.quit();
                    client = null;
                    break;
            }
        }
    }

    //일반스레드
    public class TCPClient extends Thread{
        Boolean loop;
        SocketAddress socketAddress;
        String line;
        private final int connection_timeout = 6000;  //타임아웃 시간 6초
        public TCPClient(String ip, int port){
            //아이피 주소와 포트번호를 이용하여 SocketAddress 객체를 만든다
            socketAddress = new InetSocketAddress(ip, port);
        }

        @Override
        public void run(){
            try{
                //아래 소켓 객체를 스레드 생성자에서 만들어도 되지만
                //아이피 주소와 포트번호를 사용하여 서버에 연결요청을 한다면 run() 안에서 구현해야 한다.
                socket = new Socket();
                socket.setSoTimeout(connection_timeout);
                socket.setSoLinger(true, connection_timeout);

                //블록모드로 작동하는 connect() 메소드에서 반환되면 서버와 정상적으로 연결된 것으로 인식할 수 있다.
                socket.connect(socketAddress, connection_timeout);
                networkWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                InputStreamReader i = new InputStreamReader(socket.getInputStream());
                networkReader = new BufferedReader(i);

                //위의 모든 작업이 정상적으로 수행되면 화면으로 연결되었음을 알린다.
                Message toMain = mMainHandler.obtainMessage();
                toMain.what = MSG_CONNECT;
                mMainHandler.sendMessage(toMain);





              //  Intent intent = new Intent(MainActivity.this, subActivity.class);
              //  intent.putExtra("name", name);
             //   startActivity(intent);
                Log.d(TAG, "액티비티 2시작");


                //핸들러로부터 메시지 하나 반환받는다(new로 생성하는 것이 아닌 핸들러에 있는 msg 가져오기)
                Message msg = mServiceHandler.obtainMessage();
                msg.what = MSG_START;
                String connectname = name+"connect";
                msg.obj = connectname;
                Log.d(TAG, "서버전달시작");
                //핸들러스레드를 통해 문자를 서버에 전달
                mServiceHandler.sendMessage(msg);
                Log.d(TAG, "서버전달 완료");


                loop = true;
            }catch (Exception e){
                loop = false;
                Log.d(TAG, "에러발생", e);
                Message toMain = mMainHandler.obtainMessage();
                toMain.what = MSG_ERROR;
                toMain.obj = "소켓을 생성하지 못했습니다.";
                mMainHandler.sendMessage(toMain);
            }

            while (loop){
                try{
                    line = networkReader.readLine();

                    //서버로부터 FIN 패킷을 수신하면 read() 메소드는 null을 반환
                    Log.d(TAG, "받은메세지"+ line);
                        if(line.contains("delete")){
                        mMainHandler.sendEmptyMessage(MSG_READY);
                    }else if (line == null){
                            break;}

                    //읽어들인 문자열은 화면 출력을 위해 Runnable 객체와 핸들러를 사용하였다.
                    //메시지를 사용하지 않은 이유는 단지 이렇게 하더라도 작동하는지 보여주기 위함
                    Runnable showUpdate = new Runnable() {
                        @Override
                        public void run() {
                            text.setText(line);
                        }
                    };
                    mMainHandler.post(showUpdate);
                }catch (InterruptedIOException e){
                }catch (IOException e){

                    //소켓을 닫거나 또는 네트워크에 문제가 발생하면 대기하고 있던 read() 메소드에서 예외 발생
                    Log.d(TAG, "에러발생", e);
                    break;
                }
            }
            //루프에서 빠져나오면 스레드 종료를 위해 소켓 객체와 스트림 객체를 닫는다.
            try{
                if (networkWriter != null){
                    networkWriter.close();
                    networkWriter = null;
                }
                if (networkReader != null){
                    networkReader.close();
                    networkReader = null;
                }
                if (socket != null){
                    socket.close();
                    socket = null;
                }
                client = null;

                //서버로 부터 FIN패킷을 받았는지 아니면 사용자가 종료 버튼을 눌렀는지 여부를 loop 변수로 판단한다.
                if (loop){
                    //loop 가 true 라면 서버에 의한 종료로 간주한다
                    loop = false;
                    Message toMain = mMainHandler.obtainMessage();
                    toMain.what = MSG_SERVER_STOP;
                    toMain.obj = "네트워크가 끊어졌습니다.";
                    mMainHandler.sendMessage(toMain);
                }
            }catch (IOException e){
                Log.d(TAG, "에러발생", e);
                Message toMain = mMainHandler.obtainMessage();
                toMain.what = MSG_ERROR;
                toMain.obj = "소켓을 닫지 못했습니다..";
                mMainHandler.sendMessage(toMain);
            }
        }

        public void quit() {
            loop = false;
            try{
                if (socket != null){
                    socket.close();
                    socket = null;
                    Message toMain = mMainHandler.obtainMessage();
                    toMain.what = MSG_CLIENT_STOP;
                    toMain.obj = "접속을 중단합니다.";
                    mMainHandler.sendMessage(toMain);
                }// 서버에서 정상적으로 닫을 때까지 대기한다.
            }catch (IOException e){
                Log.d(TAG, "에러발생", e);
            }
        }

    }
}
