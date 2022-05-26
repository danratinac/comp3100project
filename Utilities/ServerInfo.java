/**
 * Author: Daniel Ratinac
 * Last updated: 26/5/2022
 * Description: a utility class used to hold data that describes servers
 */

package Utilities;

public class ServerInfo {
    public String type; // server type
    public String id; // server id
    public int cores; // number of cores
    public int memory; // amount of memory
    public int disk; // disk space
    public int waitingJobs; // number of waiting jobs (unused)
    public int runningJobs; // number of running jobs (unused)

    public ServerInfo(String typeIn, String idIn, String coresIn, String mem, String diskIn, String jobsWaiting,
            String jobsRunning) {
        type = typeIn;
        id = idIn;
        cores = Integer.valueOf(coresIn);
        memory = Integer.valueOf(mem);
        disk = Integer.valueOf(diskIn);
        waitingJobs = Integer.valueOf(jobsWaiting);
        runningJobs = Integer.valueOf(jobsRunning);
    }

    public String toString() {
        return type + " " + id + " " + String.valueOf(cores) + " " + String.valueOf(memory) + " " + String.valueOf(disk)
            + " " + String.valueOf(waitingJobs) + " " + String.valueOf(runningJobs);
    }
}
