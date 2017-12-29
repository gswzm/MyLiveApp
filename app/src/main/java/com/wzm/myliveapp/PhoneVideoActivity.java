package com.wzm.myliveapp;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.faucamp.simplertmp.RtmpHandler;
import com.seu.magicfilter.utils.MagicFilterType;

import net.ossrs.yasea.SrsCameraView;
import net.ossrs.yasea.SrsEncodeHandler;
import net.ossrs.yasea.SrsPhoneCameraView;
import net.ossrs.yasea.SrsPhonePublisher;
import net.ossrs.yasea.SrsRecordHandler;

import java.io.IOException;
import java.net.SocketException;
import java.util.Date;

/**
 * 类名： PhoneVideoActivity
 * 时间：2017/12/29 15:07
 * 描述：
 * 修改人：
 * 修改时间：
 * 修改备注：
 *
 * @author wangzm
*/
public class PhoneVideoActivity extends AppCompatActivity implements SrsEncodeHandler.SrsEncodeListener, RtmpHandler.RtmpListener, SrsRecordHandler.SrsRecordListener{

    private SrsPhoneCameraView srsPhoneCamera;

    private Button btStopShot;

    private static final String TAG="PhoneVideoActivity";
    private SrsPhonePublisher mPublisher;
    private String rtmpUrl;
    private Handler mHanler=null;
    private boolean isException=false;
    private boolean isClick =false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_video);
        rtmpUrl=getIntent().getStringExtra("url");
        initView();
        initUIHandler();
        initEvent();
    }

    private void initView(){
        btStopShot =(Button)findViewById(R.id.bt_stop_shot);
        srsPhoneCamera=(SrsPhoneCameraView)findViewById(R.id.srs_phone_camera);

        mPublisher=new SrsPhonePublisher(srsPhoneCamera);
        //编码状态回调

        mPublisher.setEncodeHandler(new SrsEncodeHandler(this));
        mPublisher.setRecordHandler(new SrsRecordHandler(this));
        //rtmp推流状态回调
        mPublisher.setRtmpHandler(new RtmpHandler(this));
        //预览分辨率
        mPublisher.setPreviewResolution(1280, 720);
        //推流分辨率
//        mPublisher.setOutputResolution(640, 480);
        mPublisher.setOutputResolution(480, 640);
        //传输率
        mPublisher.setVideoHDMode();
//        //开启美颜（其他滤镜效果在MagicFilterType中查看）
        mPublisher.switchCameraFilter(MagicFilterType.BEAUTY);
        //软编码
//        mPublisher.switchToSoftEncoder();
        //硬编码
        mPublisher.switchToHardEncoder();
        //后置摄像头
        mPublisher.switchCameraFace(0);
        //打开摄像头，开始预览（未推流）
        mPublisher.startCamera();
        mPublisher.startPublish(rtmpUrl);


    }

    private void initEvent(){

        btStopShot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isClick){
                    mPublisher.startPublish(rtmpUrl);
                    mPublisher.startCamera();
                    btStopShot.setText("停止");
                    isClick=false;
                }else{
                    mPublisher.stopPublish();
                    mPublisher.stopRecord();
                    btStopShot.setText("开始");
                    isClick=true;
                }
            }
        });

    }


    private void initUIHandler(){
        mHanler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case 11:
                        mPublisher.stopPublish();
                        mPublisher.stopRecord();
                        btStopShot.setText("开始");
                        break;
                    case 12:
                        mPublisher.startCamera();
                        mPublisher.startPublish(rtmpUrl);
                        break;
                }
            }
        };
    }
    @Override
    protected void onResume() {
        super.onResume();
        mPublisher.resumeRecord();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: to hear");
        mPublisher.stopPublish();
        mPublisher.stopRecord();
    }
    @Override
    public void onNetworkWeak() {
        Toast.makeText(getApplicationContext(), "网络信号弱", Toast.LENGTH_SHORT).show();
//        breakpointHttp();
    }
    @Override
    public void onNetworkResume() {

    }

    private void handleException(Exception e) {
        try {
            Log.d(TAG, "handleException: e="+e.getMessage());
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            mPublisher.stopPublish();
            mPublisher.stopRecord();
            btStopShot.setText("开始");
            isException=true;
//            breakpointHttp();
        } catch (Exception e1) {
            //
        }
    }
    @Override
    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }
    @Override
    public void onRtmpConnecting(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        isException=false;
    }

    @Override
    public void onRtmpConnected(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoStreaming() {

    }

    @Override
    public void onRtmpAudioStreaming() {

    }

    @Override
    public void onRtmpStopped() {
        Toast.makeText(getApplicationContext(), "已停止", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onRtmpDisconnected() {
        Toast.makeText(getApplicationContext(), "服务已关闭", Toast.LENGTH_SHORT).show();
        if(isException){
            breakpointHttp();
        }
    }

    @Override
    public void onRtmpVideoFpsChanged(double fps) {

    }

    @Override
    public void onRtmpVideoBitrateChanged(double bitrate) {

    }

    @Override
    public void onRtmpAudioBitrateChanged(double bitrate) {

    }

    @Override
    public void onRtmpSocketException(SocketException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalStateException(IllegalStateException e) {
        handleException(e);
    }

    @Override
    public void onRecordPause() {
        Toast.makeText(getApplicationContext(), "Record paused", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordResume() {
        Toast.makeText(getApplicationContext(), "Record resumed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordStarted(String msg) {
        Toast.makeText(getApplicationContext(), "Recording file: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordFinished(String msg) {
        Toast.makeText(getApplicationContext(), "MP4 file saved: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRecordIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    /**
     * 断点续传
     */

    private int currentCount=0;
    private void breakpointHttp(){

        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "run: 定时器 start");
                currentCount++;
                if(currentCount>20){
                    mHanler.sendEmptyMessage(11);
                }else {
                    mHanler.sendEmptyMessage(12);
                }

            }
        }.start();
    }
}
