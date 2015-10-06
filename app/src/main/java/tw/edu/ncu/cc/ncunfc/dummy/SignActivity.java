package tw.edu.ncu.cc.ncunfc.dummy;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.wuman.android.auth.OAuthManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import tw.edu.ncu.cc.ncunfc.R;
import tw.edu.ncu.cc.ncunfc.dummy.OAuth.AndroidOauthBuilder;
import tw.edu.ncu.cc.ncunfc.dummy.OAuth.NCUNFCClient;
import tw.edu.ncu.cc.ncunfc.dummy.obj.Course;
import tw.edu.ncu.cc.ncunfc.dummy.obj.SignRecord;
import tw.edu.ncu.cc.ncunfc.dummy.sqlLite.CourseTable;
import tw.edu.ncu.cc.ncunfc.dummy.sqlLite.SignTable;


public class SignActivity extends ActionBarActivity {

    //NFC components
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private static final String TAG = NfcA.class.getSimpleName();

    //Activity UIs
    private Context context;
    private TextView statusTextView;
    private Button stopButton;

    //Dialog, used to display sign status message
    private static AlertDialog dialog;

    //Course Data, the one that displays on UI
    private Course course;

    //Sql data
    private SignTable signTable;

    //api
    private RequestQueue queue;
    private String baseURL;

    //Sound effects
    private MediaPlayer beepPlayer;
    private MediaPlayer errorPlayer;

    //boolean to judge the intent that start the activity
    boolean calledByActivity = false;

