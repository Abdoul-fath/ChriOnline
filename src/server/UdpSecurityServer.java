package server;

import security.UdpRateLimiter;
import util.AppLogger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpSecurityServer extends Thread {

    private static final int UDP_PORT = 7001;
    private static final int BUFFER_SIZE = 1024;

    private final UdpRateLimiter rateLimiter = new UdpRateLimiter();

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {

            AppLogger.SERVER.info("Serveur UDP sécurité lancé sur le port {}", UDP_PORT);

            byte[] buffer = new byte[BUFFER_SIZE];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                InetAddress address = packet.getAddress();
                String ip = address.getHostAddress();

                if (!rateLimiter.allow(ip)) {
                    AppLogger.SECURITY.warn("UDP Flood bloqué depuis IP={}", ip);
                    continue;
                }

                String message = new String(packet.getData(), 0, packet.getLength()).trim();

                AppLogger.SECURITY.info("Paquet UDP accepté depuis {} : {}", ip, message);

                String response = "UDP_OK";
                byte[] responseBytes = response.getBytes();

                DatagramPacket responsePacket = new DatagramPacket(
                        responseBytes,
                        responseBytes.length,
                        packet.getAddress(),
                        packet.getPort()
                );

                socket.send(responsePacket);
            }

        } catch (Exception e) {
            AppLogger.SERVER.error("Impossible de lancer le serveur UDP sur le port {} : {}", UDP_PORT, e.getMessage());
        }
    }
}