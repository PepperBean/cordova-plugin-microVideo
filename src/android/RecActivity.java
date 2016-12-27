package com.pepperbean.plugin;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.app.*;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.List;

public class RecActivity extends android.app.Activity implements SurfaceHolder.Callback, View.OnTouchListener, BothWayProgressBar.OnProgressEndListener {

    private static final int LISTENER_START = 200;
    private static final String TAG = "RecActivity";
    //预览SurfaceView
    private SurfaceView mSurfaceView;
    private Camera mCamera;
    //底部"按住拍"按钮
    private View mStartButton;
    private View mCancelButton;
    //进度条
    private BothWayProgressBar mProgressBar;
    //进度条线程
    private Thread mProgressThread;
    //录制视频
    private MediaRecorder mMediaRecorder;
    private SurfaceHolder mSurfaceHolder;
    //屏幕分辨率
    private int videoWidth, videoHeight;
    //判断是否正在录制
    private boolean isRecording;
    //段视频保存的目录
    private File mTargetFile;
    //当前进度/时间
    private float mProgress;
    //录制最大时间
    public static final int MAX_TIME = 6;  // in secents
    //是否上滑取消
    private boolean isCancel;
//    //手势处理, 主要用于变焦 (双击放大缩小)
//    private GestureDetector mDetector;
//    //是否放大
//    private boolean isZoomIn = false;

    private MyHandler mHandler;
    private TextView mTvTip;
    private boolean isRunning;
    private android.content.res.Resources resources;
    private String package_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        package_name = getApplication().getPackageName();
        resources = getApplication().getResources();
        setContentView(resources.getIdentifier("activity_main", "layout", package_name));
        //   setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        videoWidth = 640;
        videoHeight = 480;
        mSurfaceView = (SurfaceView) findViewById(resources.getIdentifier("main_surface_view", "id", package_name));

//        mDetector = new GestureDetector(this, new ZoomGestureListener());
        /**
         * 单独处理mSurfaceView的双击事件
         */
//        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                mDetector.onTouchEvent(event);
//                return true;
//            }
//        });

        mSurfaceHolder = mSurfaceView.getHolder();
        //设置屏幕分辨率
        mSurfaceHolder.setFixedSize(videoWidth, videoHeight);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);
        mStartButton = findViewById(resources.getIdentifier("main_press_control", "id", package_name));
//        mStartButton = findViewById("main_press_control");
        mTvTip = (TextView) findViewById(resources.getIdentifier("main_tv_tip", "id", package_name));
//        mTvTip = (TextView) findViewById("main_tv_tip");
        mCancelButton = findViewById(resources.getIdentifier("main_cancel_back", "id", package_name));
        mStartButton.setOnTouchListener(this);
        mCancelButton.setOnTouchListener(this);
        mProgressBar = (BothWayProgressBar) findViewById(resources.getIdentifier("main_progress_bar", "id", package_name));
