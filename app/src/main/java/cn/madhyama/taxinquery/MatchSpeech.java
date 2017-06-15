package cn.madhyama.taxinquery;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import cn.madhyama.util.DatabaseHelper;
import com.iflytek.cloud.*;
import org.ansj.domain.Term;
import org.ansj.library.DicLibrary;
import org.ansj.splitWord.analysis.DicAnalysis;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

public class MatchSpeech extends AppCompatActivity {
    private static final String SQLLV1_TitleContainsUserInput = "select bszn12366.rowid AS _id, bszn12366.BSZNID, bszn12366.BSZNBT from bszn12366 where bszn12366.BSZNBT like '%";
    private static final String SQLLV2_TitleContainsTaxKeyWords = "select DISTINCT bszn12366.rowid AS _id, bszn12366.BSZNID, bszn12366.BSZNBT from taxqafts2 inner join bszn12366 on bszn12366.bsznid = taxqafts2.FTSID where taxqafts2 match 'title:";
    private static final String SQLLV3_FTSTableContainsTaxKeyWords = "select DISTINCT bszn12366.rowid AS _id, bszn12366.BSZNID, bszn12366.BSZNBT from taxqafts2 inner join bszn12366 on bszn12366.bsznid = taxqafts2.FTSID where taxqafts2 match '";
    private static final String OrderBy = " ORDER BY rank;";
    private static String TAG = MatchSpeech.class.getSimpleName();
    private final Handler mHideHandler = new Handler();
    String speechMessage;
    private View mContentView;
    private final Runnable mHideActionBarAndNavBarRunnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(TaxInQuery.IMMERSE);
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
    private boolean dicLoaded = false;
    private TextUnderstander mTextUnderstander;
    private Toast mToast;
    private EditText speechText;
    private InitListener textUnderstanderListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "textUnderstanderListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码：" + code);
            }
        }
    };
    private TextUnderstanderListener textListener = new TextUnderstanderListener() {

        @Override
        public void onResult(final UnderstanderResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (null != result) {
                        // 显示
                        Log.d(TAG, "understander result：" + result.getResultString());
                        String text = result.getResultString();
                        if (!TextUtils.isEmpty(text)) {
                            try {
                                JSONObject rst = new JSONObject(text);
                                JSONObject answer = rst.getJSONObject("answer");
                                go2answerCloudReturn(answer.getString("text"));

                            } catch (JSONException e) {

                                e.printStackTrace();
                            }
                        }
                    } else {
                        Log.d(TAG, "understander result:null");
                        showTip("识别结果不正确。");
                    }
                }
            });
        }

        @Override
        public void onError(SpeechError error) {
            // 文本语义不能使用回调错误码14002，请确认您下载sdk时是否勾选语义场景和私有语义的发布
            showTip("onError Code：" + error.getErrorCode());

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_speech);
        mContentView = findViewById(R.id.fullscreen_content_ms);
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                decorView.setSystemUiVisibility(TaxInQuery.IMMERSE);
            }
        });
        mTextUnderstander = TextUnderstander.createTextUnderstander(this, textUnderstanderListener);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
//        mContentView.setOnTouchListener(new OnSwipeTouchListener(this){
//            @Override
//            public void onSwipeRight() {
//                back();
//            }
//        });
        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        speechMessage = intent.getStringExtra("speechText");

        // Capture the layout's TextView and set the string as its text
        speechText = (EditText) findViewById(R.id.speechText);
        speechText.setText(speechMessage);

        if (speechMessage != null) {
            if (speechText.length() <= 9) {
                speechText.setMaxLines(1);
                speechText.setTextSize(64);
            }
            if (speechText.length() >= 10 && speechText.length() <= 36) {
                speechText.setMaxLines(2);
                speechText.setTextSize(32);
            }
            if (speechText.length() > 36) {
                speechText.setMaxLines(3);
                speechText.setTextSize(23);
            }
            this.segMsgToSearch();
        } else {
            speechText.requestFocus();
            speechText.setText(R.string.writeDownQuestion);
            speechText.selectAll();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(speechText, InputMethodManager.SHOW_IMPLICIT);
            TextView mResultHint = (TextView) findViewById(R.id.resultHint);
            mResultHint.setText(R.string.pleaseInput);
        }


