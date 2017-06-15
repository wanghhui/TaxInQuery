package cn.madhyama.taxinquery;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class QuestionSuggestion extends AppCompatActivity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_suggestion);
        mContentView = findViewById(R.id.fullscreen_content);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String message = intent.getStringExtra("message");
        EditText question = (EditText) findViewById(R.id.manual);
        if (message != null) question.setText(message);
        question.setSelectAllOnFocus(true);


        ImageButton mQuit = (ImageButton) findViewById(R.id.mainpage);
        mQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                go2page(Welcome.class);
            }
        });
        TextView mTextQuit = (TextView) findViewById(R.id.textMainpage);
        mTextQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go2page(Welcome.class);
            }
        });

        ImageButton mWriteQuestion = (ImageButton) findViewById(R.id.writeQuestion);
        View.OnClickListener lsnrBack = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go2page(MatchSpeech.class);
            }
        };
        mWriteQuestion.setOnClickListener(lsnrBack);
        TextView mTextWriteQuestion = (TextView) findViewById(R.id.textWriteQuestion);
        mTextWriteQuestion.setOnClickListener(lsnrBack);


        ImageButton mAskAgain = (ImageButton) findViewById(R.id.askagain);
        mAskAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                go2page(AskQuestion.class);
            }
        });

        TextView mTextAskAgain = (TextView) findViewById(R.id.textAskAgain);
        mTextAskAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
}
