package com.example.zhandezheng.filesync;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Button btn1;
    Button btn2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn1 = (Button) findViewById(R.id.btn1);
        btn1.setOnClickListener(new btListener());

        btn2 = (Button) findViewById(R.id.btn2);
        btn2.setOnClickListener(new btListener());
    }

    private class btListener implements View.OnClickListener{  //自定义监听类，继承OnClickListener
        public void onClick(View view){                        //实现方法
            // TODO Auto-generated method stub
            switch (view.getId()) {
                case R.id.btn1:
                    Toast.makeText(MainActivity.this, "This is Button 111", Toast.LENGTH_SHORT).show();
                    break;

                case R.id.btn2:
                    // TODO Auto-generated method stub
                    //创建需要对应目标Activity的intent
                    Intent intent=new Intent(MainActivity.this,MainActivity2.class);
                    //启动指定Activity并等待返回的结果，0是请求码。用于表示该请求
                    startActivity(intent);
                    break;
                default:
                    break;
            }
        }
    }
}
