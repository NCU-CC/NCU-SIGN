package tw.edu.ncu.cc.ncunfc.dummy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

import com.wuman.android.auth.OAuthManager;

import java.util.ArrayList;

import tw.edu.ncu.cc.ncunfc.R;
import tw.edu.ncu.cc.ncunfc.dummy.OAuth.AndroidOauthBuilder;
import tw.edu.ncu.cc.ncunfc.dummy.OAuth.NCUNFCClient;
import tw.edu.ncu.cc.ncunfc.dummy.OAuth.NCUNFCConfig;
import tw.edu.ncu.cc.ncunfc.dummy.obj.Course;
import tw.edu.ncu.cc.ncunfc.dummy.sqlLite.CourseTable;
import tw.edu.ncu.cc.ncunfc.dummy.sqlLite.SignTable;

public class MainActivity extends ActionBarActivity {

    //OAuth
    private static final String CLIENT_ID = "20";
    private static final String CALL_BACK = "https://api.cc.ncu.edu.tw/oauth/oauth/test";
    private static final String SECRET = "E.VpwapX7dutp0HDTQyBtNqVBGSEZQVD";

    private CookieManager cookieManager;
    private boolean auth = false;
    public NCUNFCClient ncuNfcClient;

    //debug
    CourseTable courseTable;

    //UI
    private ListView courseListView;
    public static CustomAdapter adapter;
    //為了讓CourseDetailActivity在更改完課程資料後可以notify這個adapter，所以把這個field設為public static
    //不然更改完內容直接跳回來的話，有可能因為listView裡的東西被改過，又沒有(或來不及，因為AsyncTask來不及通知)通知listview的adapter而閃退。
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("debug","MainActivity.OnCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        //for debug
        /*
        initDataBase(this);
        if(courseTable.getCount() <= 0){
            Log.e("debug", "insert result: " + courseTable.insert(new Course(1,"測試課程",System.currentTimeMillis(),"xxx663xxx@gmail.com")));;
        }*/

        //OAuth
        //check network status
        CookieSyncManager.createInstance(this);
        cookieManager = CookieManager.getInstance();

        NetworkInfo networkInfo = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected() || !networkInfo.isAvailable()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            //builder.setIcon(R.drawable.ic_launcher);
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
            //NCUNFCConfig ncuNFCConfig = new NCUNFCConfig("https://appstore.cc.ncu.edu.tw/course/", getString(R.string.language));
            NCUNFCConfig ncuNFCConfig = new NCUNFCConfig("https://api.cc.ncu.edu.tw/oauth/authorize", "zh-TW");
            AndroidOauthBuilder oauthBuilder = AndroidOauthBuilder.initContext(this)
                    .clientID(MainActivity.CLIENT_ID)
                    .clientSecret(MainActivity.SECRET)
                    .callback(MainActivity.CALL_BACK)
                    .scope("CLASS_READ")
                    .fragmentManager(getSupportFragmentManager());
            OAuthManager oAuthManager = oauthBuilder.build();
            ncuNfcClient = new NCUNFCClient(ncuNFCConfig, oAuthManager, this);

            new AuthTask().execute();

        }
    }


    @Override
    public void onResume() {
        Log.e("debug","MainActivity.OnResume");
        super.onResume();
        //利用asynctask從DB拿資料
        new getCourseTask(this, adapter).execute();
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        }

        return super.onOptionsItemSelected(item);
    }


    private class AuthTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            cookieManager.setCookie("portal.ncu.edu.tw", "JSESSIONID=");
            try {
                ncuNfcClient.initAccessToken();
            } catch (Exception e) {
                finish();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
                auth = true;
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
            Log.e("debug","MainActivity.getCourseTask.onPreExecute()");
            super.onPreExecute();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("載入資料中");
            dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            //get courses from DB and pass it to listView adapter
            initDataBase(context);
            ArrayList<Course> courses = courseTable.getAll();

            for(int i=0;i<courses.size();i++) {
                Log.e("debug","courses.get(" + i + ").getName()" + courses.get(i).getName());
            }

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
        new SignTable(context).cleanSDCard();
    }

    private void closeDataBase(){
        if(courseTable != null){
            courseTable.close();
        }

    }

}
