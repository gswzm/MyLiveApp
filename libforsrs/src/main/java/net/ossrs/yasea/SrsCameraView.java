package net.ossrs.yasea;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.util.AttributeSet;

import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.seu.magicfilter.base.gpuimage.GPUImageFilter;
import com.seu.magicfilter.utils.MagicFilterFactory;
import com.seu.magicfilter.utils.MagicFilterType;
import com.seu.magicfilter.utils.OpenGLUtils;

/**
 * Created by Leo Ma on 2016/2/25.
 */
public class SrsCameraView extends GLSurfaceView implements GLSurfaceView.Renderer{

    private GPUImageFilter magicFilter;
    private SurfaceTexture surfaceTexture;
    private int mOESTextureId = OpenGLUtils.NO_TEXTURE;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private float mInputAspectRatio;
    private float mOutputAspectRatio;
    private float[] mProjectionMatrix = new float[16];
    private float[] mSurfaceMatrix = new float[16];
    private float[] mTransformMatrix = new float[16];

    private UVCCamera mCamera;
    private ByteBuffer mGLPreviewBuffer;
    private int mCamId = -1;
    private int mPreviewRotation = 90;
    private int mPreviewOrientation = Configuration.ORIENTATION_PORTRAIT;

    private boolean mIsEncoding;
    private Thread worker;
    private final Object writeLock = new Object();
    private ConcurrentLinkedQueue<IntBuffer> mGLIntBufferCache = new ConcurrentLinkedQueue<>();
    private PreviewCallback mPrevCb;

    private UsbControlBlock ctrlBlock;

    public SrsCameraView(Context context) {
        this(context, null);
    }

    public SrsCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setPreviewCallback(PreviewCallback cb) {
        mPrevCb = cb;
    }

    public int[] setPreviewResolution(int width, int height) {
        getHolder().setFixedSize(width, height);


        mPreviewWidth = width;
        mPreviewHeight = height;

        mGLPreviewBuffer = ByteBuffer.allocate(mPreviewWidth * mPreviewHeight * 4);
        mInputAspectRatio = mPreviewWidth > mPreviewHeight ?
                (float) mPreviewWidth / mPreviewHeight : (float) mPreviewHeight / mPreviewWidth;

        return new int[] { mPreviewWidth, mPreviewHeight };
    }



