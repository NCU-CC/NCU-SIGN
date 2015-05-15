package tw.edu.ncu.cc.ncunfc.dummy.sqlLite;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
import tw.edu.ncu.cc.ncunfc.dummy.obj.Course;
import tw.edu.ncu.cc.ncunfc.dummy.obj.SignRecord;

// 資料功能類別
public class SignTable {
    // 表格名稱(為該課的SN)
    public static String TABLE_NAME = "sign_table";

    // 表格欄位名稱
    public static final String SN_COLUMN = "SN";
    public static final String SIGNTIME_COLUMN = "signtime";
    public static final String UNIT_COLUMN = "unit";
    public static final String NAME_COLUMN = "name";

    // 使用上面宣告的變數建立表格的SQL指令
    public static String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    SN_COLUMN + " TEXT, " +
                    SIGNTIME_COLUMN + " INTEGER, " +
                    UNIT_COLUMN + " TEXT, " +
                    NAME_COLUMN + " TEXT)";

    // 資料庫物件
    private SQLiteDatabase db;

    // 建構子，一般的應用都不需要修改
    public SignTable(Context context) {
        db = MyDBHelper.getDatabase(context);
    }

    // 關閉資料庫，一般的應用都不需要修改
    public void close() {
        db.close();
    }

    // 新增參數指定的物件
    public long insert(SignRecord s) {
        // 建立準備新增資料的ContentValues物件
        ContentValues cv = new ContentValues();

        // 加入ContentValues物件包裝的新增資料
        // 第一個參數是欄位名稱， 第二個參數是欄位的資料
        cv.put(SN_COLUMN, s.getSN());
        Log.e("debug","s.getSN():" + s.getSN());
        cv.put(SIGNTIME_COLUMN, s.getSignTime());
        cv.put(UNIT_COLUMN, s.getUnit());
        cv.put(NAME_COLUMN, s.getName());

        // 新增一筆資料並取得編號
        // 第一個參數是表格名稱
        // 第二個參數是沒有指定欄位值的預設值
        // 第三個參數是包裝新增資料的ContentValues物件
        return db.insert(TABLE_NAME, null, cv);

    }

    /*
    // 修改參數指定的物件
    public boolean update(Item item) {
        // 建立準備修改資料的ContentValues物件
        ContentValues cv = new ContentValues();

        // 加入ContentValues物件包裝的修改資料
        // 第一個參數是欄位名稱， 第二個參數是欄位的資料
        cv.put(DATETIME_COLUMN, item.getDatetime());
        cv.put(COLOR_COLUMN, item.getColor().parseColor());
        cv.put(TITLE_COLUMN, item.getTitle());
        cv.put(CONTENT_COLUMN, item.getContent());
        cv.put(FILENAME_COLUMN, item.getFileName());
        cv.put(LATITUDE_COLUMN, item.getLatitude());
        cv.put(LONGITUDE_COLUMN, item.getLongitude());
        cv.put(LASTMODIFY_COLUMN, item.getLastModify());

        // 設定修改資料的條件為編號
        // 格式為「欄位名稱＝資料」
        String where = KEY_ID + "=" + item.getId();

        // 執行修改資料並回傳修改的資料數量是否成功
        return db.update(TABLE_NAME, cv, where, null) > 0;
    }

    // 刪除參數指定編號的資料
    public boolean delete(long id){
        // 設定條件為編號，格式為「欄位名稱=資料」
        String where = KEY_ID + "=" + id;
        // 刪除指定編號資料並回傳刪除是否成功
        return db.delete(TABLE_NAME, where , null) > 0;
    }
    */

    // 讀取所有記事資料
    public ArrayList<SignRecord> getAll() {
        ArrayList<SignRecord> result = new ArrayList<SignRecord>();
        Cursor cursor = db.query(
                TABLE_NAME, null, null, null, null, null, null, null);

        cursor.moveToFirst();

        //debug
        int count = 0;
        while (cursor.moveToNext()) {
            Log.e("debug","cursor.getString(0):" + cursor.getString(0));
            result.add(getRecord(cursor));
            Log.e("debug","result.get("+count+").getSN()" + result.get(count).getSN());
            Log.e("debug","result.get("+count+").getName()" + result.get(count).getName());
            count++;
        }

        for (SignRecord re : result) {
            Log.e("debug", re.getName());
        }

        Log.e("debug","dump:"+DatabaseUtils.dumpCursorToString(cursor));

        cursor.close();
        for (SignRecord re : result) {
            Log.e("debug", re.getName());
        }

        return result;
    }

