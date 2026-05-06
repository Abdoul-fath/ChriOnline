package test;

import service.UserCertificateService;

public class TestUserCertificate {

    public static void main(String[] args) {
        UserCertificateService service = new UserCertificateService();

        int userId = 1; // mets l'ID d'un vrai admin ou client dans ta DB

        boolean ok = service.createCertificateForUser(userId);

        System.out.println("Certificat créé ? " + ok);
    }
}