package dao;

import database.DatabaseConnection;
import model.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class PaymentDAO {

    // Connexion à la base de données
    private Connection conn;

    public PaymentDAO() {
        // On récupère la connexion via la classe DatabaseConnection
        this.conn = DatabaseConnection.getConnection();
    }

    // ── Enregistrer un paiement ───────────────────────────────
    public boolean save(Payment payment) {
        // Requête SQL pour insérer un paiement dans la table payments
        String sql = "INSERT INTO payments (order_id, method, amount, status, paid_at) VALUES (?, ?, ?, ?, ?)";

        // PreparedStatement permet d'exécuter la requête SQL avec des paramètres
        // Statement.RETURN_GENERATED_KEYS permet de récupérer l'id auto-généré après l'insertion
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Remplacement des ? dans la requête par les valeurs de l'objet payment
            ps.setInt(1, payment.getOrderId());      // id de la commande liée au paiement
            ps.setString(2, payment.getMethod());    // méthode de paiement : card ou especes
            ps.setDouble(3, payment.getAmount());    // montant payé
            ps.setString(4, payment.getStatus());    // statut du paiement : pending, success, failed

            // Gestion de la date paidAt
            // Si la date existe, on la convertit en Timestamp SQL
            if (payment.getPaidAt() != null) {
                ps.setTimestamp(5, Timestamp.valueOf(payment.getPaidAt()));
            } else {
                // Sinon on met NULL dans la base
                ps.setTimestamp(5, null);
            }

            // Exécution de l'insertion
            // executeUpdate retourne le nombre de lignes affectées
            int rows = ps.executeUpdate();

            // Si au moins une ligne a été insérée
            if (rows > 0) {
                // On récupère l'id généré automatiquement par la base
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    payment.setId(rs.getInt(1)); // on met à jour l'objet payment avec son id
                }
                return true; // insertion réussie
            }

        } catch (SQLException e) {
            // En cas d'erreur SQL, on affiche le message d'erreur
            System.out.println("Erreur save payment : " + e.getMessage());
        }

        // Si l'insertion échoue, on retourne false
        return false;
    }
}