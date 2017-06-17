package cn.madhyama.taxinquery;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import cn.madhyama.util.DatabaseHelper;
import cn.madhyama.util.TtsSettings;
import com.iflytek.cloud.*;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class PostAnswer extends AppCompatActivity {
    private static String TAG = PostAnswer.class.getSimpleName();
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
    private String bsznbt = "";
    private String bsznywgs = "";
    private String bsznzcyj = "";
    private SharedPreferences mSharedPreferences;
    private Toast mToast;
    private SpeechSynthesizer mTts;
    private TextView answer;
    //缓冲进度
    private int mPercentForBuffering = 0;
    //播放进度
    private int mPercentForPlaying = 0;

    private static boolean mute=false;
    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码：" + code);
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在oncde3@WSX
                // Create方法中创建完合成对象之后马上就调用startSpeaking进行合成，
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
            // 合成进度
            mPercentForBuffering = percent;
            showTip(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            mPercentForPlaying = percent;
            showTip(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_answer);
        mContentView = findViewById(R.id.fullscreen_content);
        TextView question = (TextView) findViewById(R.id.question);
        answer = (TextView) findViewById(R.id.answer);


        Intent intent = getIntent();
        String bsznid = intent.getStringExtra("bsznid");
        if (bsznid != null) {

            DatabaseHelper myDbHelper = new DatabaseHelper(this);
            //myDbHelper.createDataBase();
            myDbHelper.openDataBase();

            SQLiteDatabase mydatabase = myDbHelper.getReadableDatabase("");

            Cursor contentSet = mydatabase.rawQuery("select bsznbt, bsznywgs, bsznzcyj from bszn12366 where bsznid = '" + bsznid + "'", null);
            if (contentSet.moveToNext()) {
                bsznbt = contentSet.getString(0);
                bsznywgs = contentSet.getString(1).replace((char) 12288, ' ');
                bsznzcyj = contentSet.getString(2).replace((char) 12288, ' ');
            }


            question.setText(bsznbt.trim());
            answer.setText(bsznywgs.trim());

            final Button mGSJD = (Button) findViewById(R.id.gsjd);

            mGSJD.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mGSJD.setBackgroundResource(R.drawable.button_gsjd_press);
                            answer.setText(bsznywgs.trim());
                            return true; // if you want to handle the touch event
                        case MotionEvent.ACTION_UP:
                            mGSJD.setBackgroundResource(R.drawable.button_gsjd);
                            return true; // if you want to handle the touch event
                    }

                    return false;
                }
            });


            final Button mZCYJ = (Button) findViewById(R.id.zcyj);
            mZCYJ.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mZCYJ.setBackgroundResource(R.drawable.button_zcyj_press);
                            answer.setText(bsznzcyj.trim());
                            return true; // if you want to handle the touch event
                        case MotionEvent.ACTION_UP:
                            mZCYJ.setBackgroundResource(R.drawable.button_zcyj);
                            return true; // if you want to handle the touch event
                    }

                    return false;
                }
            });




        } else {
            question.setText(intent.getStringExtra("question"));
            answer.setText(intent.getStringExtra("answer"));
//            ImageButton mGSJD = (ImageButton)findViewById(R.id.gsjd);
//            mGSJD.setd
//            ImageButton mZCYJ = (ImageButton)findViewById(R.id.zcyj);
        }

        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);
        setTTSParam();

        final Button buttonMute = (Button)findViewById(R.id.mute);
        if(mute){
            buttonMute.setBackgroundResource(R.drawable.volumeoff);
        }
        else {
            mTts.startSpeaking(answer.getText().toString(), mTtsListener);
            buttonMute.setBackgroundResource(R.drawable.volumeon);
        }
        buttonMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 200 milliseconds
                vibrator.vibrate(200);
                mute = !mute;
                if(mute){

                    buttonMute.setBackgroundResource(R.drawable.volumeoff);
                    if(mTts.isSpeaking())mTts.stopSpeaking();
                }
                else {
                    buttonMute.setBackgroundResource(R.drawable.volumeon);
                    mTts.startSpeaking(answer.getText().toString(), mTtsListener);
                }

            }
        });

        mSharedPreferences = getSharedPreferences(TtsSettings.PREFER_NAME, Activity.MODE_PRIVATE);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);


        ImageButton mQuit = (ImageButton) findViewById(R.id.mainpage);
        mQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTts.stopSpeaking();
                go2page(Welcome.class);
            }
        });
        TextView mTextQuit = (TextView) findViewById(R.id.textMainpage);
        mTextQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTts.stopSpeaking();
                go2page(Welcome.class);
            }
        });

        ImageButton mBackToList = (ImageButton) findViewById(R.id.backToList);
        View.OnClickListener lsnrBack = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTts.stopSpeaking();
                onBackPressed();
            }
        };
        mBackToList.setOnClickListener(lsnrBack);
        TextView mTextBackToList = (TextView) findViewById(R.id.textBackToList);
        mTextBackToList.setOnClickListener(lsnrBack);


        ImageButton mAskAgain = (ImageButton) findViewById(R.id.askagain);
        mAskAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTts.stopSpeaking();
                go2page(AskQuestion.class);
            }
        });

        TextView mTextAskAgain = (TextView) findViewById(R.id.textAskAgain);
        mTextAskAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTts.stopSpeaking();
                go2page(AskQuestion.class);
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

    private void go2page(Class cls) {
        Intent intent = new Intent(this, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mTts.stopSpeaking();
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

    private void setTTSParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        //设置合成

        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        //设置发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoqi");

        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, "50");
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, "50");
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, "50");
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");

        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
    }
}
