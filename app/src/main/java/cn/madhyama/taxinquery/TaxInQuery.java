package cn.madhyama.taxinquery;

import android.app.Application;
import android.util.Log;
import android.view.View;
import cn.madhyama.util.DatabaseHelper;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import org.ansj.library.DicLibrary;
import org.ansj.splitWord.analysis.DicAnalysis;
import net.sqlcipher.database.SQLiteDatabase;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.content.ContentValues.TAG;

/**
 * Created by WIWANG on 2017/5/6.
 * TaxInQuery
 */
public class TaxInQuery extends Application {
    final static int IMMERSE = View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

    @Override
    public void onCreate() {

        String param = ("appid=" + getString(R.string.app_id)) +
                "," +
                SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC;
        // 设置使用v5+
        SpeechUtility.createUtility(TaxInQuery.this, param);
        super.onCreate();
        SQLiteDatabase.loadLibs(this);
        DatabaseHelper myDbHelper = new DatabaseHelper(this);
        try {
            myDbHelper.createDataBase();
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            InputStream taxDic = this.getAssets().open("library/keywords_tax.txt");
            BufferedReader bf = new BufferedReader(new InputStreamReader(taxDic));


            String s;
            while ((s = bf.readLine()) != null) {

                DicLibrary.insert(DicLibrary.DEFAULT, s, "tax", 1000000);
            }

            InputStream large_set = this.getAssets().open("library/keywords_largest.txt");
            BufferedReader large_set_bf = new BufferedReader(new InputStreamReader(taxDic));

            while ((s = bf.readLine()) != null) {

                DicLibrary.insert(DicLibrary.DEFAULT, s, "large", 999999);
            }


            bf.close();
            taxDic.close();
            large_set.close();
            large_set_bf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Log.i(TAG, "预处理" + DicAnalysis.parse("ansj"));
    }
}
