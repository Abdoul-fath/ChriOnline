package tp1;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class attaquebrute{

    private static final Logger logger = LogManager.getLogger(attaquebrute.class);
    private static final int PORT = 7016;

    // Base utilisateurs
    private static final Map<String, String> users = new ConcurrentHashMap<>();
    // Compteur tentatives
    private static final Map<String, Integer> attempts = new ConcurrentHashMap<>();
    // Limite max de tentatives
    private static final int MAX_ATTEMPTS = 3;

    public static void main(String[] args) throws IOException {
        users.put("admin", "admin123");
        

        ServerSocket serverSocket = new ServerSocket(PORT);
        logger.info("Serveur démarré sur le port " + PORT + "...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new ClientHandler(clientSocket).start();
        }
    }

    // Classe interne pour gérer chaque client
    static class ClientHandler extends Thread {
        private Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            String ipClient = socket.getInetAddress().getHostAddress();
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream out = new DataOutputStream(socket.getOutputStream())
            ) {
                logger.info("Nouvelle connexion depuis : " + ipClient);

                // LOGIN
                out.writeBytes("LOGIN:\n");
                String login = in.readLine();

                // PASSWORD
                out.writeBytes("PASSWORD:\n");
                String password = in.readLine();

                // Vérifier blocage
                if (attempts.getOrDefault(login, 0) >= MAX_ATTEMPTS) {
                    logger.warn("Compte bloqué : " + login + " depuis " + ipClient);
                    out.writeBytes("ACCOUNT_BLOCKED\n");
                    socket.close();
                    return;
                }

                // Authentification
                if (users.containsKey(login) && users.get(login).equals(password)) {
                    logger.info("Connexion réussie : " + login + " depuis " + ipClient);
                    out.writeBytes("AUTH_SUCCESS\n");
                    attempts.put(login, 0); // reset compteur après succès
                } else {
                    int count = attempts.getOrDefault(login, 0) + 1;
                    attempts.put(login, count);
                    logger.error("Echec login (" + count + "/" + MAX_ATTEMPTS + ") pour utilisateur : "
                            + login + " depuis " + ipClient);
                    out.writeBytes("AUTH_FAILED (" + count + "/" + MAX_ATTEMPTS + ")\n");
                }

                socket.close();
            } catch (IOException e) {
                logger.error("Erreur avec le client " + ipClient + " : " + e.getMessage());
            }
        }
    }
}