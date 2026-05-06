package test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestReplayAttackTP2 {

    public static void main(String[] args) {
        try (
                Socket socket = new Socket("localhost", 5000);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            System.out.println("Serveur : " + in.readLine());

            out.println("ADMIN_GET_NONCE");
            String nonceResponse = in.readLine();
            System.out.println("Nonce reçu : " + nonceResponse);

            if (nonceResponse == null || !nonceResponse.startsWith("NONCE:")) {
                System.out.println("Erreur : nonce non reçu.");
                return;
            }

            String nonce = nonceResponse.substring("NONCE:".length());

            String command = "ADMIN_SECURE_TEST:" + nonce + ":TEST_MESSAGE";

            out.println(command);
            String firstResponse = in.readLine();
            System.out.println("1ère tentative : " + firstResponse);

            out.println(command);
            String replayResponse = in.readLine();
            System.out.println("Replay tentative : " + replayResponse);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}