    private void deleteTextures() {
        if (mOESTextureId != OpenGLUtils.NO_TEXTURE) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    GLES20.glDeleteTextures(1, new int[]{ mOESTextureId }, 0);
                    mOESTextureId = OpenGLUtils.NO_TEXTURE;
                }
            });
        }
    }

    public void setCameraId(int id) {
        mCamId = id;
        setPreviewOrientation(mPreviewOrientation);
    }

    public boolean setFilter(final MagicFilterType type) {
        if (mCamera == null) {
            return false;
        }

        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (magicFilter != null) {
                    magicFilter.destroy();
                }
                magicFilter = MagicFilterFactory.initFilters(type);
                if (magicFilter != null) {
                    magicFilter.init(getContext().getApplicationContext());
                    magicFilter.onInputSizeChanged(mPreviewWidth, mPreviewHeight);
                    magicFilter.onDisplaySizeChanged(mSurfaceWidth, mSurfaceHeight);
                }
            }
        });
        requestRender();
        return true;
    }

    public void setPreviewOrientation(int orientation) {
        mPreviewOrientation = orientation;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCamId, info);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mPreviewRotation = 90;
        } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                mPreviewRotation = 90;
            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mPreviewRotation = 0;
            }
        }
    }

    public int getCameraId() {
        return mCamId;
    }

    public boolean startCamera() {
//        worker = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (!Thread.interrupted()) {
//                    while (!mGLIntBufferCache.isEmpty()) {
//                        IntBuffer picture = mGLIntBufferCache.poll();
//                        mGLPreviewBuffer.asIntBuffer().put(picture.array());
//                        mPrevCb.onGetRgbaFrame(mGLPreviewBuffer.array(), mPreviewWidth, mPreviewHeight);
//                    }
//                    // Waiting for next frame
//                    synchronized (writeLock) {
//                        try {
//                            // isEmpty() may take some time, so we set timeout to detect next frame
//                            writeLock.wait(500);
//                        } catch (InterruptedException ie) {
//                            worker.interrupt();
//                        }
//                    }
//                }
//            }
//        });
//        worker.start();
        if (mCamera == null) {
            mCamera = new UVCCamera();
            mCamera.open(ctrlBlock);
            if (mCamera == null) {
                return false;
            }
        }
        try {
            mCamera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
        } catch (final IllegalArgumentException e) {
            try {
                // fallback to YUV mode
                mCamera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
            } catch (final IllegalArgumentException e1) {
                mCamera.destroy();
                mCamera = null;
            }
        }

        mCamera.setPreviewTexture(surfaceTexture);
        mCamera.startPreview();

        return true;
    }

    public void stopCamera() {
//        if (worker != null) {
//            worker.interrupt();
//            try {
//                worker.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//                worker.interrupt();
//            }
//            mGLIntBufferCache.clear();
//            worker = null;
//        }

        disableEncoding();
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.destroy();
            mCamera = null;
        }
    }

    public void enableEncoding() {
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    while (!mGLIntBufferCache.isEmpty()) {
                        IntBuffer picture = mGLIntBufferCache.poll();
                        mGLPreviewBuffer.asIntBuffer().put(picture.array());
                        mPrevCb.onGetRgbaFrame(mGLPreviewBuffer.array(), mPreviewWidth, mPreviewHeight);
                    }
                    // Waiting for next frame
                    synchronized (writeLock) {
                        try {
                            // isEmpty() may take some time, so we set timeout to detect next frame
                            writeLock.wait(500);
                        } catch (InterruptedException ie) {
                            worker.interrupt();
                        }
                    }
                }
            }
        });
        worker.start();
        mIsEncoding = true;
    }

    public void disableEncoding() {
        mIsEncoding = false;
        mGLIntBufferCache.clear();

        if (worker != null) {
            worker.interrupt();
            try {
                worker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                worker.interrupt();
            }
            worker = null;
        }
    }
    private int[] adaptFpsRange(int expectedFps, List<int[]> fpsRanges) {
        expectedFps *= 1000;
        int[] closestRange = fpsRanges.get(0);
        int measure = Math.abs(closestRange[0] - expectedFps) + Math.abs(closestRange[1] - expectedFps);
        for (int[] range : fpsRanges) {
            if (range[0] <= expectedFps && range[1] >= expectedFps) {
                int curMeasure = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
                if (curMeasure < measure) {
                    closestRange = range;
                    measure = curMeasure;
                }
            }
        }
        return closestRange;
    }

    public interface PreviewCallback {

        void onGetRgbaFrame(byte[] data, int width, int height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glDisable(GL10.GL_DITHER);
        GLES20.glClearColor(0, 0, 0, 0);

        magicFilter = new GPUImageFilter(MagicFilterType.NONE);
        magicFilter.init(getContext().getApplicationContext());
        magicFilter.onInputSizeChanged(mPreviewWidth, mPreviewHeight);

        mOESTextureId = OpenGLUtils.getExternalOESTextureID();
        surfaceTexture = new SurfaceTexture(mOESTextureId);
        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                requestRender();
            }
        });

        // For camera preview on activity creation
        if (mCamera != null) {
            mCamera.setPreviewTexture(surfaceTexture);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        magicFilter.onDisplaySizeChanged(width, height);

        mOutputAspectRatio = width > height ? (float) width / height : (float) height / width;
        float aspectRatio = mOutputAspectRatio / mInputAspectRatio;
        if (width > height) {
            Matrix.orthoM(mProjectionMatrix, 0, -1.0f, 1.0f, -aspectRatio, aspectRatio, -1.0f, 1.0f);
        } else {
            Matrix.orthoM(mProjectionMatrix, 0, -aspectRatio, aspectRatio, -1.0f, 1.0f, -1.0f, 1.0f);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        surfaceTexture.updateTexImage();

        surfaceTexture.getTransformMatrix(mSurfaceMatrix);
        Matrix.multiplyMM(mTransformMatrix, 0, mSurfaceMatrix, 0, mProjectionMatrix, 0);
        magicFilter.setTextureTransformMatrix(mTransformMatrix);

        magicFilter.onDrawFrame(mOESTextureId);
//        mGLIntBufferCache.add(magicFilter.getGLFboBuffer());
//        synchronized (writeLock) {
//            writeLock.notifyAll();
//        }
        if (mIsEncoding) {
            mGLIntBufferCache.add(magicFilter.getGLFboBuffer());
            synchronized (writeLock) {
                writeLock.notifyAll();
            }
        }
    }

    public void initCtrlBlock(UsbControlBlock ctrlBlock) {
        this.ctrlBlock  = ctrlBlock;
    }

    public boolean isOpened() {
        return mCamera != null;
    }
}
