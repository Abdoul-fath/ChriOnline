package test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestInvalidCommand {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 5000;

        try (
                Socket socket = new Socket(host, port);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String welcome = in.readLine();
            System.out.println("Serveur : " + welcome);

            String command = "HACK_TEST:abc";
            System.out.println("Commande envoyée : " + command);

            out.println(command);

            String response = in.readLine();
            System.out.println("Réponse serveur : " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}