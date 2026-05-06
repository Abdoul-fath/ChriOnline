package tp1;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class VulnerableServer {

    public static void main(String[] args) {
        int port = 6000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur vulnérable lancé sur le port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String message = in.readLine();

                System.out.println("Message reçu : " + message);

                // Traitement très simple d'une commande PAY:user:amount
                if (message != null && message.startsWith("PAY:")) {
                    String[] parts = message.split(":");

                    if (parts.length == 3) {
                        String user = parts[1];
                        String amount = parts[2];

                        System.out.println("Paiement accepté pour " + user + " de " + amount + " MAD");
                        out.println("OK: paiement effectué");
                    } else {
                        out.println("ERREUR: format invalide");
                    }
                } else {
                    out.println("ERREUR: commande inconnue");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}