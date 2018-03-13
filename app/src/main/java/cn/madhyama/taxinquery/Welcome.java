package cn.madhyama.taxinquery;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.FaceDetector;
import com.iflytek.cloud.FaceRequest;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RequestListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.Accelerometer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import cn.madhyama.util.FaceRect;
import cn.madhyama.util.FaceUtil;
import cn.madhyama.util.ParseResult;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class Welcome extends AppCompatActivity {

    private static Map names = new HashMap(10);
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHideActionBarAndNavBarRunnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
            mHideHandler.removeCallbacks(mHideRunnable);
            mHideHandler.post(mHideActionBarAndNavBarRunnable);
        }
    };
    private SurfaceView mPreviewSurface;
    private SurfaceView mFaceSurface;
    private Camera mCamera;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    // Camera nv21格式预览帧的尺寸，默认设置640*480
    private int PREVIEW_WIDTH = 640;
    private int PREVIEW_HEIGHT = 480;
    // 预览帧数据存储数组和缓存数组
    private byte[] nv21;
    private byte[] buffer;
    // 缩放矩阵
    private Matrix mScaleMatrix = new Matrix();
    // 加速度感应器，用于获取手机的朝向
    private Accelerometer mAcc;
    // FaceDetector对象，集成了离线人脸识别：人脸检测、视频流检测功能
    private FaceDetector mFaceDetector;
    private boolean mStopTrack;
    private Toast mToast;
    private long mLastClickTime;
    private int isAlign = 0;
    private SpeechSynthesizer mTts;
    private SurfaceHolder.Callback mPreviewCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            closeCamera();
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            openCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            mScaleMatrix.setScale(width / (float) PREVIEW_HEIGHT, height / (float) PREVIEW_WIDTH);
        }
    };
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d("Welcome", "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码：" + code);
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            showTip("开始播放");
        }

        @Override
        public void onSpeakPaused() {
            showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            showTip("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
//            // 合成进度
//            mPercentForBuffering = percent;
//            showTip(String.format(getString(R.string.tts_toast_format),
//                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
//            // 播放进度
//            mPercentForPlaying = percent;
//            showTip(String.format(getString(R.string.tts_toast_format),
//                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                showTip("播放完成");
            } else if (error != null) {
                showTip(error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };
    // FaceRequest对象，集成了人脸识别的各种功能
    private FaceRequest mFaceRequest;
    private boolean isVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        mContentView = findViewById(R.id.fullscreen_content);

        initUI();
        mFaceRequest = new FaceRequest(this);
        nv21 = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT * 2];
        buffer = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT * 2];
        mAcc = new Accelerometer(Welcome.this);
        mFaceDetector = FaceDetector.createDetector(Welcome.this, null);
        mTts = SpeechSynthesizer.createSynthesizer(Welcome.this, mTtsInitListener);
        setNames();
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                go2ask(view, null);
            }
        });

    }

    private void initUI() {
        mPreviewSurface = (SurfaceView) findViewById(R.id.sfv_preview);
        mFaceSurface = (SurfaceView) findViewById(R.id.sfv_face);

        mPreviewSurface.getHolder().addCallback(mPreviewCallback);
        mPreviewSurface.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mFaceSurface.setZOrderOnTop(true);
        mFaceSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        // 点击SurfaceView，切换摄相头
        mFaceSurface.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 只有一个摄相头，不支持切换
                if (Camera.getNumberOfCameras() == 1) {
                    showTip("只有后置摄像头，不能切换");
                    return;
                }
                closeCamera();
                if (Camera.CameraInfo.CAMERA_FACING_FRONT == mCameraId) {
                    mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                } else {
                    mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                }
                openCamera();
            }
        });

        // 长按SurfaceView 500ms后松开，摄相头聚集
        mFaceSurface.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mLastClickTime = System.currentTimeMillis();
                        break;
                    case MotionEvent.ACTION_UP:
                        if (System.currentTimeMillis() - mLastClickTime > 500) {
                            mCamera.autoFocus(null);
                            return true;
                        }
                        break;

                    default:
                        break;
                }
                return false;
            }
        });


        setSurfaceSize();
        mToast = Toast.makeText(Welcome.this, "", Toast.LENGTH_SHORT);
    }
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {

    private void setSurfaceSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int width = metrics.widthPixels;
        int height = (int) (width * PREVIEW_WIDTH / (float) PREVIEW_HEIGHT);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        //mPreviewSurface.setLayoutParams(new RelativeLayout.LayoutParams(1, 1));
        mPreviewSurface.setLayoutParams(params);
        mFaceSurface.setLayoutParams(params);
    }

    private boolean checkCameraPermission() {
        int status = checkPermission(Manifest.permission.CAMERA, Process.myPid(), Process.myUid());
        return PackageManager.PERMISSION_GRANTED == status;

    }

    private void openCamera() {
        if (null != mCamera) {
            return;
        }

        if (!checkCameraPermission()) {
            showTip("摄像头权限未打开，请打开后再试");
            mStopTrack = true;
            return;
        }

        // 只有一个摄相头，打开后置
        if (Camera.getNumberOfCameras() == 1) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        try {
            mCamera = Camera.open(mCameraId);
            if (Camera.CameraInfo.CAMERA_FACING_FRONT == mCameraId) {
                showTip("前置摄像头已开启，点击可切换");
            } else {
                showTip("后置摄像头已开启，点击可切换");
            }
        } catch (Exception e) {
            e.printStackTrace();
            closeCamera();
            return;
        }

        Camera.Parameters params = mCamera.getParameters();
        params.setPreviewFormat(ImageFormat.NV21);
        params.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        mCamera.setParameters(params);

        // 设置显示的偏转角度，大部分机器是顺时针90度，某些机器需要按情况设置
        mCamera.setDisplayOrientation(90);
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                System.arraycopy(data, 0, nv21, 0, data.length);
            }
        });

        try {
            mCamera.setPreviewDisplay(mPreviewSurface.getHolder());
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.post(mHideActionBarAndNavBarRunnable);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.post(mHideActionBarAndNavBarRunnable);
    }

    public void go2ask(View view, String name) {
        Intent intent = new Intent(this, AskQuestion.class);
        intent.putExtra("name", name);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (null != mAcc) {
            mAcc.start();
        }

        mStopTrack = false;
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (!mStopTrack) {
                    if (null == nv21) {
                        continue;
                    }

                    synchronized (nv21) {
                        System.arraycopy(nv21, 0, buffer, 0, nv21.length);
                    }

                    // 获取手机朝向，返回值0,1,2,3分别表示0,90,180和270度
                    int direction = Accelerometer.getDirection();
                    boolean frontCamera = (Camera.CameraInfo.CAMERA_FACING_FRONT == mCameraId);
                    // 前置摄像头预览显示的是镜像，需要将手机朝向换算成摄相头视角下的朝向。
                    // 转换公式：a' = (360 - a)%360，a为人眼视角下的朝向（单位：角度）
                    if (frontCamera) {
                        // SDK中使用0,1,2,3,4分别表示0,90,180,270和360度
                        direction = (4 - direction) % 4;
                    }

                    if (mFaceDetector == null) {
                        /**
                         * 离线视频流检测功能需要单独下载支持离线人脸的SDK
                         * 请开发者前往语音云官网下载对应SDK
                         */
                        showTip("本SDK不支持离线视频流检测");
                        break;
                    }

                    String result = mFaceDetector.trackNV21(buffer, PREVIEW_WIDTH, PREVIEW_HEIGHT, isAlign, direction);
                    Log.d("Welcome", "result:" + result);

                    FaceRect[] faces = ParseResult.parseResult(result);

                    Canvas canvas = mFaceSurface.getHolder().lockCanvas();
                    if (null == canvas) {
                        continue;
                    }

                    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    canvas.setMatrix(mScaleMatrix);

                    if (faces.length <= 0) {
                        mFaceSurface.getHolder().unlockCanvasAndPost(canvas);
                        continue;
                    }

                    if (null != faces && frontCamera == (Camera.CameraInfo.CAMERA_FACING_FRONT == mCameraId)) {
                        for (FaceRect face : faces) {
                            face.bound = FaceUtil.RotateDeg90(face.bound, PREVIEW_WIDTH, PREVIEW_HEIGHT);
                            if (face.point != null) {
                                for (int i = 0; i < face.point.length; i++) {
                                    face.point[i] = FaceUtil.RotateDeg90(face.point[i], PREVIEW_WIDTH, PREVIEW_HEIGHT);
                                }
                            }
                            FaceUtil.drawFaceRect(canvas, face, PREVIEW_WIDTH, PREVIEW_HEIGHT,
                                    frontCamera, false);
                        }

                        if (!isVerified && faces.length == 1) {
                            isVerified = true;
                            // 拷贝到临时数据中
                            byte[] tmp = new byte[nv21.length];
                            System.arraycopy(nv21, 0, tmp, 0, nv21.length);
                            byte[] image = Bitmap2Bytes(RotateDeg90(decodeToBitMap(tmp)));
                            try {
                                for (Object userid : names.keySet()) {


                                    verify(image, (String) userid);
                                    Thread.sleep(1000);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }


                    } else {
                        Log.d("Welcome", "faces:0");
                    }

                    mFaceSurface.getHolder().unlockCanvasAndPost(canvas);
                }
            }
        }).start();
    }

    private void verify(byte[] mImageData, final String userid) {


        if (null != mImageData) {
            // 设置用户标识，格式为6-18个字符（由字母、数字、下划线组成，不得以数字开头，不能包含空格）。
            // 当不设置时，云端将使用用户设备的设备ID来标识终端用户。

            mFaceRequest.setParameter(SpeechConstant.AUTH_ID, userid);
            mFaceRequest.setParameter(SpeechConstant.WFR_SST, "verify");
            mFaceRequest.sendRequest(mImageData, new RequestListener() {

                @Override
                public void onEvent(int arg0, Bundle arg1) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onCompleted(SpeechError arg0) {
                    if (arg0 != null)
                        Log.e("Welcome", "error:" + arg0.getErrorCode());
                }

                @Override
                public void onBufferReceived(byte[] arg0) {
                    try {
                        String result = new String(arg0, "utf-8");
                        Log.d("FaceDemo", "test");
                        Log.d("FaceDemo", result);

                        JSONObject object = new JSONObject(result);
                        int ret = object.getInt("ret");
                        if (ret != 0) {
                            showTip("验证失败");
                            return;
                        }
                        if ("success".equals(object.get("rst"))) {
                            if (object.getBoolean("verf")) {
                                String username = (String) names.get(userid);
                                mTts.startSpeaking("感谢使用税务云知音，" + username + "，欢迎您回来", mTtsListener);
                                closeCamera();
                                go2ask(null, username);
                            } else {
                                // showTip(userid+"验证不通过");
                            }
                        } else {
                            showTip("验证失败");
                        }
                    } catch (UnsupportedEncodingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (JSONException e) {
                        // TODO: handle exception
                    }
                }
            });

        } else {
            showTip("请选择图片后再验证");
        }
    }

    private Bitmap decodeToBitMap(byte[] data) {
        try {
            YuvImage image = new YuvImage(data, ImageFormat.NV21, PREVIEW_WIDTH,
                    PREVIEW_HEIGHT, null);
            if (image != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT),
                        80, stream);
                Bitmap bmp = BitmapFactory.decodeByteArray(
                        stream.toByteArray(), 0, stream.size());
                stream.close();
                return bmp;
            }
        } catch (Exception ex) {
            Log.e("Sys", "Error:" + ex.getMessage());
        }
        return null;
    }

    private Bitmap RotateDeg90(Bitmap bmp) {
        // 定义矩阵对象
        Matrix matrix = new Matrix();
        // 缩放原图
        matrix.postScale(1f, 1f);
        // 向左旋转45度，参数为正则向右旋转
        matrix.postRotate(-90);
        //bmp.getWidth(), 500分别表示重绘后的位图宽高
        Bitmap dstbmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(),
                matrix, true);
        return dstbmp;
    }


    private byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        if (null != mAcc) {
            mAcc.stop();
        }
        mStopTrack = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 销毁对象
        mFaceDetector.destroy();
    }

    private void setNames() {
        names.put("gu", "谷先生");

        names.put("lei", "雷女士");
        names.put("wang", "王先生");
        names.put("sun", "孙先生");
        names.put("zhao", "赵先生");
        //      names.put("xu","徐先生");
        //    names.put("liang1","梁先生");
        //   names.put("liang2","梁先生");
        //     names.put("wu","吴先生");
        //   names.put("cui","崔先生");
        //     names.put("li","李先生");


    }

}