//        mProgressBar = (BothWayProgressBar) findViewById("main_progress_bar");
        mProgressBar.setOnProgressEndListener(this);
        mHandler = new MyHandler(this);
        mMediaRecorder = new MediaRecorder();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
    }

    ///////////////////////////////////////////////////////////////////////////
    // SurfaceView回调
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        startPreView(holder);
    }

    /**
     * 开启预览
     *
     * @param holder
     */
    private void startPreView(SurfaceHolder holder) {
        Log.d(TAG, "startPreView: ");

        if (mCamera == null) {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
//        if (mMediaRecorder == null) {
//            mMediaRecorder = new MediaRecorder();
//            // mMediaRecorder.setOnErrorListener(null);
//
//        }
////        else {
////            mMediaRecorder.reset();
////        }
        if (mCamera != null) {
            mCamera.setDisplayOrientation(90);
            try {
                mCamera.setPreviewDisplay(holder);
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setPreviewSize(videoWidth, videoHeight);
                mCamera.setParameters(parameters);
                //实现Camera自动对焦
                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes != null) {
                    for (String mode : focusModes) {
                        mode.contains("continuous-video");
                        parameters.setFocusMode("continuous-video");
                    }
                }
                mCamera.setParameters(parameters);

                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            Log.d(TAG, "surfaceDestroyed: ");
            //停止预览并释放摄像头资源
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // 进度条结束后的回调方法
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onProgressEndListener() {
        if (isCancel) {
            stopRecordUnSave();
        } else {
            //视频停止录制
            stopRecordSave();
        }

    }

    /**
     * 开始录制
     */
    private void startRecord() {
//        if (mMediaRecorder == null) {
        mMediaRecorder = new MediaRecorder();
//        } else {
//            mMediaRecorder.reset();
//        }

        //没有外置存储, 直接停止录制
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return;
        }
        try {
            //mMediaRecorder.reset();
            //  mCamera.lock();
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);
            //从相机采集视频
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            // 从麦克采集音频信息
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            // TODO: 2016/10/20  设置视频格式
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            // 在l36h上，由于硬件问题以下三行会报错
            mMediaRecorder.setVideoSize(videoWidth, videoHeight);
            //每秒的帧数
            mMediaRecorder.setVideoFrameRate(30);
            // 设置帧频率
            mMediaRecorder.setVideoEncodingBitRate(1500000);
//                mMediaRecorder.setVideoEncodingBitRate(1 * 1024 * 1024 * 100);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            // TODO: 2016/10/20 临时写个文件地址, 稍候该!!!
//            File targetDir = Environment.
//                    getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
            File targetDir = this.getApplicationContext().getFilesDir();
            mTargetFile = new File(targetDir,
                    SystemClock.currentThreadTimeMillis() + ".mp4");
            mMediaRecorder.setOutputFile(mTargetFile.getAbsolutePath());
            mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
            //解决录制视频, 播放器横向问题
            mMediaRecorder.setOrientationHint(90);
            // mMediaRecorder.setOnErrorListener();
            mMediaRecorder.prepare();
            android.util.Log.i("RecActivity","before recorder start!!");
            //正式录制
            mMediaRecorder.start();
            isRecording = true;
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * 停止录制 并且保存
     */
    private void stopRecordSave() {

        isRunning = false;
        if (mCamera != null) {
            mCamera.lock();

        }
        if (mMediaRecorder == null) {
            return;
        }
        mMediaRecorder.setOnErrorListener(null);
        try {
            mMediaRecorder.stop();
            isRecording = false;
            // Toast.makeText(this, "视频已经放至" + mTargetFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent();
            intent.putExtra("back", mTargetFile.getAbsolutePath());
            setResult(RESULT_OK, intent);
            finish();
        } catch (RuntimeException e) {
            // Toast.makeText(this, "视频保存失败！", Toast.LENGTH_SHORT).show();
            Toast toast = Toast.makeText(this, "视频保存失败！", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            if (mTargetFile.exists()) {
                //不保存直接删掉
                mTargetFile.delete();
            }
        } finally {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

    }

    /**
     * 停止录制, 不保存，不返回
     */
    private void stopRecordUnSave() {
        isRunning = false;
        if (mCamera != null) {
            mCamera.lock();

        }
        if (mMediaRecorder == null) {
            return;
        }
        mMediaRecorder.setOnErrorListener(null);
        try {
            mMediaRecorder.stop();
        } catch (RuntimeException e) {
//                if (mTargetFile.exists()) {
//                    //不保存直接删掉
//                    mTargetFile.delete();
//                }
        } finally {
            mMediaRecorder.release();
            mMediaRecorder = null;
            if (mTargetFile.exists()) {
                //不保存直接删掉
                mTargetFile.delete();
            }
        }
    }
//
//    /**
//     * 相机变焦
//     *
//     * @param zoomValue
//     */
//    public void setZoom(int zoomValue) {
//        if (mCamera != null) {
//            Camera.Parameters parameters = mCamera.getParameters();
//            if (parameters.isZoomSupported()) {//判断是否支持
//                int maxZoom = parameters.getMaxZoom();
//                if (maxZoom == 0) {
//                    return;
//                }
//                if (zoomValue > maxZoom) {
//                    zoomValue = maxZoom;
//                }
//                parameters.setZoom(zoomValue);
//                mCamera.setParameters(parameters);
//            }
//        }
//
//    }


    ///////////////////////////////////////////////////////////////////////////
    // Handler处理
    ///////////////////////////////////////////////////////////////////////////
    private static class MyHandler extends Handler {
        private WeakReference<RecActivity> mReference;
        private RecActivity mActivity;

        public MyHandler(RecActivity activity) {
            mReference = new WeakReference<RecActivity>(activity);
            mActivity = mReference.get();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    mActivity.mProgressBar.setProgress(mActivity.mProgress);
                    break;
            }

        }
    }

    /**
     * 触摸事件的触发
     *
     * @param v
     * @param event
     * @return
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean ret = false;
        int action = event.getAction();
        float ey = event.getY();
        float ex = event.getX();
        //只监听中间的按钮处
        int vW = v.getWidth();
        int left = LISTENER_START;
        int right = vW - LISTENER_START;

        float downY = 0;

        int main_press_control_ID = resources.getIdentifier("main_press_control", "id", package_name);
        int main_cancel_back_ID = resources.getIdentifier("main_cancel_back", "id", package_name);
        if (v.getId() == main_cancel_back_ID) {
            Intent intent = new Intent();
            intent.putExtra("back", "CANCELED");
            setResult(RESULT_CANCELED, intent);
            finish();
        } else if (v.getId() == main_press_control_ID) {
//            case "main_press_control": {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    android.util.Log.i("RecActivity","ACTION_DOWN");
                    if (ex > left && ex < right) {
                        mProgressBar.setCancel(false);
                        //显示上滑取消
                        mTvTip.setVisibility(View.VISIBLE);
                        mTvTip.setText("↑ 上滑取消");
                        //记录按下的Y坐标
                        downY = ey;
                        // TODO: 2016/10/20 开始录制视频, 进度条开始走
                        mProgressBar.setVisibility(View.VISIBLE);
                        //开始录制
                        startRecord();
                        //    Toast.makeText(this, "开始录制", Toast.LENGTH_SHORT).show();
                        mProgressThread = new Thread() {
                            @Override
                            public void run() {
                                super.run();
                                try {
                                    int progCount = 0;
                                    int progCountMax = MAX_TIME * 50;
                                    isRunning = true;
                                    while (isRunning && !isCancel) {

                                        progCount++;
                                        mProgress = (float) progCount / (float) progCountMax;
                                        mHandler.obtainMessage(0).sendToTarget();
                                        Thread.sleep(20);
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        };

                        mProgressThread.start();
                        ret = true;
                    }


                    break;
                case MotionEvent.ACTION_UP:
                    android.util.Log.i("RecActivity","ACTION_UP");
                    if (ex > left && ex < right) {
                        mTvTip.setVisibility(View.INVISIBLE);
                        mProgressBar.setVisibility(View.INVISIBLE);
                        //判断是否为录制结束, 或者为成功录制(时间过短)
                        if (!isCancel) {
                            // 少于1秒
                            if (mProgress < (0.8 / MAX_TIME)) {
                                //时间太短不保存
                                stopRecordUnSave();
                                //  Toast.makeText(this, "时间太短", Toast.LENGTH_SHORT).show();
                                Toast toast = Toast.makeText(this, "时间太短", Toast.LENGTH_SHORT);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                                mProgressBar.setCancel(false);
                                break;
                            } else {
                                //停止录制
                                stopRecordSave();
                            }

                        } else {
                            //现在是取消状态,不保存
                            stopRecordUnSave();
                            isCancel = false;
                            Toast toast = Toast.makeText(this, "取消录制", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            mProgressBar.setCancel(false);
                        }
                        mProgress = 0;
                        ret = false;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    android.util.Log.i("RecActivity","ACTION_MOVE");
                    if (ex > left && ex < right) {
                        float currentY = event.getY();
                        if (downY - currentY > 10) {
                            isCancel = true;
                            mProgressBar.setCancel(true);
                        }
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    if(!isRunning)
                    {
                        break;
                    }
                    android.util.Log.i("RecActivity","ACTION_CANCEL");
                    stopRecordUnSave();
                    isCancel = false;
                    Toast toast = Toast.makeText(this, "取消录制", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    mProgressBar.setCancel(true);
                    mTvTip.setVisibility(View.INVISIBLE);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mProgress = 0;
                    break;
                case MotionEvent.ACTION_OUTSIDE:
                    android.util.Log.i("RecActivity","ACTION_OUTSIDE");
                    break;
//                }
//                break;

            }

        }
        return ret;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Intent intent = new Intent();
            intent.putExtra("back", "CANCELED");
            setResult(RESULT_CANCELED, intent);
            finish();
            return true;
        }else {
            return super.onKeyDown(keyCode, event);
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // 变焦手势处理类
    ///////////////////////////////////////////////////////////////////////////
//    class ZoomGestureListener extends GestureDetector.SimpleOnGestureListener {
//        //双击手势事件
//        @Override
//        public boolean onDoubleTap(MotionEvent e) {
//            super.onDoubleTap(e);
//            Log.d(TAG, "onDoubleTap: 双击事件");
//            if (mMediaRecorder != null) {
//                if (!isZoomIn) {
//                    setZoom(20);
//                    isZoomIn = true;
//                } else {
//                    setZoom(0);
//                    isZoomIn = false;
//                }
//            }
//            return true;
//        }
//    }


}
