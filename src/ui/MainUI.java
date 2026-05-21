package ui;

import Client.ClientSocketService;

import javax.swing.*;

public class MainUI {
    public static void main(String[] args) {
        // FlatLaf DOIT être appelé avant tout composant Swing
        UITheme.setupLookAndFeel();

        SwingUtilities.invokeLater(() -> {
            ClientSocketService clientService = new ClientSocketService();
            new LoginFrame(clientService).setVisible(true);
        });
    }
}