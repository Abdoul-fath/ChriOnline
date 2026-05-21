package dao;

import database.DatabaseConnection;
import model.UserCertificateInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

public class UserCertificateDAO {

    public UserCertificateInfo findActiveByEmail(String email) {
        String sql = """
                SELECT *
                FROM user_certificates
                WHERE email = ?
                ORDER BY id DESC
                LIMIT 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserCertificateInfo cert = new UserCertificateInfo();
                    cert.setId(rs.getInt("id"));
                    cert.setUserId(rs.getInt("user_id"));
                    cert.setEmail(rs.getString("email"));
                    cert.setRole(rs.getString("role"));
                    cert.setAliasName(rs.getString("alias_name"));
                    cert.setKeystorePath(rs.getString("keystore_path"));
                    cert.setCertificatePath(rs.getString("certificate_path"));
                    cert.setStatus(rs.getString("status"));

                    // ✅ expires_at en DATETIME
                    if (rs.getTimestamp("expires_at") != null) {
                        cert.setExpiresAt(rs.getTimestamp("expires_at")
                                .toLocalDateTime());
                    }

                    // ⭐ mot de passe chiffré
                    cert.setKeystorePasswordEncrypted(
                            rs.getString("keystore_password_encrypted"));

                    return cert;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void revokeCertificate(int certId, String reason) {
        String sql = """
                UPDATE user_certificates
                SET status = 'revoked',
                    revoked_at = NOW(),
                    revoke_reason = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setInt(2, certId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}