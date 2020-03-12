package com.lrs.camera2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.lrs.camera2.camera.CameraActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class VideoActivity extends AppCompatActivity {

    Button btget;
    LinearLayout llview;
    TextView tvtext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        llview = findViewById(R.id.llview);
        btget = findViewById(R.id.btget);
        tvtext = findViewById(R.id.tvtext);
        btget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //人脸验证
//                Intent intent = new Intent(VideoActivity.this, TestActivity.class);
//                startActivity(intent);
                Intent intent = new Intent(VideoActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });
        EventBus.getDefault().register(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    /**
     * 接受返回状态
     */
    @Subscribe
    public void eventsbusMessage(MessageEvent event) {
        switch (event.getId()) {
            case 200:
//                if (event.getBitmap() != null) {
//                    ImageView imageView = new ImageView(this);
//                    imageView.setImageBitmap(event.getBitmap());
//                    llview.addView(imageView, 2);
//                }
                tvtext.setText("身份验证成功，可以开始学习");
                tvtext.setTextColor(getResources().getColor(R.color.color_green));
                break;
        }
    }
}
