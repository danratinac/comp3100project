
/**
 * Author: Daniel Ratinac
 * Last updated: 26/5/2022
 * Description: the main client class responsible for all scheduling-related 
 * functionality
 */

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import Utilities.JobInfo;
import Utilities.ServerInfo;

public class Client {

    private static JobInfo currentJob; // stores info on the current job that requires scheduling
    private static int currentServer = 0; // stores the index of the server that should receive the next job scheduled

    private static ServerInfo[] servers; // stores the hardware information of all the servers as sent by ds-server
    private static List<ServerInfo> largestServers = new ArrayList<ServerInfo>(); // a list of all servers of the
                                                                                  // largest type

    private static final int DEFAULT_PORT = 50000;
    private static final int MAX_RUNTIME = 1000;

    public static void main(String[] args) {
        try {
            // set args to avoid null issues with later switch statements
            if (args.length == 0) {
                args = new String[1];
                args[0] = "blank";
            }

            // open the socket used to connect to the server
            Socket socket = new Socket("localhost", DEFAULT_PORT);

            // initialise for input and output
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // send HELO to server
            sendMessage("HELO", out);

            // wait for OK from server
            waitFor("OK", in);

            // send AUTH and authentication info to server
            sendMessage("AUTH" + " " + System.getProperty("user.name"), out);

            // wait for OK from server
            waitFor("OK", in);

            // send REDY when ready to start reading jobs
            sendMessage("REDY", out);

            // read first job
            String msg = receiveMessage(in);
            currentJob = extractJobInfo(msg);

            // initialise server info array
            servers = getServersData(in, out, "all");

            // space output
            System.out.println();

            // get list of largest servers for use in scheduling
            largestServers = getLargestServers();

            // schedule first job
            switch (args[0]) {
                case "-fc":
                    scheduleJobFc(in, out);
                    break;
                case "-lrr":
                    scheduleJobLrr(out);
                    break;
                case "-lb":
                    scheduleToLeastBusy(in, out);
                    break;
                case "-fcl":
                    scheduleJobFcLimited(in, out);
                    break;
                case "-sw":
                    scheduleJobShortestWait(in, out);
                    break;
                default:
                    scheduleJobCustom(in, out);
                    break;
            }

            // wait for OK
            waitFor("OK", in);

            // used for switching based on what the server's reply to REDY is
            String command;

            // used to break out of loop if no more jobs remain
            Boolean moreJobs = true;

            // main loop, handles all messages from the server from now on
            while (moreJobs) {
                // space console output for readability
                System.out.println();

                // send REDY for next info
                sendMessage("REDY", out);

                // get server's reply
                msg = receiveMessage(in);

                // set command so we can check how to handle reply
                command = msg.substring(0, 4);

                // perform appropriate action based on server reply
                switch (command) {
                    case "JOBN":
                        // schedule the job
                        currentJob = extractJobInfo(msg);

                        switch (args[0]) {
                            case "-fc":
                                scheduleJobFc(in, out);
                                break;
                            case "-lrr":
                                scheduleJobLrr(out);
                            case "-lb":
                                scheduleToLeastBusy(in, out);
                                break;
                            case "-fcl":
                                scheduleJobFcLimited(in, out);
                                break;
                            case "-sw":
                                scheduleJobShortestWait(in, out);
                                break;
                            default:
                                scheduleJobCustom(in, out);
                                break;
                        }

                        waitFor("OK", in);
                        break;
                    case "NONE":
                        // there are no more jobs so stop the loop
                        moreJobs = false;
                        break;
                    case "JCPL":
                        String[] split = msg.split(" ");
                        switch (args[0]) {
                            case "-lb":
                                // decrement jobs for appropriate server
                                servers[getServerIndexByInfo(split[3], split[4])].jobs--;
                                break;
                            case "-sw":
                                servers[getServerIndexByInfo(split[3], split[4])].estCompletionTimes.remove(0);
                                break;
                            default:
                                break;
                        }
                    default:
                        break;
                }
            }

            // quit
            sendMessage("QUIT", out);

            receiveMessage(in);

            out.close();
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /////////// SCHEDULING METHODS ////////////

    // schedules jobs according to a largest round robin algorithm
    private static void scheduleJobLrr(DataOutputStream out) {
        try {
            // send scheduling request
            sendMessage("SCHD " + currentJob.id + " " + largestServers.get(currentServer).type + " "
                    + largestServers.get(currentServer).id, out);

            // move to next server for lrr scheduling
            if (currentServer == largestServers.size() - 1)
                currentServer = 0;
            else
                currentServer++;

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // schedules jobs according to a first capable algorithm
    private static void scheduleJobFc(BufferedReader in, DataOutputStream out) {
        try {
            // get first capable server
            ServerInfo capableInfo = getServersData(in, out, "capable")[0];

            // send scheduling request
            sendMessage("SCHD " + currentJob.id + " " + capableInfo.type + " " + capableInfo.id, out);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // schedules jobs according to a custom algorithm
    private static void scheduleJobCustom(BufferedReader in, DataOutputStream out) {
        try {
            String rply;

            // check for servers with necessary resources currently available
            ServerInfo[] availServers = getServersData(in, out, "available");

            // if there are servers with the required resources available, schedule to the
            // first one
            if (availServers != null) {
                // send scheduling request
                sendMessage("SCHD " + currentJob.id + " " + availServers[0].type + " " + availServers[0].id, out);
            } else { // otherwise fall back to the servers that can eventually provide the required
                     // resources
                // get capable servers
                ServerInfo[] capServers = getServersData(in, out, "capable");

                // find the first capable server with an estimated runtime under the threshold
                int index = 0;
                int currentEstRuntime = 0;
                do {
                    // get total estimate runtime for server
                    sendMessage("EJWT " + capServers[index].type + " " + capServers[index].id, out);

                    rply = receiveMessage(in);

                    currentEstRuntime = Integer.valueOf(rply);

                    index++;
                } while (currentEstRuntime > MAX_RUNTIME && index < capServers.length);

                // decrement index by one as it is incremented regardless of whether loop will
                // continue
                index--;

                // send scheduling request
                sendMessage("SCHD " + currentJob.id + " " + capServers[index].type + " " + capServers[index].id, out);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // schedule jobs to the capable server with the least total jobs (i.e. waiting
    // jobs + running jobs)
    private static void scheduleToLeastBusy(BufferedReader in, DataOutputStream out) {
        try {
            int currentJobs = 1;
            int minJobs = 10000;
            int scheduleTo = 0;
            int i = 0;

            // find the capable server with the least jobs based on local data
            do {
                // check that server is capable of running job
                if (servers[i].cores >= currentJob.reqCores && servers[i].memory >= currentJob.reqMem
                        && servers[i].disk >= currentJob.reqDisk) {
                    currentJobs = servers[i].jobs;
                    // if server has less jobs waiting than current min, make it min
                    if (currentJobs < minJobs) {
                        minJobs = currentJobs;
                        scheduleTo = i;
                    }
                }
                i++;
            } while (minJobs > 0 && i < servers.length);

            // send scheduling request
            sendMessage("SCHD " + currentJob.id + " " + servers[scheduleTo].type + " " + servers[scheduleTo].id, out);

            // increment jobs for server that recieved jobs
            servers[scheduleTo].jobs++;

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static void scheduleJobFcLimited(BufferedReader in, DataOutputStream out) {
        String rply;

        // get capable servers
        ServerInfo[] capServers = getServersData(in, out, "capable");

        // find the first capable server with an estimated runtime under the threshold
        int index = 0;
        int currentEstRuntime = 0;
        do {
            // get total estimate runtime for server
            sendMessage("EJWT " + capServers[index].type + " " + capServers[index].id, out);

            rply = receiveMessage(in);

            currentEstRuntime = Integer.valueOf(rply);

            index++;
        } while (currentEstRuntime > MAX_RUNTIME && index < capServers.length);

        // decrement index by one as it is incremented regardless of whether loop will
        // continue
        index--;

        // send scheduling request
        sendMessage("SCHD " + currentJob.id + " " + capServers[index].type + " " + capServers[index].id, out);
    }

    private static void scheduleJobShortestWait(BufferedReader in, DataOutputStream out) {
        try {
            // find the first capable server with an estimated runtime under the threshold
            int index = 0;
            int currentWait = 0;
            int minWait = 10000000;
            int scheduleTo = 0;

            Integer holder;

            do {
                holder = servers[index].estCompletionTimes.get(0);
                currentWait = holder == null ? 0 : holder;
                if (currentWait < minWait) {
                    minWait = currentWait;
                    scheduleTo = index;
                }
                index++;
            } while (minWait > 0 && index < servers.length);

            // decrement index by one as it is incremented regardless of whether loop will
            // continue
            index--;

            // send scheduling request
            sendMessage("SCHD " + currentJob.id + " " + servers[scheduleTo].type + " " + servers[scheduleTo].id, out);
            servers[getServerIndexByInfo(servers[scheduleTo].type, servers[scheduleTo].id)].estCompletionTimes
                    .add(currentJob.estRunTime);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /////////// UTILITY METHODS ////////////

    // waits until the specified message is received
    private static void waitFor(String msg, BufferedReader in) {
        String input = "";
        try {
            while (!input.equals(msg)) {
                input = in.readLine();
            }
            System.out.println("Received: " + input);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // send a message to the server
    private static void sendMessage(String msg, DataOutputStream out) {
        try {
            out.write((msg + "\n").getBytes());
            out.flush();
            // output to console
            System.out.println("Sent: " + msg);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // receive message
    private static String receiveMessage(BufferedReader in) {
        try {
            String rply = in.readLine();
            // output to console
            System.out.println("Received: " + rply);

            return rply;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    // extracts job info in useable format from a ds-server JOBN message
    private static JobInfo extractJobInfo(String msg) {
        String[] info = msg.split(" ");
        JobInfo job = new JobInfo(info[1], info[2], info[3], info[4], info[5], info[6]);
        return job;
    }

    // extracts server info in useable format from a ds-server GETS record
    private static ServerInfo extractServerInfo(String msg) {
        String[] info = msg.split(" ");
        ServerInfo server = new ServerInfo(info[0], info[1], info[4], info[5], info[6]);
        return server;
    }

    // get the position of a server within the local servers array based on server
    // type and id
    private static int getServerIndexByInfo(String type, String id) {
        for (int i = 0; i < servers.length; i++) {
            if (servers[i].type.equals(type) && servers[i].id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    // returns a list of all servers that are of the type with most CPU cores
    private static ArrayList<ServerInfo> getLargestServers() {
        int largestIndex = 0;
        // determine which server type has the most CPU cores; if more than one, take
        // the first
        for (int i = 0; i < servers.length; i++) {
            if (servers[i].cores > servers[largestIndex].cores)
                largestIndex = i;
        }
        // add all servers of the largest type to the largestType list
        String largestType = servers[largestIndex].type;
        ArrayList<ServerInfo> largest = new ArrayList<ServerInfo>();
        for (ServerInfo server : servers) {
            if (server.type.equals(largestType))
                largest.add(server);
        }
        return largest;
    }

    private static ServerInfo[] getServersData(BufferedReader in, DataOutputStream out, String mode) {
        String msg;
        String rply;

        // send the appropriate request based on the mode
        switch (mode) {
            case "all":
                sendMessage("GETS All", out);
                break;
            case "available":
                sendMessage("GETS Avail " + currentJob.reqCores + " " + currentJob.reqMem + " "
                        + currentJob.reqDisk, out);
                break;
            case "capable":
                sendMessage("GETS Capable " + currentJob.reqCores + " " + currentJob.reqMem + " "
                        + currentJob.reqDisk, out);
                break;
            default:
                return null;
        }

        // get data
        rply = receiveMessage(in);

        // send OK
        sendMessage("OK", out);

        // check that there is actually servers to return
        int numServers = Integer.valueOf(rply.split(" ")[1]);

        // return if there are no servers matching request
        if (numServers == 0) {
            receiveMessage(in);
            return null;
        }

        // intialise server array
        ServerInfo[] servers = new ServerInfo[numServers];

        // get server info
        for (int i = 0; i < servers.length; i++) {
            msg = receiveMessage(in);
            servers[i] = extractServerInfo(msg);
        }

        // send OK
        sendMessage("OK", out);

        // get and discard reply
        receiveMessage(in);

        return servers;
    }
}