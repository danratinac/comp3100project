import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import Utilities.JobInfo;
import Utilities.ServerInfo;

public class Client {

    private static JobInfo currentJob;
    private static ServerInfo[] servers;
    private static List<ServerInfo> largestServers = new ArrayList<ServerInfo>();
    private static int currentServer = 0;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 50000);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // send HELO to server
            out.write(("HELO\n").getBytes());
            out.flush();
            System.out.println("Sent: HELO");

            // wait for OK from server
            waitFor("OK", in);

            // send AUTH and authentication info to server
            out.write(("AUTH" + System.getProperty("user.name") + "\n").getBytes());
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

            // save server info
            for (int i = 0; i < servers.length; i++) {
                msg = in.readLine();
                info = msg.split(" ");
                servers[i] = new ServerInfo(info[0], info[1], info[4], info[5], info[6], info[7], info[8]);
            }

            // print server info
            for (ServerInfo server : servers) {
                System.out.println(server.toString());
            }

            // send OK
            out.write(("OK\n").getBytes());
            out.flush();
            System.out.println("Sent: OK");

            // get list of largest servers for use in scheduling
            largestServers = getLargestServers();

            // schedule first job
            scheduleJob(out);

            // wait for OK
            waitFor("OK", in);

            // used for switching based on what the server's reply to REDY is
            String command;

            // used to break out of loop if no more jobs remain
            Boolean moreJobs = true;

            // schedule rest of jobs
            while (moreJobs) {
                // send REDY for next info
                out.write(("REDY\n").getBytes());
                out.flush();

                // get server's reply
                msg = in.readLine();
                System.out.println("Recieved: " + msg);

                // set command so we can check how to handle reply
                command = msg.substring(0, 4);
                System.out.println("Command: " + command);

                // perform appropriate action based on server reply
                switch (command) {
                    case "JOBN":
                        currentJob = extractJobInfo(msg);
                        scheduleJob(out);
        
                        waitFor("OK", in);
                        break;
                    case "NONE":
                        moreJobs = false;
                        break;
                    default:
                        break;
                }
            }

            // quit
            out.write(("QUIT\n").getBytes());
            out.flush();

            msg = in.readLine();
            System.out.println("Recieved: " + msg);

            out.close();
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    private static void waitFor(String msg, BufferedReader in) {
        String input = "";
        try {
            while (true) {
                if (!input.equals(msg))
                    input = in.readLine();
                else
                    break;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static JobInfo extractJobInfo(String msg) {
        String[] info = msg.split(" ");
        JobInfo job = new JobInfo(info[1], info[2], info[3], info[4], info[5], info[6]);
        return job;
    }

    private static ArrayList<ServerInfo> getLargestServers() {
        int largestIndex = 0;
        for(int i = 0; i < servers.length; i++) {
            if (servers[i].cores > servers[largestIndex].cores)
                largestIndex = i;
        }
        String largestType = servers[largestIndex].type;
        ArrayList<ServerInfo> largest = new ArrayList<ServerInfo>();
        for(ServerInfo server : servers) {
            if(server.type.equals(largestType)) largest.add(server);
        }
        return largest;
    }

    private static void scheduleJob(DataOutputStream out) {
        try {
            // send scheduling request
            String msg = "SCHD " + currentJob.id + " " + largestServers.get(currentServer).type + " " + largestServers.get(currentServer).id;
            out.write((msg + "\n").getBytes());
            out.flush();

            // move to next server for lrr scheduling
            if(currentServer == largestServers.size() - 1) currentServer = 0;
            else currentServer++;

            System.out.println("Sent: " + msg);
        } catch(Exception e) {
            System.out.println(e);
        }
    }
}