import java.io.*;
import java.net.*;

import Utilities.JobInfo;
import Utilities.ServerInfo;

public class Client {

    private static JobInfo currentJob;
    private static ServerInfo[] servers;

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
            for (ServerInfo server : servers) {
                msg = in.readLine();
                info = msg.split(" ");
                server = new ServerInfo(info[0], info[1], info[4], info[5], info[6], info[7], info[8]);
                System.out.println(server.toString());
            }

            // send OK
            out.write(("OK\n").getBytes());
            out.flush();
            System.out.println("Sent: OK");

            // schedule first job
            int largest = getLargestServer();
            out.write(("SCHD " + currentJob.id + " " + servers[largest].type + " " + servers[largest].id + "\n")
                    .getBytes());
            out.flush();

            // wait for OK
            waitFor("OK", in);

            // schedule rest of jobs
            while (true) {
                out.write(("REDY\n").getBytes());
                out.flush();
                msg = in.readLine();
                if (msg.equals("NONE"))
                    break;
                largest = getLargestServer();
                out.write(("SCHD " + currentJob.id + " " + servers[largest].type + " " + servers[largest].id + "\n")
                        .getBytes());
                out.flush();

                waitFor("OK", in);
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

    private static JobInfo getNextJob(BufferedReader in) {
        JobInfo info = null;
        try {
            String msg = in.readLine();
            extractJobInfo(msg);
        } catch (Exception e) {
            System.out.println(e);
        }
        return info;
    }

    private static int getLargestServer() {
        int largestIndex = 0;
        for (int i = 0; i < servers.length; i++) {
            if (servers[i].cores > servers[largestIndex].cores)
                largestIndex = i;
        }
        return largestIndex;
    }
}