package tw.edu.ncu.cc.ncunfc.dummy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Locale;

import tw.edu.ncu.cc.ncunfc.R;
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
    private String currentStudentID;

    //Activity UIs
    private TextView statusTextView;
    private Button stopButton;

    //Course Data
    private Course course;
    private int mCount = 0;

    //Sql data
    private SignTable signTable;

    //Sound effects
    MediaPlayer beepPlayer;
    MediaPlayer errorPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign);

        Bundle bundle = this.getIntent().getExtras();
        long SN = bundle.getLong(CourseTable.SN_COLUMN, 1);
        String name = bundle.getString(CourseTable.NAME_COLUMN, "null");
        long date = bundle.getLong(CourseTable.DATE_TIME_COLUMN, System.currentTimeMillis());
        String mailDes = bundle.getString(CourseTable.MAILDES_COLUMN, "null");
        course = new Course(SN, name, date, mailDes);

        initView();
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
        mAdapter = NfcAdapter.getDefaultAdapter(this);

        //unchecked
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
        }

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


    @Override
    public void onNewIntent(Intent intent) {
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.i("Foreground dispatch", "Discovered tag with intent: " + intent);
        String cardID = bytesToHex(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
        //statusTextView.append("\nDiscovered tag " + ++mCount + " with intent: " + intent + "ID: " + cardID);
        new getStudentDataTask(this, course.getSN(),
                System.currentTimeMillis(), cardID).execute();
        beepPlayer.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.disableForegroundDispatch(this);
        //throw new RuntimeException("onPause not implemented to fix build");
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

    private class getStudentDataTask extends AsyncTask<Void,Void,Void> {

        private final WeakReference<SignActivity> signActivityWeakRef;
        String SN;
        Long time;
        String cardID;
        Dialog dialog;

        DummyResponse response;

        public getStudentDataTask(SignActivity s, long SN, Long time, String cardID){
            super();
            this.signActivityWeakRef =new WeakReference<SignActivity>(s);
            this.SN = "" + SN;
            this.time = time;
            this.cardID = cardID;
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            if (signActivityWeakRef.get() != null && !signActivityWeakRef.get().isFinishing()){
                AlertDialog.Builder builder = new AlertDialog.Builder(SignActivity.this);
                builder.setMessage("取得學生資料中");
                dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
            }

        }

        @Override
        protected Void doInBackground(Void... params) {
            //call api get student data and save into sql table
            response = dummyApiCall(cardID);
            SignRecord temp = new SignRecord();
            temp.setName(response.dummyName);
            temp.setUnit(response.dummyUnit);
            temp.setTime(this.time);
            temp.setSN(this.SN);
            signTable.insert(temp);
            return null;
        }

        @Override
        protected void onPostExecute(Void unused){
            if (signActivityWeakRef.get() != null && !signActivityWeakRef.get().isFinishing()){
                dialog.dismiss();
            }
            updateStatusTextView(response.dummyName, response.dummyUnit, this.time);
            errorPlayer.start();
        }

        private DummyResponse dummyApiCall(String cardID){
            if(cardID.equals("E098AFFC")){
                return new DummyResponse("資工二A","胡家銘");
            }
            return new DummyResponse("不認識的單位","不認識的人");
        }

        private class DummyResponse{
            public String dummyUnit;
            public String dummyName;
            private DummyResponse(String dummyUnit, String dummyName){
                this.dummyUnit = dummyUnit;
                this.dummyName = dummyName;
            }
        }
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
                               "\n卡號：" + unit +
                               "\n簽到時間：" + new Timestamp(timeStamp).toString());
    }

}
