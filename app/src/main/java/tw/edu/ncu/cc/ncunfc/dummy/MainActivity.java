package tw.edu.ncu.cc.ncunfc.dummy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.wuman.android.auth.OAuthManager;

import java.util.ArrayList;

import tw.edu.ncu.cc.ncunfc.R;
import tw.edu.ncu.cc.ncunfc.dummy.OAuth.AndroidOauthBuilder;
import tw.edu.ncu.cc.ncunfc.dummy.OAuth.NCUNFCClient;
import tw.edu.ncu.cc.ncunfc.dummy.obj.Course;
import tw.edu.ncu.cc.ncunfc.dummy.sqlLite.CourseTable;
import tw.edu.ncu.cc.ncunfc.dummy.sqlLite.SignTable;

public class MainActivity extends ActionBarActivity {

    //OAuth
    private CookieManager cookieManager;
    private NCUNFCClient ncuNfcClient;
    private Context context;

    //sql data
    private CourseTable courseTable;

    //UI
    private Menu menu;
    private ListView courseListView;
    private int sortMode = CustomAdapter.SORT_BY_DATE;
    protected static CustomAdapter adapter;
    //為了讓CourseDetailActivity在更改完課程資料後可以notify這個adapter，所以把這個field設為protected static
    //不然更改完內容直接跳回來的話，有可能因為listView裡的東西被改過，又沒有通知(或來不及通知，因為AsyncTask來不及)listview的adapter而閃退。

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        SharedPreferences settings = context.getSharedPreferences("NCUSIGNSettings", Context.MODE_PRIVATE);
        sortMode = settings.getInt("sortmode",CustomAdapter.SORT_BY_CREATE_DATE);

