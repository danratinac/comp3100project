package Utilities;

public class ServerInfo {
    public String type;
    public String id;
    public int cores;
    public int memory;
    public int disk;
    public int waitingJobs;
    public int runningJobs;

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
        return type + id + String.valueOf(cores) + String.valueOf(memory) + String.valueOf(disk)
                + String.valueOf(waitingJobs) + String.valueOf(runningJobs);
    }
}
