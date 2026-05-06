package tp1;

import java.io.*;
import java.net.Socket;

public class ReplaySimulator {

    public static void main(String[] args) {
        String host = "100.104.161.142";
        int port = 6000;

        // On rejoue exactement le même message que le client légitime
        String replayedCommand = "PAY:alice:100";

        try (
            Socket socket = new Socket(host, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            System.out.println("ReplaySimulator renvoie : " + replayedCommand);
            out.println(replayedCommand);

            String response = in.readLine();
            System.out.println("Réponse serveur : " + response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}