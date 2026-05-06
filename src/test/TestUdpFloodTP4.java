package test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TestUdpFloodTP4 {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 7001;

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(500);

            InetAddress address = InetAddress.getByName(host);

            for (int i = 1; i <= 30; i++) {
                String message = "UDP_TEST_PACKET_" + i;

                byte[] data = message.getBytes();

                DatagramPacket packet = new DatagramPacket(
                        data,
                        data.length,
                        address,
                        port
                );

                socket.send(packet);

                try {
                    byte[] buffer = new byte[1024];

                    DatagramPacket responsePacket = new DatagramPacket(
                            buffer,
                            buffer.length
                    );

                    socket.receive(responsePacket);

                    String response = new String(
                            responsePacket.getData(),
                            0,
                            responsePacket.getLength()
                    );

                    System.out.println(i + " -> Réponse : " + response);

                } catch (Exception timeout) {
                    System.out.println(i + " -> Paquet ignoré / bloqué");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}