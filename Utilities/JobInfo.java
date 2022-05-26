/**
 * Author: Daniel Ratinac
 * Last updated: 26/5/2022
 * Description: a utility class used to hold data that describes jobs
 */

package Utilities;

public class JobInfo {
    public int receivedTime; // time the job was received
    public String id; // job id
    public int estRunTime; // estimated runtime of job
    public int reqCores; // number of cores required by the job
    public int reqMem; // amount of memory required by the job
    public int reqDisk; // amount of disk space required by the job

    public JobInfo(String time, String idIn, String runTime, String cores, String mem, String disk) {
        receivedTime = Integer.valueOf(time);
        id = idIn;
        estRunTime = Integer.valueOf(runTime);
        reqCores = Integer.valueOf(cores);
        reqMem = Integer.valueOf(mem);
        reqDisk = Integer.valueOf(disk);
    }
}
