package com.example.pjw.quzzer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class subActivity extends AppCompatActivity {

private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);
        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        ImageButton ibtn = (ImageButton)findViewById(R.id.ibtn);

        ibtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(getApplicationContext(), "버튼 눌림" + name, Toast.LENGTH_SHORT).show();
            }
        });

    }
}
