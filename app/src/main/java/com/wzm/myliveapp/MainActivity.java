package com.wzm.myliveapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

/**
 * 类名： MainActivity
 * 时间：2017/12/29 15:07
 * 描述：
 * 修改人：
 * 修改时间：
 * 修改备注：
 *
 * @author wangzm
*/
public class MainActivity extends AppCompatActivity {

    private String rtmpUrl="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void phoneCamera(View view) {
        startActivity(new Intent(this,PhoneVideoActivity.class).putExtra("url",rtmpUrl));
    }
    public void externalCamera(View view) {
        startActivity(new Intent(this,ExternalVideoActivity.class).putExtra("url",rtmpUrl));
    }
}
