package com.wzm.myliveapp;

import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.faucamp.simplertmp.RtmpHandler;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;
import com.seu.magicfilter.utils.MagicFilterType;

import net.ossrs.yasea.SrsCameraView;
import net.ossrs.yasea.SrsEncodeHandler;
import net.ossrs.yasea.SrsPublisher;
import net.ossrs.yasea.SrsRecordHandler;

import java.io.IOException;
import java.net.SocketException;
import java.util.Date;
import java.util.List;

public class ExternalVideoActivity extends AppCompatActivity implements RtmpHandler.RtmpListener,
        SrsRecordHandler.SrsRecordListener, SrsEncodeHandler.SrsEncodeListener{

    private static final String TAG = "ExternalVideoActivity";

    private static final int UI_EVENT_RECONNECT_SERVER = 10;
    Button btnPublish = null;
    private String rtmpUrl = "rtmp://192.168.10.55:1935/live/seam";
    private SrsPublisher mPublisher;
    private USBMonitor mUSBMonitor;
    private SrsCameraView srsCameraView;
    private boolean isClick = false;
    private boolean isException = false;

    private Handler mHanler = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external_video);
        initView();
    }

    private void initView(){
        initUIHandler();
        rtmpUrl = getIntent().getStringExtra("url");
        Log.i(TAG, rtmpUrl);
        btnPublish = (Button) findViewById(R.id.publish);

        srsCameraView = (SrsCameraView) findViewById(R.id.glsurfaceview_camera);
        mPublisher = new SrsPublisher(srsCameraView);

        mPublisher.setEncodeHandler(new SrsEncodeHandler(this));
        mPublisher.setRtmpHandler(new RtmpHandler(this));
        mPublisher.setRecordHandler(new SrsRecordHandler(this));

        //硬编码
//        mPublisher.switchToHardEncoder();
        //软编码
        mPublisher.switchToSoftEncoder();
        //预览分辨率
        mPublisher.setPreviewResolution(1280, 720);
//        //开启美颜（其他滤镜效果在MagicFilterType中查看）
        mPublisher.switchCameraFilter(MagicFilterType.BEAUTY);
        mPublisher.setOutputResolution(640, 480);
        mPublisher.setVideoHDMode();

        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isClick) {
                    mPublisher.startPublish(rtmpUrl);
                    isClick = false;
                } else {
                    getSRSStop();
                    isClick = true;
                }
            }
        });

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        mUSBMonitor.register();
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            if (!srsCameraView.isOpened()) {
                final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(ExternalVideoActivity.this, com.gstb.jwldn.R.xml.device_filter);
                List<UsbDevice> mList = mUSBMonitor.getDeviceList(filter.get(0));
                if (mList.size() > 0) {
                    mUSBMonitor.requestPermission(mList.get(0));
                }
            }
        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            Toast.makeText(ExternalVideoActivity.this, "已链接", Toast.LENGTH_SHORT).show();
            srsCameraView.initCtrlBlock(ctrlBlock);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mPublisher.startPublish(rtmpUrl);
                }
            }).start();
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            Toast.makeText(ExternalVideoActivity.this, "uvc断开链接", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onDettach(final UsbDevice device) {
        }

        @Override
        public void onCancel() {
        }
    };

        @Override
    protected void onResume() {
        super.onResume();
        mPublisher.resumeRecord();
    }
    private void getSRSStop() {
        mPublisher.stopPublish();
        mPublisher.stopRecord();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPublisher.stopPublish();
        mPublisher.stopRecord();
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }

    }


    private void initUIHandler() {
        mHanler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case UI_EVENT_RECONNECT_SERVER:
                        mPublisher.startPublish(rtmpUrl);
                        break;
                    case 11:
                        mPublisher.stopPublish();
                        mPublisher.stopRecord();
                        break;
                    case 12:
                        mPublisher.startPublish(rtmpUrl);
                        break;
                }
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPublisher.pauseRecord();
    }


    private void handleException(Exception e) {
        try {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            mPublisher.stopPublish();
            mPublisher.stopRecord();
            isException = true;
        } catch (Exception e1) {
            //
        }
    }


    // Implementation of SrsRtmpListener.

    @Override
    public void onRtmpConnecting(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        isException = false;
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
        Toast.makeText(getApplicationContext(), "推流停止", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpDisconnected() {
        Toast.makeText(getApplicationContext(), "srs断开链接", Toast.LENGTH_SHORT).show();
        if (isException) {
            breakpointHttp();
        }
    }

    @Override
    public void onRtmpVideoFpsChanged(double fps) {
        Log.i(TAG, String.format("Output Fps: %f", fps));
    }

    @Override
    public void onRtmpVideoBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Video bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(TAG, String.format("Video bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpAudioBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Audio bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(TAG, String.format("Audio bitrate: %d bps", rate));
        }
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

    // Implementation of SrsRecordHandler.

    @Override
    public void onRecordPause() {
//        Toast.makeText(getApplicationContext(), "Record paused", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordResume() {
//        Toast.makeText(getApplicationContext(), "Record resumed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordStarted(String msg) {
//        Toast.makeText(getApplicationContext(), "Recording file: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordFinished(String msg) {
//        Toast.makeText(getApplicationContext(), "MP4 file saved: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRecordIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    // Implementation of SrsEncodeHandler.

    @Override
    public void onNetworkWeak() {
        Toast.makeText(getApplicationContext(), "网络信号弱", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNetworkResume() {
        Toast.makeText(getApplicationContext(), "Network resume", Toast.LENGTH_SHORT).show();
    }

    @Override

    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }


    /**
     * 断点续传
     */

    private int currentCount = 0;

    private void breakpointHttp() {

        new Thread() {
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
                if (currentCount > 20) {
                    mHanler.sendEmptyMessage(11);
                } else {
                    mHanler.sendEmptyMessage(12);
                }

            }
        }.start();
    }
}
