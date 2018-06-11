package com.example.pjw.quzzer;

import android.app.Activity;
import android.content.Intent;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class subActivity extends AppCompatActivity {

private String name;
public static Activity sub_Activity;  //서브액티비티인 자신을 지칭하는 객체변수 선언  <- 메인액티비티에서 서브액티비티 종료하기위해 선언

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);
        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        ImageButton ibtn = (ImageButton)findViewById(R.id.ibtn);

        sub_Activity = subActivity.this;  //onCreate 안에서 그 변수가 나 자신이라는 것을 입증.

        ibtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // MainActivity MA = (MainActivity)MainActivity.Main_Activity;
               // Message msg = mServiceHandler.obtainMessage();
              //  msg.what = MSG_START;
              //  msg.obj = name;

                //핸들러스레드를 통해 문자를 서버에 전달
             //  mServiceHandler.sendMessage(msg);
                Toast.makeText(getApplicationContext(), "버튼 눌림" + name, Toast.LENGTH_SHORT).show();
            }
        });

    }
}
