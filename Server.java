import java.io.*;
import java.net.*;

public class Server {

    public static void main(String[] args) {
        try{
            ServerSocket serverSocket = new ServerSocket(1234);
            Socket socket = serverSocket.accept();

            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            String msg = "";

            while (true) {
                msg = in.readUTF();
                System.out.println("Recieved: " + msg);

                if (msg.equals("HELO")) {
                    out.writeUTF("G'DAY");
                    out.flush();
                    System.out.println("Sent: G'DAY");
                } else if (msg.equals("BYE")) {
                    out.writeUTF("BYE");
                    out.flush();
                    System.out.println("Sent: BYE");
                    break;
                }
            }
            
            in.close();
            socket.close();
            serverSocket.close();
            
        } catch(Exception e) {
            System.out.println(e);
        }
    }

}