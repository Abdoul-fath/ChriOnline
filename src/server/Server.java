package server;

import security.ConnectionLimiter;
import util.AppLogger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int PORT = 5000;

    // TP3 SYN FLOOD PROTECTION
    private static final ConnectionLimiter connectionLimiter = new ConnectionLimiter();

    public static void main(String[] args) {

        AppLogger.SERVER.info("========================================");
        AppLogger.SERVER.info("Démarrage du serveur ChriOnline...");
        AppLogger.SERVER.info("Protection TP1/TP2/TP3/TP4 activée");
        AppLogger.SERVER.info("========================================");

        // =========================================================
        // TP4 UDP SECURITY SERVER
        // =========================================================
        UdpSecurityServer udpServer = new UdpSecurityServer();
        udpServer.start();

        // =========================================================
        // TCP SERVER
        // =========================================================
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            AppLogger.SERVER.info("Serveur TCP lancé sur le port {}", PORT);

            while (true) {

                Socket clientSocket = serverSocket.accept();

                String ip = clientSocket.getInetAddress().getHostAddress();

                // =========================================================
                // TP3 SYN FLOOD / CONNECTION FLOOD PROTECTION
                // =========================================================
                if (!connectionLimiter.allow(ip)) {

                    AppLogger.SECURITY.warn(
                            "Connexion refusée : trop de connexions depuis {}",
                            ip
                    );

                    try {
                        PrintWriter tempOut =
                                new PrintWriter(clientSocket.getOutputStream(), true);

                        tempOut.println("ERROR:TOO_MANY_CONNECTIONS");

                    } catch (Exception ignored) {
                    }

                    clientSocket.close();
                    continue;
                }

                AppLogger.SERVER.info(
                        "Nouveau client connecté : {}",
                        ip
                );

                // =========================================================
                // CLIENT HANDLER THREAD
                // =========================================================
                ClientHandler clientHandler = new ClientHandler(clientSocket) {

                    @Override
                    public void run() {

                        try {
                            super.run();

                        } finally {

                            // =========================================================
                            // LIBÉRATION CONNEXION
                            // =========================================================
                            connectionLimiter.release(ip);

                            AppLogger.SERVER.info(
                                    "Connexion libérée : {}",
                                    ip
                            );
                        }
                    }
                };

                clientHandler.start();
            }

        } catch (IOException e) {

            AppLogger.SERVER.error(
                    "Erreur serveur TCP : {}",
                    e.getMessage()
            );

            e.printStackTrace();
        }
    }
}