package Utilities;

public class JobInfo {
    public int receivedTime;
    public String id;
    public int estRunTime;
    public int reqCores;
    public int reqMem;
    public int reqDisk;

    public JobInfo(String time, String idIn, String runTime, String cores, String mem, String disk) {
        receivedTime = Integer.valueOf(time);
        id = idIn;
        estRunTime = Integer.valueOf(runTime);
        reqCores = Integer.valueOf(reqCores);
        reqMem = Integer.valueOf(mem);
        reqDisk = Integer.valueOf(disk);
    }
}
