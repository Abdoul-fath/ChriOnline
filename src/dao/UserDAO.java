package dao;

import database.DatabaseConnection;
import model.Admin;
import model.Client;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private final Connection connection;

    public UserDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    public User findByEmail(String email) {
        String sql = "SELECT u.*, c.address, c.phone, c.ville " +
                "FROM users u LEFT JOIN clients c ON u.id = c.id " +
                "WHERE u.email = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }

        } catch (SQLException e) {
            System.out.println("Erreur findByEmail : " + e.getMessage());
        }

        return null;
    }

    public User findByEmailWithPublicKey(String email) {
        String sql = "SELECT u.*, c.address, c.phone, c.ville " +
                "FROM users u LEFT JOIN clients c ON u.id = c.id " +
                "WHERE u.email = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }

        } catch (SQLException e) {
            System.out.println("Erreur findByEmailWithPublicKey : " + e.getMessage());
        }

        return null;
    }

    public boolean save(Client client) {
        return saveClientWithStatus(client, "active");
    }

    public boolean savePendingClient(Client client) {
        return saveClientWithStatus(client, "pending");
    }

    private boolean saveClientWithStatus(Client client, String status) {
        String sqlUser = "INSERT INTO users (nom, prenom, email, password, role, status) VALUES (?, ?, ?, ?, 'client', ?)";
        String sqlClient = "INSERT INTO clients (id, address, phone, ville) VALUES (?, ?, ?, ?)";

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement psUser = connection.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, client.getNom());
                psUser.setString(2, client.getPrenom());
                psUser.setString(3, client.getEmail());
                psUser.setString(4, client.getPassword());
                psUser.setString(5, status);
                psUser.executeUpdate();

                ResultSet keys = psUser.getGeneratedKeys();
                if (!keys.next()) {
                    connection.rollback();
                    return false;
                }

                int id = keys.getInt(1);
                client.setId(id);

                try (PreparedStatement psClient = connection.prepareStatement(sqlClient)) {
                    psClient.setInt(1, id);
                    psClient.setString(2, client.getAddress());
                    psClient.setString(3, client.getPhone());
                    psClient.setString(4, client.getVille());
                    psClient.executeUpdate();
                }
            }

            connection.commit();
            return true;

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            System.out.println("Erreur save client : " + e.getMessage());
            return false;

        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            System.out.println("Erreur emailExists : " + e.getMessage());
            return false;
        }
    }

    public boolean isAccountActive(String email) {
        String sql = "SELECT status FROM users WHERE email = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return "active".equalsIgnoreCase(rs.getString("status"));
            }

        } catch (SQLException e) {
            System.out.println("Erreur isAccountActive : " + e.getMessage());
        }

        return false;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();

        String sql = "SELECT u.*, c.address, c.phone, c.ville " +
                "FROM users u LEFT JOIN clients c ON u.id = c.id " +
                "ORDER BY u.id DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                users.add(mapUser(rs));
            }

        } catch (SQLException e) {
            System.out.println("Erreur findAll users : " + e.getMessage());
        }

        return users;
    }

    public User findById(int userId) {
        String sql = "SELECT u.*, c.address, c.phone, c.ville " +
                "FROM users u LEFT JOIN clients c ON u.id = c.id " +
                "WHERE u.id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }

        } catch (SQLException e) {
            System.out.println("Erreur findById : " + e.getMessage());
        }

        return null;
    }

    // =========================================================
    // RSA PUBLIC KEY METHODS
    // =========================================================

    public boolean updatePublicKey(int userId, String publicKey) {
        String sql = "UPDATE users SET public_key = ?, auth_type = 'rsa' WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, publicKey);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur updatePublicKey : " + e.getMessage());
            return false;
        }
    }

    public String getPublicKeyByEmail(String email) {
        String sql = "SELECT public_key FROM users WHERE email = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("public_key");
            }

        } catch (SQLException e) {
            System.out.println("Erreur getPublicKeyByEmail : " + e.getMessage());
        }

        return null;
    }

    public boolean saveAdminChallenge(int userId, String challenge, java.util.Date expiresAt) {
        String sql = "INSERT INTO admin_challenges (user_id, challenge_value, expires_at, used_flag, created_at) " +
                "VALUES (?, ?, ?, 0, NOW())";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, challenge);
            ps.setTimestamp(3, new Timestamp(expiresAt.getTime()));
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Erreur saveAdminChallenge : " + e.getMessage());
            return false;
        }
    }

    public boolean verifyAndUseChallenge(int userId, String challenge) {
        String sql = "SELECT id FROM admin_challenges " +
                "WHERE user_id = ? AND challenge_value = ? AND used_flag = 0 AND expires_at > NOW() " +
                "ORDER BY id DESC LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, challenge);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int challengeId = rs.getInt("id");

                String updateSql = "UPDATE admin_challenges SET used_flag = 1 WHERE id = ?";
                try (PreparedStatement updatePs = connection.prepareStatement(updateSql)) {
                    updatePs.setInt(1, challengeId);
                    updatePs.executeUpdate();
                }

                return true;
            }

        } catch (SQLException e) {
            System.out.println("Erreur verifyAndUseChallenge : " + e.getMessage());
        }

        return false;
    }

    public boolean updateProfile(int userId, String nomComplet, String email, String phone, String address, String ville) {
        try {
            connection.setAutoCommit(false);

            String nom = nomComplet;
            String prenom = "";

            if (nomComplet != null && nomComplet.trim().contains(" ")) {
                int idx = nomComplet.trim().indexOf(" ");
                prenom = nomComplet.trim().substring(0, idx).trim();
                nom = nomComplet.trim().substring(idx + 1).trim();
            }

            if (nom == null || nom.isBlank()) {
                nom = "SansNom";
            }

            String sqlUser = "UPDATE users SET nom = ?, prenom = ?, email = ? WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sqlUser)) {
                ps.setString(1, nom);
                ps.setString(2, prenom);
                ps.setString(3, email);
                ps.setInt(4, userId);
                ps.executeUpdate();
            }

            String role = null;
            String roleSql = "SELECT role FROM users WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(roleSql)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    role = rs.getString("role");
                }
            }

            if ("client".equalsIgnoreCase(role)) {
                String checkClientSql = "SELECT id FROM clients WHERE id = ?";
                boolean exists = false;

                try (PreparedStatement ps = connection.prepareStatement(checkClientSql)) {
                    ps.setInt(1, userId);
                    ResultSet rs = ps.executeQuery();
                    exists = rs.next();
                }

                if (exists) {
                    String updateClientSql = "UPDATE clients SET address = ?, phone = ?, ville = ? WHERE id = ?";
                    try (PreparedStatement ps = connection.prepareStatement(updateClientSql)) {
                        ps.setString(1, address);
                        ps.setString(2, phone);
                        ps.setString(3, ville);
                        ps.setInt(4, userId);
                        ps.executeUpdate();
                    }
                } else {
                    String insertClientSql = "INSERT INTO clients (id, address, phone, ville) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(insertClientSql)) {
                        ps.setInt(1, userId);
                        ps.setString(2, address);
                        ps.setString(3, phone);
                        ps.setString(4, ville);
                        ps.executeUpdate();
                    }
                }
            }

            connection.commit();
            return true;

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            System.out.println("Erreur updateProfile : " + e.getMessage());
            return false;

        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        String status = rs.getString("status");
        String publicKey = null;

        try {
            publicKey = rs.getString("public_key");
        } catch (SQLException ignored) {
        }

        if ("client".equalsIgnoreCase(role)) {
            Client client = new Client(
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("address"),
                    rs.getString("phone"),
                    rs.getString("ville")
            );
            client.setId(rs.getInt("id"));
            client.setRole(role);
            client.setStatus(status);
            client.setPublicKey(publicKey);
            return client;
        } else {
            Admin admin = new Admin(
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("email"),
                    rs.getString("password")
            );
            admin.setId(rs.getInt("id"));
            admin.setRole(role);
            admin.setStatus(status);
            admin.setPublicKey(publicKey);
            return admin;
        }
    }
}