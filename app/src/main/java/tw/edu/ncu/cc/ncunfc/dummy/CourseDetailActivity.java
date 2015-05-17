package tw.edu.ncu.cc.ncunfc.dummy;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import tw.edu.ncu.cc.ncunfc.R;
import tw.edu.ncu.cc.ncunfc.dummy.obj.Course;
import tw.edu.ncu.cc.ncunfc.dummy.obj.SignRecord;
import tw.edu.ncu.cc.ncunfc.dummy.sqlLite.CourseTable;
import tw.edu.ncu.cc.ncunfc.dummy.sqlLite.SignTable;

public class CourseDetailActivity extends ActionBarActivity {

    //UI
    private Context context;
    private Button checkButton;
    private Button sendButton;
    private Button saveButton;
    private Button startButton;
    private Button deleteButton;
    private EditText nameEditText;
    private EditText dateEditText;
    private EditText timeEditText;
    private EditText mailDesEditText;
    private DatePickerDialog dateDialog;
    private TimePickerDialog timeDialog;

    //Data
    private Course course;
    private Calendar courseDateTime;

    //DB
    private CourseTable courseTable;
    private SignTable signTable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        this.context = this;

        //取得intent中的bundle物件
        Bundle bundle = this.getIntent().getExtras();
        long SN = bundle.getLong(CourseTable.SN_COLUMN, 1);
        String name = bundle.getString(CourseTable.NAME_COLUMN, "null");
        long date = bundle.getLong(CourseTable.DATE_TIME_COLUMN, System.currentTimeMillis());
        String mailDes = bundle.getString(CourseTable.MAILDES_COLUMN,"null");
        course = new Course(SN, name, date, mailDes);
        courseDateTime = Calendar.getInstance();
        courseDateTime.setTimeInMillis(course.getDateTime());

        initView();
        initDataBase();
        setOnClickListeners();

