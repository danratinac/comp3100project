import java.io.*;
import java.net.*;

public class Client {

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

            // get server info

            // send REDY when ready to start reading jobs
            out.write(("REDY\n").getBytes());
            out.flush();
            System.out.println("Sent: REDY");

            String msg = "";

            while (true) {
                msg = in.readLine();
                System.out.println("Recieved: " + msg);
                break;
            }

            out.close();
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    private static void waitFor(String msg, BufferedReader in) {
        try {
            String input = in.readLine();
            while (!input.equals(msg)) {
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}