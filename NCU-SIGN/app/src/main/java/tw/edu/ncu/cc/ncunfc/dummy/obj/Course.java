package tw.edu.ncu.cc.ncunfc.dummy.obj;


public class Course {

    private long SN;
    private String name;
    private long dateTime;
    private String mailDes;

    public Course(){

    }

    public Course(long SN, String name, long dateTime, String mailDes){
        this.SN = new Long(SN);
        this.name = new String (name);
        this.dateTime = new Long(dateTime);
        this.mailDes =new String(mailDes);
    }

    public long getSN(){
        return SN;
    }

    public String getName(){
        return name;
    }

    public long getDateTime(){
        return dateTime;
    }

    public String getMailDes() {
        return mailDes;
    }

    public void setSN(long SN){
        this.SN = new Long(SN);
    }

    public void setName(String name){
        this.name = new String(name);
    }

    public void setDateTime(long dateTime){
        this.dateTime = new Long(dateTime);
    }

    public void setMailDes(String mailDes){
        this.mailDes = new String(mailDes);
    }
}