    // 取得指定編號的資料物件
    public ArrayList<SignRecord> get(String SN) {
        ArrayList<SignRecord> result = new ArrayList<>();

        // 使用編號為查詢條件
        String where = SN_COLUMN + "=" + SN;
        // 執行查詢
        Cursor cursor = db.query(
                TABLE_NAME, null, where, null, null, null, null, null);

        while (cursor.moveToNext()) {
            result.add(getRecord(cursor));
        }

        // 關閉Cursor物件
        cursor.close();
        // 回傳結果
        return result;
    }

    // 把Cursor目前的資料包裝為物件
    public SignRecord getRecord(Cursor cursor) {
        // 準備回傳結果用的物件
        SignRecord result = new SignRecord();

        result.setSN(cursor.getString(0));
        result.setTime(cursor.getLong(1));
        result.setUnit(cursor.getString(2));
        result.setName(cursor.getString(3));

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

    // 建立範例資料
    public void sample() {
        SignRecord s0 = new SignRecord("0", System.currentTimeMillis(), "unit0", "name0");
        insert(s0);
        Log.e("debug","s0.getSN():" + s0.getSN());

        SignRecord s1 = new SignRecord("1", System.currentTimeMillis(), "unit1", "name1");
        insert(s1);
        Log.e("debug","s1.getSN():" + s1.getSN());

        SignRecord s2 = new SignRecord("2", System.currentTimeMillis(), "unit2", "name2");
        insert(s2);
        Log.e("debug","s2.getSN():" + s2.getSN());

        SignRecord s3 = new SignRecord("test", System.currentTimeMillis(), "unit3", "name3");
        insert(s3);
        Log.e("debug","s3.getSN():" + s3.getSN());

    }

    //建立excel檔並回傳檔案路徑
    public File getExcelFilePath(Course c){
        // 使用編號為查詢條件
        String where = SN_COLUMN + "=" + c.getSN();
        // 執行查詢
        Cursor cursor = db.query(
                TABLE_NAME, null, where, null, null, null, null, null);

        final String fileName = c.getName() + "課程簽到記錄.xls";

        //Saving file in external storage
        File sdCard = Environment.getExternalStorageDirectory();
        File directory = new File(sdCard.getAbsolutePath() + "/ncuNFC");

        //create directory if not exist
        if(!directory.isDirectory()){
            directory.mkdirs();
        }

        //file path
        File file = new File(directory, fileName);

        WorkbookSettings wbSettings = new WorkbookSettings();
        //wbSettings.setLocale(new Locale("en", "EN"));
        WritableWorkbook workbook;

        try {
            workbook = Workbook.createWorkbook(file, wbSettings);
            //Excel sheet name. 0 represents first sheet
            WritableSheet sheet = workbook.createSheet("SignList", 0);

            try {
                sheet.addCell(new Label(0, 0, "簽到時間")); // column and row
                sheet.addCell(new Label(1, 0, "單位"));
                sheet.addCell(new Label(2, 0, "姓名"));
                if (cursor.moveToFirst()) {
                    do {
                        //String title = cursor.getString(cursor.getColumnIndex(DatabaseHelper.TODO_SUBJECT));
                        //String desc = cursor.getString(cursor.getColumnIndex(DatabaseHelper.TODO_DESC));

                        String signTime = new Timestamp(cursor.getLong(cursor.getColumnIndex(SignTable.SIGNTIME_COLUMN))).toString();
                        String name = cursor.getString(cursor.getColumnIndex(SignTable.NAME_COLUMN));
                        String unit = cursor.getString(cursor.getColumnIndex(SignTable.UNIT_COLUMN));

                        int i = cursor.getPosition() + 1;
                        sheet.addCell(new Label(0, i, signTime));
                        sheet.addCell(new Label(1, i, unit));
                        sheet.addCell(new Label(2, i, name));
                    } while (cursor.moveToNext());
                }
                //closing cursor
                cursor.close();
            } catch (RowsExceededException e) {
                e.printStackTrace();
            } catch (WriteException e) {
                e.printStackTrace();
            }
            workbook.write();
            try {
                workbook.close();
            } catch (WriteException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    //借用完手機的空間建立好excel表並寄出去之後，要把占用的空間清乾淨
    public void cleanSDCard(){
        try{
            File sdCard = Environment.getExternalStorageDirectory();
            File directory = new File(sdCard.getAbsolutePath() + "/ncuNFC");
            deleteRecurisively(directory);
        }catch(Exception e){//暫存資料夾被清掉的話，就沒有辦法建立File物件了
            e.printStackTrace();
        }

    }

    //檢查外部儲存空間可否寫入(android不保證外部空間的狀態一直處於可存取的狀態)
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    //檢查外部儲存空間可否讀取()
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public void deleteRecurisively(File f){
        File[] childrenFile = f.listFiles();
        if(childrenFile != null || childrenFile.length != 0) {
            for (int i = 0; i < childrenFile.length; i++) {
                deleteRecurisively(childrenFile[i]);
            }
        }
        f.delete();
    }

}