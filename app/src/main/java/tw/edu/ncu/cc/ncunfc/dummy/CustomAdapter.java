package tw.edu.ncu.cc.ncunfc.dummy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import tw.edu.ncu.cc.ncunfc.R;
import tw.edu.ncu.cc.ncunfc.dummy.obj.Course;
import tw.edu.ncu.cc.ncunfc.dummy.sqlLite.CourseTable;

public class CustomAdapter extends BaseAdapter {
    public static final int SORT_BY_NAME = 0;
    public static final int SORT_BY_DATE = 1;
    public static final int SORT_BY_CREATE_DATE = 2;

    private Context context;
    private ArrayList<Course> courseList = new ArrayList<Course>();

    public CustomAdapter(Context context, ArrayList courseList){
        this.context = context;
        this.courseList = courseList;
    };

    public void setArray(ArrayList<Course> courseList){
        this.courseList = new ArrayList<Course>(courseList);
    }

    public void refresh(int sortMode){
        CourseTable courseTable = new CourseTable(context);
        this.courseList = courseTable.getAll();
        courseTable.close();
        if(sortMode != SORT_BY_CREATE_DATE){
            Collections.sort(courseList, new CustomComparator(sortMode));
        }
        this.notifyDataSetChanged();
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
    private class ViewTag{
        TextView dateTextView;
        TextView nameTextView;

        public ViewTag(TextView dateTextView, TextView nameTextView){
            this.dateTextView = dateTextView;
            this.nameTextView = nameTextView;
        }
    }

    private class CustomComparator implements Comparator<Course> {
        private int sortMode = CustomAdapter.SORT_BY_DATE;

        public CustomComparator(int sortMode){
            this.sortMode = sortMode;
        }

        @Override
        public int compare(Course lhs, Course rhs) {
            int retVal = 0;

            switch (sortMode){
                case SORT_BY_DATE:
                    Long lhsDateTime = lhs.getDateTime();
                    retVal = lhsDateTime.compareTo(rhs.getDateTime());
                    break;
                case SORT_BY_NAME:
                    retVal = lhs.getName().compareTo(rhs.getName());
                    break;
            }

            return retVal;
        }
    }
}
