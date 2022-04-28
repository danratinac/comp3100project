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

    public static void main(String[] args) {
        try {
            System.out.println(args);

            // open the socket used to connect to the server
            Socket socket = new Socket("localhost", DEFAULT_PORT);

            // initialise for input and output
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // send HELO to server
            out.write(("HELO\n").getBytes());
            out.flush();
            System.out.println("Sent: HELO");

            // wait for OK from server
            waitFor("OK", in);

            // send AUTH and authentication info to server
            out.write(("AUTH" + " " + System.getProperty("user.name") + "\n").getBytes());
            out.flush();
            System.out.println("Sent: AUTH");

            // wait for OK from server
            waitFor("OK", in);

            // send REDY when ready to start reading jobs
            out.write(("REDY\n").getBytes());
            out.flush();
            System.out.println("Sent: REDY");

            // read first job
            String msg = "";
            msg = in.readLine();
            System.out.println("Recieved: " + msg);
            currentJob = extractJobInfo(msg);

            // request server info
            out.write(("GETS All\n").getBytes());
            out.flush();
            System.out.println("Sent: GETS All");

            // get reply and initialise info array
            msg = in.readLine();
            System.out.println("Recieved: " + msg);
            String[] info = msg.split(" ");
            servers = new ServerInfo[Integer.valueOf(info[1])];

            // send OK
            out.write(("OK\n").getBytes());
            out.flush();
            System.out.println("Sent: OK");

            // store server info
            for (int i = 0; i < servers.length; i++) {
                msg = in.readLine();
                info = msg.split(" ");
                servers[i] = new ServerInfo(info[0], info[1], info[4], info[5], info[6], info[7], info[8]);
            }

            // print server info
            for (ServerInfo server : servers) {
                System.out.println(server.toString());
            }
            System.out.println();

            // send OK
            out.write(("OK\n").getBytes());
            out.flush();
            System.out.println("Sent: OK");

            // get list of largest servers for use in scheduling
            largestServers = getLargestServers();

            // schedule first job
            switch (args[0]) {
                case "fc":
                    scheduleJobFc(in, out);
                    break;
                default:
                    scheduleJobLrr(out);
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
                out.write(("REDY\n").getBytes());
                out.flush();
                System.out.println("Sent: REDY");

                // get server's reply
                msg = in.readLine();
                System.out.println("Recieved: " + msg);

                // set command so we can check how to handle reply
                command = msg.substring(0, 4);

                // perform appropriate action based on server reply
                switch (command) {
                    case "JOBN":
                        // schedule the job
                        currentJob = extractJobInfo(msg);

                        switch (args[0]) {
                            case "fc":
                                scheduleJobFc(in, out);
                                break;
                            default:
                                scheduleJobLrr(out);
                                break;
                        }

                        waitFor("OK", in);
                        break;
                    case "NONE":
                        // there are no more jobs so stop the loop
                        moreJobs = false;
                        break;
                    default:
                        break;
                }
            }

            // quit
            out.write(("QUIT\n").getBytes());
            out.flush();
            System.out.println("Sent: QUIT");

            msg = in.readLine();
            System.out.println("Recieved: " + msg);

            out.close();
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    // waits until the specified message is received
    private static void waitFor(String msg, BufferedReader in) {
        String input = "";
        try {
            while (!input.equals(msg)) {
                input = in.readLine();
            }
            System.out.println("Recieved: " + input);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // extracts job info in useable format from a ds-server JOBN message
    private static JobInfo extractJobInfo(String msg) {
        String[] info = msg.split(" ");
        JobInfo job = new JobInfo(info[1], info[2], info[3], info[4], info[5], info[6]);
        return job;
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

    // schedules jobs according a largest round robin algorithm
    private static void scheduleJobLrr(DataOutputStream out) {
        try {
            // send scheduling request
            String msg = "SCHD " + currentJob.id + " " + largestServers.get(currentServer).type + " "
                    + largestServers.get(currentServer).id;
            out.write((msg + "\n").getBytes());
            out.flush();

            // move to next server for lrr scheduling
            if (currentServer == largestServers.size() - 1)
                currentServer = 0;
            else
                currentServer++;

            System.out.println("Sent: " + msg);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static void scheduleJobFc(BufferedReader in, DataOutputStream out) {
        try {
            // get first capable server
            String msg = "GETS Capable " + currentJob.reqCores + " " + currentJob.reqMem + " "
                    + currentJob.reqDisk;
            out.write((msg + "\n").getBytes());
            out.flush();

            System.out.println("Sent: " + msg);

            // get data and discard, we don't need any of the info it contains
            System.out.println("Recieved: " + in.readLine());
            out.write(("OK\n").getBytes());
            out.flush();
            System.out.println("Sent: OK");

            // get server info
            String[] capableInfo = in.readLine().split(" ");
            System.out.println("First capable: " + capableInfo[1] + capableInfo[2]);

            // send OK
            out.write(("OK\n").getBytes());
            out.flush();
            System.out.println("Sent: OK");

            // send scheduling request
            msg = "SCHD " + currentJob.id + " " + capableInfo[1] + " " + capableInfo[2];
            out.write((msg + "\n").getBytes());
            out.flush();

            System.out.println("Sent: " + msg);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}