    //boolean to judge whether the phone NFC feature
    boolean hasNFC = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign);

        queue= Volley.newRequestQueue(this);

        Bundle bundle = this.getIntent().getExtras();
        long SN = bundle.getLong(CourseTable.SN_COLUMN, 1);
        String name = bundle.getString(CourseTable.NAME_COLUMN, "null");
        long date = bundle.getLong(CourseTable.DATE_TIME_COLUMN, System.currentTimeMillis());
        String mailDes = bundle.getString(CourseTable.MAILDES_COLUMN, "null");
        course = new Course(SN, name, date, mailDes);

        calledByActivity = bundle.getBoolean("CALLED_BY_ACTIVITY",false);

        initView();
        context = this;
        initSounds();
        setListeners(this);
        initDataBase(this);

        // Create a generic PendingIntent that will be deliver to this activity. The NFC stack
        // will fill in the intent with the details of the discovered tag before delivering to
        // this activity.
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Setup an intent filter for all MIME based dispatches
        IntentFilter ntec = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        try {
            ntec.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        mFilters = new IntentFilter[] {
                ntec,
        };

        // Setup a tech list for all NfcF tags
        mTechLists = new String[][] { new String[] { NfcA.class.getName() } };
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
            // Device not compatible for NFC support
            AlertDialog.Builder builder = new AlertDialog.Builder(SignActivity.this);
            builder.setMessage("此裝置不支援NFC功能，無法開始點名");
            builder.setCancelable(false);
            builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });
            builder.create().show();
            hasNFC = false;
            return;
        }else {
            hasNFC = true;
        }

        NetworkInfo networkInfo = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected() || !networkInfo.isAvailable()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("無網路連線");
            builder.setMessage("無網路連線取得學生資料");
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
        }

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        if(mAdapter == null || !mAdapter.isEnabled()){
            AlertDialog.Builder builder = new AlertDialog.Builder(SignActivity.this);
            builder.setMessage("此裝置未開啟NFC，請至\"設定\"->\"NFC\"開啟NFC功能");
            builder.setCancelable(false);
            builder.setPositiveButton("設定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
                    dialog.dismiss();
                }
            });
            builder.create().show();
            return;
        }
        mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_nfc, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {/*
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
            return true;
        }
        */
        return super.onOptionsItemSelected(item);
    }

    //偵測到NFC卡後要做什麼事
    @Override
    public void onNewIntent(Intent intent) {
        if(!calledByActivity){
            this.finish();
            return;
        }
        final String cardID = bytesToHex(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
        final View view = getLayoutInflater()
                .inflate(R.layout.dialog_id, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(SignActivity.this);
        builder.setTitle("請輸入身分證後四碼");
        builder.setView(view);
        builder.setPositiveButton("確認", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                EditText editText = (EditText) view.findViewById(R.id.editText);
                String id = editText.getText().toString();
                dialog.dismiss();
                //authenticate(cardID + "?id=" + id, System.currentTimeMillis(), course.getSN());
                authenticate(cardID + "?id=" + "7297", System.currentTimeMillis(), course.getSN());//展示用途
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
        beepPlayer.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(hasNFC){
            mAdapter.disableForegroundDispatch(this);
        }
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void authenticate(String httpGetParams, final long signTime, final long SN){
        baseURL = "https://api.cc.ncu.edu.tw/personnel/v1/cards/";//testing baseURL
        if(System.currentTimeMillis() > NCUNFCClient.tokenValidUntil){
            NCUNFCClient.refreshAccessToken(context);
        }
        queue.add( new StringRequest( Request.Method.GET, baseURL + httpGetParams,
                new Response.Listener< String >() {
                    @Override
                    public void onResponse( String response ) {
                        Log.e("debug",response);
                        try {
                            JSONObject obj = new JSONObject(response);
                           //save student data into sql table
                            SignRecord temp = new SignRecord();
                            temp.setName((String) obj.get("name"));
                            temp.setUnit((String) obj.get("unit"));
                            temp.setTime(signTime);
                            String SNString = SN+"";
                            temp.setSN(SNString);
                            signTable.insert(temp);
                            Toast.makeText(context,"簽到成功",Toast.LENGTH_LONG).show();
                            updateStatusTextView((String) obj.get("name"), (String) obj.get("unit"), signTime);
                        } catch (JSONException e) {
                            Log.e("debug", e.toString());
                            e.printStackTrace();
                            Toast.makeText(context, "簽到失敗", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse( VolleyError error ) {
                        if(error.networkResponse.statusCode == 401){
                            Toast.makeText(context, "簽到失敗，請點名者重新登入", Toast.LENGTH_LONG).show();
                            NCUNFCClient.deleteAllToken(context);
                            AndroidOauthBuilder oauthBuilder = AndroidOauthBuilder.initContext(context)
                                    .clientID(getResources().getString(R.string.clientID))
                                    .clientSecret(getResources().getString(R.string.clientSecret))
                                    .callback(getResources().getString(R.string.callBack))
                                    .scope("user.info.basic.read")
                                    .fragmentManager(getSupportFragmentManager());
                            OAuthManager oAuthManager = oauthBuilder.build();
                            NCUNFCClient ncuNfcClient = new NCUNFCClient(oAuthManager);
                            new AuthTask().execute();
                        }else{
                            Toast.makeText(context, "簽到失敗", Toast.LENGTH_LONG).show();
                        }
                        Log.e("debug", error.toString());

                        errorPlayer.start();
                    }
                }
        ) {
            public Map< String, String > getHeaders() throws AuthFailureError {
                Map< String, String > headers = new HashMap<>();
                //headers.put( "X-NCU-API-TOKEN", getResources().getString(R.string.apiToken));
                headers.put( "Authorization", "Bearer " + NCUNFCClient.accessToken);
                return headers;
            }
        } );
    }

    private void initDataBase(Context context){
        signTable = new SignTable(context);
    }

    private void closeDataBase(){
        signTable.close();
    }

    private void initView(){
        statusTextView = (TextView) findViewById(R.id.status_textView);
        stopButton = (Button) findViewById(R.id.stop_button);

        TextView tempView = (TextView) findViewById(R.id.name_textView);
        tempView.setText(course.getName());

        Timestamp temp = new Timestamp(course.getDateTime());
        String[] tempString  = temp.toString().split(" ");
        String dateString = tempString[0];
        String timeString = tempString[1].substring(0, tempString[1].lastIndexOf(":"));

        tempView = (TextView) findViewById(R.id.date_textView);
        tempView.setText(dateString);

        tempView = (TextView) findViewById(R.id.time_textView);
        tempView.setText(timeString);
    }

    private void initSounds(){
        beepPlayer = MediaPlayer.create(this, R.raw.beep);
        errorPlayer = MediaPlayer.create(this, R.raw.error);
    }

    private void setListeners(final SignActivity s){
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDataBase();
                s.finish();
            }
        });
    }

    public void updateStatusTextView(String name, String unit, long timeStamp){
        statusTextView.setText("姓名：" + name +
                "\n單位：" + unit +
                "\n簽到時間：" + new Timestamp(timeStamp).toString());
    }

    private class AuthTask extends AsyncTask<Void, Void, Void> {
        private boolean authSuccess = true;
        @Override
        protected Void doInBackground(Void... params) {
            //debug
            CookieSyncManager.createInstance(context);
            CookieManager cookieManager = CookieManager.getInstance();
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
            }

        }
    }

}