        courseListView = (ListView) findViewById(R.id.listView);
        adapter = new CustomAdapter(this, new ArrayList());
        //設定ListView未取得內容時顯示的view, empty建構在activity_main.xml中。
        courseListView.setEmptyView(findViewById(R.id.empty));
        courseListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent newAct = new Intent();
                newAct.setClass(MainActivity.this, CourseDetailActivity.class);
                Bundle bundle = new Bundle();
                bundle.putLong(CourseTable.SN_COLUMN, adapter.getCourse(position).getSN());
                bundle.putString(CourseTable.NAME_COLUMN, adapter.getCourse(position).getName());
                bundle.putLong(CourseTable.DATE_TIME_COLUMN, adapter.getCourse(position).getDateTime());
                bundle.putString(CourseTable.MAILDES_COLUMN, adapter.getCourse(position).getMailDes());
                newAct.putExtras(bundle);
                closeDataBase();
                startActivity(newAct);
            }
        });
        courseListView.setAdapter(adapter);
        View emptyView = findViewById(R.id.empty);
        emptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent newAct = new Intent();
                newAct.setClass(MainActivity.this, NewCourseActivity.class);
                closeDataBase();
                startActivity(newAct);
            }
        });

        //OAuth
        CookieSyncManager.createInstance(this);
        cookieManager = CookieManager.getInstance();
    }


    @Override
    public void onResume() {
        super.onResume();
        // new getCourseTask(this, adapter).execute();
        adapter.refresh(sortMode);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if(NCUNFCClient.isLoggedIn(context)){
            menu.getItem(1).setTitle("登出");
        }else{
            menu.getItem(1).setTitle("登入");
        }

        // check NFC Feature
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
            // Device not compatible for NFC support
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("此裝置不支援NFC功能，無法開始點名");
            builder.setCancelable(false);
            builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
            return true;
        }

        // check network status before starting OAuth flow
        NetworkInfo networkInfo = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected() || !networkInfo.isAvailable()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("無網路連線");
            builder.setMessage("無網路連線以登入簽到系統");
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.setPositiveButton("設定網路", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                    finish();
                }
            });
            builder.show();
        } else {
            AndroidOauthBuilder oauthBuilder = AndroidOauthBuilder.initContext(this)
                    .clientID(getResources().getString(R.string.clientID))
                    .clientSecret(getResources().getString(R.string.clientSecret))
                    .callback(getResources().getString(R.string.callBack))
                    .scope("user.info.basic.read")
                    .fragmentManager(getSupportFragmentManager());
            OAuthManager oAuthManager = oauthBuilder.build();
            ncuNfcClient = new NCUNFCClient(oAuthManager);
            new AuthTask().execute();//一定要在拿到menu之後才能開啟authTask，不然menu沒有值，要AuthTask要改變menu title的時候會發生nullPointer exception
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item){
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add) {
            Intent newAct = new Intent();
            newAct.setClass(MainActivity.this, NewCourseActivity.class);
            closeDataBase();
            startActivity(newAct);
            return true;
        }else if(id == R.id.action_log_in){
            if(NCUNFCClient.isLoggedIn(context)){
                Log.e("debug", "登出");
                NCUNFCClient.deleteAllToken(context);
                menu.getItem(1).setTitle("登入");
                Toast.makeText(context, "已登出", Toast.LENGTH_SHORT).show();
            }else{
                Log.e("debug", "登入");
                new AuthTask().execute();
                menu.getItem(1).setTitle("登出");
            }
        }else if(id == R.id.action_sort_by_create_date){
            sortMode = CustomAdapter.SORT_BY_CREATE_DATE;
            SharedPreferences settings = context.getSharedPreferences("NCUSIGNSettings", Context.MODE_PRIVATE);
            settings.edit().putInt("sortmode", sortMode).apply();
            adapter.refresh(sortMode);
        }else if(id == R.id.action_sort_by_date){
            sortMode = CustomAdapter.SORT_BY_DATE;
            SharedPreferences settings = context.getSharedPreferences("NCUSIGNSettings", Context.MODE_PRIVATE);
            settings.edit().putInt("sortmode", sortMode).apply();
            adapter.refresh(sortMode);
        }else if(id == R.id.action_sort_by_name){
            sortMode = CustomAdapter.SORT_BY_NAME;
            SharedPreferences settings = context.getSharedPreferences("NCUSIGNSettings", Context.MODE_PRIVATE);
            settings.edit().putInt("sortmode", sortMode).apply();
            adapter.refresh(sortMode);
        }else if(id == R.id.term_of_service){
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("使用條款")
                    .setMessage("一分鐘內試誤次數不得超過5次，超過上限將鎖定點名者之帳號，欲解鎖請寄信至 mobile@cc.ncu.edu.tw")
                    .setCancelable(false)
                    .setPositiveButton("同意", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
        }
        else if(id == R.id.action_announcement){
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("關於")
                    .setMessage("Version：v1.0\n" +
                            "License：MIT License\n" +
                            "Repository：https://github.com/NCU-CC/NCU-SIGN\n" +
                            "Contributors：xxx663xxx\n" +
                            "Owner：National Central University, Computer Center\n" +
                            "Url：https://www.cc.ncu.edu.tw\n" +
                            "Email：mobile@cc.ncu.edu.tw")
                    .setCancelable(false)
                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
        }
        /*else if(id == R.id.action_invalidate_token){//debug
            NCUNFCClient.setTokenUnvalid(context);
        }*/

        return super.onOptionsItemSelected(item);
    }

    private class AuthTask extends AsyncTask<Void, Void, Void> {
            private boolean authSuccess = true;
        @Override
        protected Void doInBackground(Void... params) {
            //debug
            cookieManager.setCookie("portal.ncu.edu.tw", "JSESSIONID=");
            try {
                NCUNFCClient.initAccessToken(context);
                authSuccess = true;
            } catch (Exception e) {
                authSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(!authSuccess){
                Toast.makeText(context, "登入失敗", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(context, "已登入", Toast.LENGTH_LONG).show();
                menu.getItem(1).setTitle("登出");
            }

        }
    }

    private class getCourseTask extends AsyncTask<Void,Void,Void>{

        Context context;
        Dialog dialog;
        CustomAdapter adapter;

        public getCourseTask(Context context, CustomAdapter adapter){
            this.context = context;
            this.adapter = adapter;
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("載入資料中");
            dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            initDataBase(context);
            ArrayList<Course> courses = courseTable.getAll();
            adapter.setArray(courses);
            return null;
        }

        @Override
        protected void onPostExecute(Void unused){
            adapter.notifyDataSetChanged();
            if(dialog!=null && dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    private void initDataBase(Context context){
        this.courseTable = new CourseTable(context);
        SignTable.cleanSDCard();
    }

    private void closeDataBase(){
        if(courseTable != null){
            courseTable.close();
        }
    }

}
