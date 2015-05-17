package tw.edu.ncu.cc.ncunfc.dummy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.sql.Timestamp;
import java.util.ArrayList;

import tw.edu.ncu.cc.ncunfc.R;
import tw.edu.ncu.cc.ncunfc.dummy.obj.Course;

public class CustomAdapter extends BaseAdapter {

    private LayoutInflater myInflater;
    private Context context;
    private ArrayList<Course> courseList = new ArrayList<Course>();

    public CustomAdapter(Context context, ArrayList courseList){
        this.context = context;
        this.courseList = courseList;
        myInflater = LayoutInflater.from(context);
    };

    public void setArray(ArrayList<Course> courseList){
        this.courseList = new ArrayList<Course>(courseList);
    }

    @Override
    public int getCount() {
        return courseList.size();
    }

    @Override
    public Object getItem(int position) {
        return courseList.get(position);
    }

    public Course getCourse(int position) {
        return courseList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //自訂類別，表達個別listItem中的view物件集合。
        ViewTag viewTag;

        if(convertView == null){
            //取得listItem容器 view
            convertView = LayoutInflater.from(context).inflate(R.layout.course_list_adapter_item, null);

            //建構listItem內容view
            viewTag = new ViewTag(
                    (TextView)convertView.findViewById(
                            R.id.dateTextView),
                    (TextView) convertView.findViewById(
                            R.id.nameTextView)
            );

            //設置容器內容
            convertView.setTag(viewTag);
        }
        else{
            viewTag = (ViewTag) convertView.getTag();
        }

        //設定內容文字
        String timeString = new Timestamp(courseList.get(position).getDateTime()).toString();
        timeString = timeString.substring(0,timeString.lastIndexOf(":"));
        viewTag.dateTextView.setText(timeString);
        viewTag.nameTextView.setText(courseList.get(position).getName());

        return convertView;
    }

    //自訂類別，表達個別listItem中的view物件集合。
    class ViewTag{
        TextView dateTextView;
        TextView nameTextView;

        public ViewTag(TextView dateTextView, TextView nameTextView){
            this.dateTextView = dateTextView;
            this.nameTextView = nameTextView;
        }
    }
}
