package tw.edu.ncu.cc.ncunfc.dummy.sqlLite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

import tw.edu.ncu.cc.ncunfc.dummy.obj.Course;


// 資料功能類別
public class CourseTable {
    // 表格名稱(為該課的SN)
    public static String TABLE_NAME = "course_table";

    // 表格欄位名稱
    public static final String SN_COLUMN = "SN";
    public static final String NAME_COLUMN = "course_name";
    public static final String DATE_TIME_COLUMN = "course_date";
    public static final String MAILDES_COLUMN = "mail_des";

    // 使用上面宣告的變數建立表格的SQL指令
    public static String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    SN_COLUMN + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    NAME_COLUMN + " TEXT, " +
                    DATE_TIME_COLUMN + " INTEGER, " +
                    MAILDES_COLUMN + " TEXT)";

    // 資料庫物件
    private SQLiteDatabase db;

    // 建構子，一般的應用都不需要修改
    public CourseTable(Context context) {
        db = MyDBHelper.getDatabase(context);
    }

    // 關閉資料庫，一般的應用都不需要修改
    public void close() {
        db.close();
    }

    // 新增參數指定的物件
    public long insert(Course c) {
        // 建立準備新增資料的ContentValues物件
        ContentValues cv = new ContentValues();

        // 加入ContentValues物件包裝的新增資料
        // 第一個參數是欄位名稱， 第二個參數是欄位的資料
        //cv.put(SN_COLUMN, c.getSN());//AUTOINCREMENT
        cv.put(NAME_COLUMN, c.getName());
        cv.put(DATE_TIME_COLUMN, c.getDateTime());
        cv.put(MAILDES_COLUMN, c.getMailDes());

        // 新增一筆資料並取得編號
        // 第一個參數是表格名稱
        // 第二個參數是沒有指定欄位值的預設值
        // 第三個參數是包裝新增資料的ContentValues物件
        return db.insert(TABLE_NAME, null, cv);

    }

    // 修改參數指定的物件
    public boolean update(Course c) {
        // 建立準備修改資料的ContentValues物件
        ContentValues cv = new ContentValues();

        // 加入ContentValues物件包裝的修改資料
        // 第一個參數是欄位名稱， 第二個參數是欄位的資料
        cv.put(SN_COLUMN, c.getSN());
        cv.put(NAME_COLUMN, c.getName());
        cv.put(DATE_TIME_COLUMN, c.getDateTime());
        cv.put(MAILDES_COLUMN, c.getMailDes());

        // 設定修改資料的條件為編號
        // 格式為「欄位名稱＝資料」
        String where = SN_COLUMN + "=" + c.getSN();

        // 執行修改資料並回傳修改的資料數量是否成功
        return db.update(TABLE_NAME, cv, where, null) > 0;
    }


    // 刪除參數指定編號的資料
    public boolean delete(long SN){
        // 設定條件為編號，格式為「欄位名稱=資料」
        String where = SN_COLUMN + "=" + SN;
        // 刪除指定編號資料並回傳刪除是否成功
        return db.delete(TABLE_NAME, where , null) > 0;
    }

    // 讀取所有記事資料
    public ArrayList<Course> getAll() {
        ArrayList<Course> result = new ArrayList<Course>();
        Cursor cursor = db.query(
                TABLE_NAME, null, null, null, null, null, null, null);

        //debug
        int count = 0;
        while (cursor.moveToNext()) {
            result.add(getCourse(cursor));
            count++;
        }

        Log.e("debug","result.size:"+ result.size());

        cursor.close();

        return result;
    }

    // 取得指定編號的資料物件
    public ArrayList<Course> get(String SN) {
        ArrayList<Course> result = new ArrayList<Course>();

        // 使用編號為查詢條件
        String where = SN_COLUMN + "=" + SN;
        // 執行查詢
        Cursor cursor = db.query(
                TABLE_NAME, null, where, null, null, null, null, null);

        cursor.moveToFirst();
        while (cursor.moveToNext()) {
            result.add(getCourse(cursor));
        }

        // 關閉Cursor物件
        cursor.close();
        // 回傳結果
        return result;
    }

    // 把Cursor目前的資料包裝為物件
    public Course getCourse(Cursor cursor) {
        // 準備回傳結果用的物件
        Course result = new Course();

        result.setSN(cursor.getLong(0));
        result.setName(cursor.getString(1));
        result.setDateTime(cursor.getLong(2));
        result.setMailDes(cursor.getString(3));

        // 回傳結果
        return result;
    }

    // 取得資料數量(目前只有insertDummydata才會用到)
    public int getCount() {
        int result = 0;
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);

        if (cursor.moveToNext()) {
            result = cursor.getInt(0);
        }

        return result;
    }
}
