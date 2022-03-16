import java.io.*;
import java.net.*;

public class Client {

    public static void main(String[] args) {
        try{
            Socket socket = new Socket("localhost", 1234);

            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            out.writeUTF("HELO");
            out.flush();
            System.out.println("Sent: HELO");

            String msg = "";
            
            while (true) {
                msg = in.readUTF();
                System.out.println("Recieved: " + msg);

                if (msg.equals("G'DAY")) {
                    out.writeUTF("BYE");
                    out.flush();
                    System.out.println("Sent: BYE");
                } else if (msg.equals("BYE")) {
                    break;
                }
            }

            out.close();
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
        }

    }
}