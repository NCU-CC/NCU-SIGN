package tw.edu.ncu.cc.ncunfc.dummy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import tw.edu.ncu.cc.ncunfc.R;
import tw.edu.ncu.cc.ncunfc.dummy.obj.Course;


public class CourseListActivity extends ActionBarActivity {

    private ListView courseListView;
    private CustomAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_list);

        courseListView = (ListView) findViewById(R.id.listView);
        adapter = new CustomAdapter(this, new ArrayList());
        //設定ListView未取得內容時顯示的view, empty建構在list.xml中。
        //courseListView.setEmptyView(findViewById(R.id.empty));
        courseListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent newAct = new Intent();
                newAct.setClass( CourseListActivity.this, CourseDetailActivity.class );
                Bundle bundle = new Bundle();
                bundle.putInt("POSITION", position);
                newAct.putExtras(bundle);
                startActivity( newAct );
            }
        });

        courseListView.setAdapter(adapter);

        //asynctask 呼叫 api get course
        new getCourseTask(this, adapter).execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_course_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add) {
            //go to NewCourseActivity
            Intent newAct = new Intent();
            newAct.setClass( CourseListActivity.this, NewCourseActivity.class );
            startActivity( newAct );
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            builder.setMessage("資料載入中");
            dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            //call api get course list and save into Data.courseList
            /*
            for(int i=0;i<200;i++){
                Data.courseList.add(new Course(String.valueOf(i), "name"+i, "owner"+i, "date"+i, "time"+i, "mailDes"+i,"createDate"+i,"sendDate"+i));
            }*/
            return null;
        }

        @Override
        protected void onPostExecute(Void unused){
            dialog.dismiss();
            adapter.notifyDataSetChanged();
        }
    }
}