//        mSearch = (ImageButton)findViewById(R.id.ask_again);
//        mSearch.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                segMsgToSearch();
//            }
//        });

        ImageButton mSuggestion = (ImageButton) findViewById(R.id.suggestion);
        mSuggestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go2page(QuestionSuggestion.class, speechText.getText().toString());
            }
        });
        TextView mTextSuggestion = (TextView) findViewById(R.id.textSuggestion);
        mTextSuggestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go2page(QuestionSuggestion.class, speechText.getText().toString());
            }
        });

        ImageButton mAskAgain = (ImageButton) findViewById(R.id.askagain);
        mAskAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                back();
            }
        });

        TextView mmAskAgainText = (TextView) findViewById(R.id.textAskAgain);
        mmAskAgainText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                back();
            }
        });


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

        speechText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (speechText.length() <= 9) {
                    speechText.setMaxLines(1);
                    speechText.setTextSize(64);
                }
                if (speechText.length() >= 10 && speechText.length() <= 36) {
                    speechText.setMaxLines(2);
                    speechText.setTextSize(32);
                }
                if (speechText.length() > 36) {
                    speechText.setMaxLines(3);
                    speechText.setTextSize(23);
                }
                //set speechMessage to null preventing online search.
                speechMessage = null;
                segMsgToSearch();
            }
        });
    }

    private void segMsgToSearch() {
        //清空 list
        ListView lvItems = (ListView) findViewById(R.id.qlist);
        lvItems.setAdapter(null);
        //String str = "我想知道，请问纳税人办税免税流程和一些具体的办理方法特许权使用费";
        // if(!dicLoaded)loadDic();%
        EditText speechText = (EditText) findViewById(R.id.speechText);
        String message = speechText.getText().toString().trim().replace('%','％');
        message = message.replace('~','～');
        message = message.replace(',','，');
        message = message.replace('.','。');
        message = message.replace('?','？');
        message = message.replace('!','！');
        message = message.replace(':','：');
        message = message.replace('/','、');
        message = message.replace('@','＠');
        message = message.replace('\"','”');
        message = message.replace(';','；');
        message = message.replace('\'','‘');
        message = message.replace('(','（');
        message = message.replace(')','）');
        message = message.replace('<','《');
        message = message.replace('>','》');
        message = message.replace('*','×');
        message = message.replace('&','＆');
        message = message.replace('[','【');
        message = message.replace(']','】');
        message = message.replace('\\','＼');
        message = message.replace('`','·');
        message = message.replace('#','·');
        message = message.replace('$','·');
        message = message.replace('^','·');
        message = message.replace('+','·');
        message = message.replace('-','·');
        message = message.replace('=','·');
        message = message.replace('{','·');
        message = message.replace('}','·');
        message = message.replace('|','·');


        StringBuffer tax_term_matchingclause = new StringBuffer(15);
        StringBuffer large_set_term_matchingclause = new StringBuffer(15);
        if (message.equalsIgnoreCase("")) return;
        for (Term term : DicAnalysis.parse(message)) {
            String termstr = term.getName();
            if (term.natrue().natureStr.equalsIgnoreCase("tax")) tax_term_matchingclause.append("\""+termstr+"\"" + " ");
            if (term.natrue().natureStr.equalsIgnoreCase("large"))large_set_term_matchingclause.append("\""+termstr+"\"" + " ");
        }
        try {

            if ( !localSearchFeedListView(message, tax_term_matchingclause.toString(),large_set_term_matchingclause.toString())&&this.speechMessage != null ) {

                int ret = mTextUnderstander.understandText(speechText.getText().toString(), textListener);
                if (ret != 0) {
                    showTip("语义理解失败,错误码:" + ret);

                }
            }
        } catch (IOException e) {
            Log.e("Understander", e.getMessage());
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

    private boolean localSearchFeedListView(String originalText, String taxMatchClause,String largeMatchClause) throws IOException {
        TextView mResultHint = (TextView) findViewById(R.id.resultHint);
        mResultHint.setText(R.string.MatchingQuestions);
        DatabaseHelper myDbHelper = new DatabaseHelper(this);
        //myDbHelper.createDataBase();
        myDbHelper.openDataBase();

        SQLiteDatabase mydatabase = myDbHelper.getReadableDatabase("");
        Cursor contentSet = null;
        //mydatabase.execSQL("CREATE TABLE \"android_metadata\" (\"locale\" TEXT DEFAULT 'en_US')");
        //mydatabase.execSQL("INSERT INTO \"android_metadata\" VALUES ('en_US')");
        if(originalText.length()>0) {
            contentSet = mydatabase.rawQuery(SQLLV2_TitleContainsTaxKeyWords + originalText + "'"+OrderBy, null);
            if (contentSet != null && contentSet.getCount() > 0) {
                fillList(contentSet);
                return true;
            }
        }

        if(largeMatchClause.length()>0) {
            contentSet = mydatabase.rawQuery(SQLLV3_FTSTableContainsTaxKeyWords + largeMatchClause + "'"+OrderBy, null);
            if (contentSet != null && contentSet.getCount() > 0) {
                fillList(contentSet);
                return true;
            }

        }

        if(taxMatchClause.length()>0) {

            contentSet = mydatabase.rawQuery(SQLLV3_FTSTableContainsTaxKeyWords + taxMatchClause + "'"+OrderBy, null);
            if (contentSet != null && contentSet.getCount() > 0) {
                fillList(contentSet);
                return true;
            }
        }


        mResultHint.setText(R.string.MatchingNull);
        return false;
    }

    private void fillList(Cursor contentSet) {
        // Find ListView to populate
        ListView lvItems = (ListView) findViewById(R.id.qlist);
        // Setup cursor adapter using cursor from last step
        ClientCursorAdapter adapter = new ClientCursorAdapter(this, contentSet);
        // Attach cursor adapter to the ListView
        lvItems.setAdapter(adapter);
        lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView mTextbsznid = (TextView) view.findViewById(R.id.bsznid);
                String bsznid = mTextbsznid.getText().toString().trim();
                go2answer(bsznid);
            }
        });

        ((TextView)findViewById(R.id.resultHint)).setText(getResources().getString(R.string.MatchingQuestions) + lvItems.getCount()+"条");

    }

    public void go2answer(String bsznid) {
        Intent intent = new Intent(this, PostAnswer.class);
        intent.putExtra("bsznid", bsznid);
        if (bsznid.length() > 2) startActivity(intent);
    }

    public void go2answerCloudReturn(String answer) {
        Intent intent = new Intent(this, PostAnswer.class);
        intent.putExtra("question", speechText.getText().toString());
        intent.putExtra("answer", answer);
        startActivity(intent);
    }

    private void go2page(Class cls) {
        Intent intent = new Intent(this, cls);
        startActivity(intent);

    }

    private void go2page(Class cls, String message) {
        Intent intent = new Intent(this, cls);
        intent.putExtra("message", message);
        startActivity(intent);

    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        immersiveMode();
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        immersiveMode();
                    }
                });
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

    public void immersiveMode() {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(TaxInQuery.IMMERSE);
    }

    public void back() {
        super.onBackPressed();
    }

    public class ClientCursorAdapter extends CursorAdapter {
        public ClientCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
        }

        // The newView method is used to inflate a new view and return it,
        // you don't bind any data to the view at this point.
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.question_list, parent, false);
        }

        // The bindView method is used to bind all data to a given view
        // such as setting the text on a TextView.
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Find fields to populate in inflated template
            TextView mTextbsznid = (TextView) view.findViewById(R.id.bsznid);
            TextView mTextbsznbt = (TextView) view.findViewById(R.id.bsznbt);
            // Extract properties from cursor
            String bsznid = cursor.getString(1);
            String bsznbt = cursor.getString(2);
            // Populate fields with extracted properties
            mTextbsznid.setText(bsznid);
            mTextbsznbt.setText(bsznbt);
        }


    }
}
