package tw.edu.ncu.cc.ncunfc.dummy;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.api.client.testing.util.TestableByteArrayOutputStream;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import tw.edu.ncu.cc.ncunfc.R;
import tw.edu.ncu.cc.ncunfc.dummy.obj.Course;
import tw.edu.ncu.cc.ncunfc.dummy.obj.SignRecord;
import tw.edu.ncu.cc.ncunfc.dummy.sqlLite.CourseTable;
import tw.edu.ncu.cc.ncunfc.dummy.sqlLite.SignTable;


public class NewCourseActivity extends ActionBarActivity {

    //SqlLite
    CourseTable courseTable;

    //UI
    private Button addButton;
    private Button cancelButton;
    private EditText dateEditText;
    private EditText timeEditText;
    private DatePickerDialog dateDialog;
    private TimePickerDialog timeDialog;

    //Course Data to be added
    private Calendar courseDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_course);

        courseDateTime = Calendar.getInstance();

        initView();
        setListeners();
        initDataBase(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.menu_new_course, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        addButton = (Button) findViewById(R.id.add_button2);
        cancelButton = (Button) findViewById(R.id.cancel_button);
        dateEditText = (EditText) findViewById(R.id.date_editText);
        timeEditText = (EditText) findViewById(R.id.time_editText);

        dateDialog = new DatePickerDialog(NewCourseActivity.this, new DatePickerDialog.OnDateSetListener() {
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

        timeDialog = new TimePickerDialog(NewCourseActivity.this, new TimePickerDialog.OnTimeSetListener() {
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

    private void initDataBase(Context context) {
        this.courseTable = new CourseTable(context);
    }

    private void closeDataBase() {
        this.courseTable.close();
    }

    public void setListeners() {

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

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<EditText> editTextList = new ArrayList<EditText>();
                editTextList.add((EditText) findViewById(R.id.name_editText));
                editTextList.add((EditText) findViewById(R.id.email_editText));

                //之前版本是所有欄位都要填，現在改成只有課程名稱一定要填入
                /*for (int i = 0; i < editTextList.size(); i++) {
                    if (editTextList.get(i).getText().toString().equals("")) {
                        Toast.makeText(v.getContext(), "欄位不可為空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                //check e-mail format
                if (!editTextList.get(1).getText().toString().matches(
                        "^[_a-z0-9-]+([.][_a-z0-9-]+)*@[a-z0-9-]+([.][a-z0-9-]+)*$")) {
                    Toast.makeText(v.getContext(), "e-mail格式不正確", Toast.LENGTH_SHORT).show();
                    return;
                }*/
                if(editTextList.get(0).getText() == null){
                    Toast.makeText(v.getContext(), "課程欄位不可為空喔", Toast.LENGTH_SHORT).show();
                    return;
                }

                // call api to add new course
                Course course = new Course(-1,//SN AUTOINCREMENT
                        editTextList.get(0).getText().toString(),//Name
                        courseDateTime.getTimeInMillis(),//date
                        editTextList.get(1).getText().toString()//mailDes
                );
                new addCourseTask(v.getContext(), course).execute();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDataBase();
                finish();
            }
        });
    }

    private class addCourseTask extends AsyncTask<Void, Void, Void> {
        public static final int STATUS_SUCCESS = 0;
        public static final int STATUS_FAIL = 1;

        Context context;
        Dialog dialog;
        Course course;
        int status = 1;

        public addCourseTask(Context context, Course course) {
            this.context = context;
            this.course = course;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("新增中");
            dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            //add course to DB
            if (courseTable.insert(course) == -1) {
                Toast.makeText(context,"新增課程失敗", Toast.LENGTH_LONG);
                this.status = STATUS_FAIL;
            }else{
                this.status = STATUS_SUCCESS;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            dialog.dismiss();
            if(this.status == STATUS_SUCCESS){
                MainActivity.adapter.notifyDataSetChanged();
                closeDataBase();
                finish();
            }
        }
    }

    private void updateDateEditText(){
        /*
        String day = "" + courseDateTime.get(Calendar.DAY_OF_MONTH);
        String month = "" + (courseDateTime.get(Calendar.MONTH) + 1);
        String year = "" + courseDateTime.get(Calendar.YEAR);
        */

        /*
        courseDateTime.getTimeInMillis();
        SimpleDateFormat year_date = new SimpleDateFormat("yyyy", Locale.getDefault());
        String year = year_date.format(courseDateTime.getTime());

        SimpleDateFormat month_date = new SimpleDateFormat("MM", Locale.getDefault());
        String month = month_date.format(courseDateTime.getTime());

        SimpleDateFormat day_date = new SimpleDateFormat("FF", Locale.getDefault());
        String day = month_date.format(courseDateTime.getTime());

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("", Locale.getDefault());
        */
        Timestamp temp = new Timestamp(courseDateTime.getTimeInMillis());
        String[] tempString  = temp.toString().split(" ");

        dateEditText.setText(tempString[0]);
    }

    private void updateTimeEditText(){
        /*
        String hour = "" + courseDateTime.get(Calendar.HOUR_OF_DAY);
        String minute = "" + courseDateTime.get(Calendar.MINUTE);
        */
        /*
        courseDateTime.getTimeInMillis();
        SimpleDateFormat month_date = new SimpleDateFormat("HH", Locale.getDefault());
        String hour = month_date.format(courseDateTime.getTime());

        SimpleDateFormat day_date = new SimpleDateFormat("mm", Locale.getDefault());
        String minute = month_date.format(courseDateTime.getTime());
        */
        Timestamp temp = new Timestamp(courseDateTime.getTimeInMillis());
        String[] tempString  = temp.toString().split(" ");
        String timeString = tempString[1].substring(0,tempString[1].lastIndexOf(":"));

        timeEditText.setText(timeString);

    }
}

