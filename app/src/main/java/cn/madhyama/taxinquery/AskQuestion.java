package cn.madhyama.taxinquery;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import java.util.HashMap;
import java.util.LinkedHashMap;

import cn.madhyama.util.IatSettings;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class AskQuestion extends AppCompatActivity {
    private static String TAG = AskQuestion.class.getSimpleName();
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
    private Button mAsk;
    private TextView title;
    private Toast mToast;
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 语音听写UI
    private RecognizerDialog mIatDialog;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private String result = "";
    private SharedPreferences mSharedPreferences;
    private String username;
    /**
     * 初始化语音听写监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code);
            }
        }
    };
    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, "recognizer result：" + results.getResultString());
            result = cn.madhyama.util.JsonParser.parseIatResult(results.getResultString());

            go2questionList(result);
        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
        }

    };
    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            showTip(error.getPlainDescription(true));
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showTip("结束说话");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, "recognizer result：" + results.getResultString());
            result = cn.madhyama.util.JsonParser.parseIatResult(results.getResultString());

            go2questionList(result);
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据：" + data.length);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_question);
        mContentView = findViewById(R.id.fullscreen_content);

        Intent intent = getIntent();
        username = intent.getStringExtra("name");

        if (username != null) {
            title = (TextView) findViewById(R.id.titleWithName);
            title.setText(username + "您好，");
        }

        // 初始化识别无UI识别对象
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener);

        // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        mIatDialog = new RecognizerDialog(this, mInitListener);

        mSharedPreferences = getSharedPreferences(IatSettings.PREFER_NAME, Activity.MODE_PRIVATE);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        setParam();
        Button mKeyIn = (Button) findViewById(R.id.keyin);
        mKeyIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 200 milliseconds
                vibrator.vibrate(200);
                go2questionListNullString();
            }
        });
        mAsk = (Button) findViewById(R.id.ask);
//        mAsk.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (null == mIat) {
//                    // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
//                    showTip("创建对象失败，请确认 libmsc.so 放置正确，\n 且有调用 createUtility 进行初始化");
//                    return;
//                }
//                mIatResults.clear();
//
//
//                boolean isShowDialog = mSharedPreferences.getBoolean(getString(R.string.pref_key_iat_show), true);
//
//                if (isShowDialog) {
//                    // 显示听写对话框
//                    mIatDialog.setListener(mRecognizerDialogListener);
//                    mIatDialog.show();
//                    showTip(getString(R.string.text_begin));
//                } else {
//                    int ret = 0;
//                    // 不显示听写对话框
//                    ret = mIat.startListening(mRecognizerListener);
//                    if (ret != ErrorCode.SUCCESS) {
//                        showTip("听写失败,错误码：" + ret);
//                    } else {
//                        showTip(getString(R.string.text_begin));
//                    }
//                }
//            }
//        });
//        mAsk.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                return false;
//            }
//        });
        mAsk.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mAsk.setBackgroundResource(R.drawable.button_voice_pressed);
                        if (null == mIat) {
                            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
                            showTip("创建对象失败，请确认 libmsc.so 放置正确，\n 且有调用 createUtility 进行初始化");
                            return false;
                        }
                        mIatResults.clear();

                        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "100000"));
                        boolean isShowDialog = mSharedPreferences.getBoolean(getString(R.string.pref_key_iat_show), true);

                        if (isShowDialog) {
                            // 显示听写对话框
                            mIatDialog.setListener(mRecognizerDialogListener);
                            mIatDialog.show();
                            showTip(getString(R.string.text_begin));
                        } else {
                            int ret = 0;
                            // 不显示听写对话框
                            ret = mIat.startListening(mRecognizerListener);
                            if (ret != ErrorCode.SUCCESS) {
                                showTip("听写失败,错误码：" + ret);
                            } else {
                                showTip(getString(R.string.text_begin));
                            }
                        }
                        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                        // Vibrate for 200 milliseconds
                        vibrator.vibrate(200);


                        //mAsk.setBackgroundResource(R.drawable.button_zcyj_press);
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        mAsk.setBackgroundResource(R.drawable.button_voice);
                        mIat.stopListening();
                        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));
                        return true; // if you want to handle the touch event
                }

                return false;
            }
        });

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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != mIat) {
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }

    }

    private void showTip(final String str) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }

    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        String lag = mSharedPreferences.getString("iat_language_preference", "mandarin");
        // 设置引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        if (lag.equals("en_us")) {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
        } else {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);
        }

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "0"));

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/iat.wav");
    }

    public void go2questionList(String speechText) {
        Intent intent = new Intent(this, MatchSpeech.class);
        intent.putExtra("speechText", speechText);
        if (speechText.length() > 2) startActivity(intent);
    }

    public void go2questionListNullString() {
        Intent intent = new Intent(this, MatchSpeech.class);
        startActivity(intent);
    }
}
