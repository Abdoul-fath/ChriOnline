package tp1;

import java.io.*;
import java.net.Socket;

public class Clientnormal {

    public static void main(String[] args) {
        String host = "100.104.161.142";
        int port = 6000;

        String command = "PAY:alice:100";

        try (
            Socket socket = new Socket(host, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            System.out.println("Client envoie : " + command);
            out.println(command);

            String response = in.readLine();
            System.out.println("Réponse serveur : " + response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}