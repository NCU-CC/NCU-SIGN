package tw.edu.ncu.cc.ncunfc.dummy.obj;

/**
 * Created by andre.hu on 2015/4/14.
 */
public class SignRecord {
    private static String SN;
    private static long signTime;
    private static String unit;
    private static String name;

    public SignRecord(){

    }

    public SignRecord(String SN, long signTime, String unit, String name){
        this.SN = new String(SN);
        this.signTime = new Long(signTime);
        this.unit = new String(unit);
        this.name = new String(name);
    }

    public void setSN(String SN){
        this.SN = new String(SN);
    }

    public void setTime(Long signTime){
        this.signTime = new Long(signTime);
    }

    public void setUnit(String unit){
        this.unit = new String(unit);
    }

    public void setName(String name){
        this.name = new String(name);
    }

    public String getSN(){
        return this.SN;
    }

    public long getSignTime(){
        return this.signTime;
    }

    public String getUnit(){
        return this.unit;
    }

    public String getName(){
        return this.name;
    }
}