        //debug
        insertSampleIntoDataBase();
    }

    @Override
    public void onResume() {
        super.onResume();
        initDataBase();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.menu_course_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        //}

        return super.onOptionsItemSelected(item);
    }

    private void initView(){
        checkButton = (Button) findViewById(R.id.check_button);
        sendButton = (Button) findViewById(R.id.send_button);
        saveButton = (Button) findViewById(R.id.save_button);
        startButton = (Button) findViewById(R.id.start_button);
        deleteButton = (Button) findViewById(R.id.delete_button);

        nameEditText = (EditText) findViewById(R.id.name_editText);
        nameEditText.setText(course.getName());

        dateEditText = (EditText) findViewById(R.id.date_editText);
        timeEditText = (EditText) findViewById(R.id.time_editText);

        Timestamp temp = new Timestamp(course.getDateTime());
        String[] tempString  = temp.toString().split(" ");
        String dateString = tempString[0];
        String timeString = tempString[1].substring(0,tempString[1].lastIndexOf(":"));

        dateEditText.setText(dateString);
        timeEditText.setText(timeString);

        mailDesEditText = (EditText) findViewById(R.id.email_editText);
        mailDesEditText.setText(course.getMailDes());

        dateDialog = new DatePickerDialog(CourseDetailActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                courseDateTime.set(Calendar.YEAR, year);
                courseDateTime.set(Calendar.MONTH, monthOfYear);
                courseDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateEditText();
            }
        }
                , courseDateTime.get(Calendar.YEAR)
                , courseDateTime.get(Calendar.MONTH)
                , courseDateTime.get(Calendar.DAY_OF_MONTH));

        timeDialog = new TimePickerDialog(CourseDetailActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                courseDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                courseDateTime.set(Calendar.MINUTE, minute);
                updateTimeEditText();
            }
        }
                , courseDateTime.get(Calendar.HOUR_OF_DAY)
                , courseDateTime.get(Calendar.MINUTE), true);
    }

    private void updateDateEditText(){
        Timestamp temp = new Timestamp(courseDateTime.getTimeInMillis());
        String[] tempString  = temp.toString().split(" ");

        dateEditText.setText(tempString[0]);
    }

    private void updateTimeEditText(){
        Timestamp temp = new Timestamp(courseDateTime.getTimeInMillis());
        String[] tempString  = temp.toString().split(" ");
        String timeString = tempString[1].substring(0,tempString[1].lastIndexOf(":"));

        timeEditText.setText(timeString);

    }

    private void initDataBase(){
        signTable = new SignTable(getApplicationContext());
        signTable.cleanSDCard();
        courseTable = new CourseTable(context);
    }

    private void closeDataBase(){
        signTable.close();
        courseTable.close();
    }

    private void insertSampleIntoDataBase(){
        signTable = new SignTable(getApplicationContext());
        // 如果資料庫是空的，就建立一些範例資料
        // 這是為了方便測試用的，完成應用程式以後可以拿掉
        if (signTable.getCount() == 0) {
            signTable.sample();
        }
    }

    private void setOnClickListeners(){
        dateEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    dateDialog.show();
                }else{
                    dateDialog.dismiss();
                }
            }
        });
        dateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!dateDialog.isShowing()) {
                    dateDialog.show();
                }
            }
        });

        timeEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    timeDialog.show();
                } else {
                    timeDialog.dismiss();
                }
            }
        });
        timeEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!timeDialog.isShowing()){
                    timeDialog.show();
                }
            }
        });

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//檢視簽到內容
                ArrayList<SignRecord> signRecords = signTable.get("" + course.getSN());
                String dialogMessage = "";
                for(int i=0;i<signRecords.size();i++){
                    Timestamp timestamp = new Timestamp(signRecords.get(i).getSignTime());
                    String timeString = timestamp.toString().substring(0,timestamp.toString().lastIndexOf(":"));

                    //debug
                    dialogMessage += signRecords.get(i).getSN() + " ";

                    dialogMessage += timeString + " ";
                    dialogMessage += signRecords.get(i).getName() + "  ";
                    dialogMessage += signRecords.get(i).getUnit() + "\n";
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage(dialogMessage);
                builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {//寄送簽到表
            @Override
            public void onClick(View v) {
                File excelFile;
                if (signTable.isExternalStorageWritable()){
                    excelFile = signTable.getExcelFilePath(course);
                }else{
                    Dialog dialog;
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("無法建立簽到表").setMessage("無法建立簽到表，請確定手機並未接上電腦，" +
                            "且尚有足夠外部儲存空間建立簽到表").setCancelable(false).setPositiveButton("", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog = builder.create();
                    dialog.show();
                    return;
                }

                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", course.getMailDes(), null));
                Timestamp timestamp = new Timestamp(course.getDateTime());
                String title = timestamp.toString() + " " + course.getName() + "簽到記錄";
                intent.putExtra(Intent.EXTRA_SUBJECT, title);
                intent.putExtra(Intent.EXTRA_TEXT, "如附件");
                Log.e("debug","excelFilePath:" + excelFile.getAbsolutePath());
                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + excelFile.getAbsolutePath()));

                startActivity(Intent.createChooser(intent, "Send Email"));
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<EditText> editTextList = new ArrayList<EditText>();
                editTextList.add((EditText) findViewById(R.id.name_editText));
                editTextList.add((EditText) findViewById(R.id.date_editText));
                editTextList.add((EditText) findViewById(R.id.time_editText));
                editTextList.add((EditText) findViewById(R.id.email_editText));

                for(int i=0;i<editTextList.size();i++){
                    if(editTextList.get(i).getText().toString() == ""){
                        Toast.makeText(v.getContext(), "欄位不可為空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                //check e-mail format
                if(!editTextList.get(3).getText().toString().matches(
                        "^[_a-z0-9-]+([.][_a-z0-9-]+)*@[a-z0-9-]+([.][a-z0-9-]+)*$")){
                    Toast.makeText(v.getContext(),"e-mail格式不正確",Toast.LENGTH_SHORT).show();
                    return;
                }

                //update course data in DB
                course.setName(nameEditText.getText().toString());
                course.setMailDes(mailDesEditText.getText().toString());
                course.setDateTime(courseDateTime.getTimeInMillis());
                new updateCourseTask(v.getContext(),course).execute();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent newAct = new Intent();
                newAct.setClass(CourseDetailActivity.this, SignActivity.class);
                Bundle bundle = new Bundle();
                bundle.putLong(CourseTable.SN_COLUMN, course.getSN());
                bundle.putString(CourseTable.NAME_COLUMN, course.getName());
                bundle.putLong(CourseTable.DATE_TIME_COLUMN, course.getDateTime());
                bundle.putString(CourseTable.MAILDES_COLUMN, course.getMailDes());

                newAct.putExtras(bundle);
                closeDataBase();
                startActivity( newAct );
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage("確定要刪除此課程?");
                builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new deleteCourseTask(CourseDetailActivity.this, course.getSN()).execute();
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }
        });
    }

    private class deleteCourseTask extends AsyncTask<Void,Void,Void> {

        Context context;
        Dialog dialog;
        long SN;

        public deleteCourseTask(Context context, long SN){
            this.context = context;
            this.SN = SN;
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("處理中");
            dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            //delete course in DB
            courseTable.delete(SN);
            return null;
        }

        @Override
        protected void onPostExecute(Void unused){
            dialog.dismiss();
            finish();
        }
    }

    private class updateCourseTask extends AsyncTask<Void,Void,Void> {

        Context context;
        Dialog dialog;
        Course course;

        public updateCourseTask(Context context, Course course){
            this.context = context;
            this.course = course;
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("處理中");
            dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            //update course in DB
            courseTable.update(course);
            return null;
        }

        @Override
        protected void onPostExecute(Void unused){
            MainActivity.adapter.notifyDataSetChanged();
            dialog.dismiss();
            finish();
        }
    }
}
