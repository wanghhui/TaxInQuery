package cn.madhyama.util;

import android.content.Context;
import android.database.SQLException;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.io.*;

/**
 * Created by WIWANG on 2017/4/22.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    //The Android's default system path of your application database.
    private static String DB_PATH = "/data/data/cn.madhyama.taxinquery/databases/";

    private static String DB_NAME = "taxfts.db";
    private final Context myContext;
    private SQLiteDatabase myDataBase;

    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     *
     * @param context
     */
    public DatabaseHelper(Context context) {

        super(context, DB_NAME, null, 1);
        this.myContext = context;
    }

    /**
     * Creates a empty database on the system and rewrites it with your own database.
     */
    public void createDataBase() throws IOException {

        boolean dbExist = checkDataBase();

        if (dbExist) {
            //do nothing - database already exist
        } else {

            //By calling this method and empty database will be created into the default system path
            //of your application so we are gonna be able to overwrite that database with our database.
            this.getReadableDatabase("");

            try {

                copyDataBase();

            } catch (IOException e) {

                throw new Error("Error copying database");

            }
        }

    }

    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     *
     * @return true if it exists, false if it doesn't
     */
    private boolean checkDataBase() {


        try {
            String myPath = DB_PATH + DB_NAME;
            //checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
            File f = new File(myPath);
            if (!f.exists()) return false;
            FileInputStream copiedDB = new FileInputStream(myPath);

            long c = copiedDB.available();

            InputStream assetDB = myContext.getAssets().open(DB_NAME);
            long a = assetDB.available();

            assetDB.close();
            copiedDB.close();
            if (a == c) return true;


        } catch (SQLiteException e) {

            //database does't exist yet.

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return false;
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     */
    private void copyDataBase() throws IOException {

        //Open your local db as the input stream
        InputStream myInput = myContext.getAssets().open(DB_NAME);

        // Path to the just created empty db
        String outFileName = DB_PATH + DB_NAME;

        //Open the empty db as the output stream
        FileOutputStream myOutput = new FileOutputStream(outFileName);


        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) != -1) {
            myOutput.write(buffer, 0, length);
        }

        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();

    }

    public void openDataBase() throws SQLException {

        //Open the database
        String myPath = DB_PATH + DB_NAME;


        myDataBase = SQLiteDatabase.openDatabase(myPath,"", null, SQLiteDatabase.OPEN_READONLY);


    }

    @Override
    public synchronized void close() {

        if (myDataBase != null)
            myDataBase.close();

        super.close();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    // Add your public helper methods to access and get content from the database.
    // You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
    // to you to create adapters for your views.

}