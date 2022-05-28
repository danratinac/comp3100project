/**
 * Author: Daniel Ratinac
 * Last updated: 26/5/2022
 * Description: a utility class used to hold data that describes servers
 */

package Utilities;

import java.util.ArrayList;

public class ServerInfo {
    public String type; // server type
    public String id; // server id
    public int cores; // number of cores
    public int memory; // amount of memory
    public int disk; // disk space
    public int jobs; // number of jobs either waiting or running
    public ArrayList<Integer> estCompletionTimes = new ArrayList<Integer>(); // list of estimated completion times for
                                                                             // current job followed by scheduled jobs

    public ServerInfo(String typeIn, String idIn, String coresIn, String mem, String diskIn) {
        type = typeIn;
        id = idIn;
        cores = Integer.valueOf(coresIn);
        memory = Integer.valueOf(mem);
        disk = Integer.valueOf(diskIn);
    }

    public String toString() {
        return type + " " + id + " " + String.valueOf(cores) + " " + String.valueOf(memory) + " " + String.valueOf(disk)
                + " " + String.valueOf(jobs);
    }
}
