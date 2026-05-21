# ChriOnline 🛍️

> Application e-commerce complète développée en Java, basée sur une architecture Client/Serveur native avec sockets TCP/UDP.

---

---

## 🚀 Fonctionnalités

### 👤 Côté Client
- Inscription avec vérification **OTP par email**
- Connexion sécurisée avec **gestion de session**
- Catalogue produits avec **recherche** et **filtres par catégorie**
- Panier dynamique (ajout, suppression, vidage)
- **Paiement simulé** — carte bancaire ou espèces
- Historique des commandes
- Gestion du profil utilisateur
- Interface disponible en **Français, Anglais et Arabe** 🌍

### 🔐 Côté Admin
- Authentification renforcée par **signature RSA** (certificat P12)
- Dashboard avec **métriques temps réel** (produits, commandes, revenus)
- Gestion complète des **produits**, **catégories**, **commandes**, **utilisateurs**
- Suivi des **alertes stock** et **historique des mouvements**
- Panneau de **notifications** avec filtres
- Statistiques et taux de commandes payées

---

## 🔒 Sécurité implémentée

| Protection | Description |
|---|---|
| **OTP Email** | Activation du compte via code à 6 chiffres |
| **RSA Auth** | Authentification admin par certificat et signature |
| **Session Token** | Gestion sécurisée des sessions utilisateurs |
| **Anti Replay** | Nonce unique par requête sensible |
| **Anti TCP Flood** | Limitation des connexions simultanées par IP |
| **Anti UDP Flood** | Rate limiting sur le serveur UDP |
| **Log4j2** | Journalisation complète des événements serveur et sécurité |

---

## 🛠️ Stack Technique

| Composant | Technologie |
|---|---|
| Langage | Java 21 |
| Interface graphique | Java Swing + **FlatLaf** |
| Communication réseau | Sockets **TCP/UDP** natifs |
| Base de données | **MySQL** via JDBC |
| Sécurité | **RSA**, OTP, Log4j2 |
| Email | JavaMail (javax.mail) |
| Build | Eclipse IDE |
| Versioning | Git / GitHub |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                    CLIENT (Swing UI)                 │
│  LoginFrame │ ShopFrame │ CartFrame │ AdminMainFrame │
└─────────────────────┬───────────────────────────────┘
                      │  TCP Socket
┌─────────────────────▼───────────────────────────────┐
│                    SERVEUR JAVA                      │
│  ClientHandler (Thread/client)  │  UdpSecurityServer│
│  ─────────────────────────────────────────────────  │
│  AuthService │ CartService │ OrderService            │
│  PaymentService │ ProductService │ NotificationService│
└─────────────────────┬───────────────────────────────┘
                      │  JDBC
┌─────────────────────▼───────────────────────────────┐
│                  BASE DE DONNÉES MySQL               │
│  users │ products │ categories │ orders │ payments   │
│  cart │ notifications │ stock_movements │ otp_codes  │
└─────────────────────────────────────────────────────┘
```

---

## 📁 Structure du projet

```
tp1/
├── src/
│   ├── Client/
│   │   ├── ClientApp.java
│   │   ├── ClientSocketService.java
│   │   └── AppSession.java
│   ├── dao/              # Accès base de données
│   ├── model/            # Entités métier
│   ├── security/         # RSA, OTP, Session, Flood protection
│   ├── server/           # Serveur TCP multi-clients + UDP
│   ├── service/          # Logique métier
│   ├── ui/               # Interface client (Swing)
│   │   ├── admin/        # Panneaux administration
│   │   ├── components/   # Composants réutilisables
│   │   └── theme/        # Thème visuel
│   └── util/             # AppLogger (Log4j2)
├── lib/                  # JARs externes
├── keys/                 # Certificats RSA (non versionnés)
├── image/                # Images produits
└── log4j2.xml
```

---

## ⚙️ Installation & Lancement

### Prérequis
- Java 21+
- MySQL 8+
- Eclipse IDE (ou tout autre IDE Java)

### 1. Cloner le projet
```bash
git clone https://github.com/TON_USERNAME/ChriOnline.git
cd ChriOnline
```

### 2. Configurer la base de données
```sql
CREATE DATABASE chrionline;
```
Puis importer le fichier SQL fourni et mettre à jour les credentials dans `DatabaseConnection.java`.

### 3. Ajouter les dépendances (dossier `lib/`)
- `flatlaf-3.7.1.jar`
- `mysql-connector-j-9.6.0.jar`
- `log4j-api-2.25.4.jar`
- `log4j-core-2.25.4.jar`
- `javax.mail.jar`
- `activation-1.1.1.jar`

### 4. Lancer le serveur
```
Exécuter : server/Server.java
```

### 5. Lancer le client
```
Exécuter : ui/MainUI.java
```


---

## 👨‍💻 Auteur

**Abdoulfatah Hisoua**
- 🎓 Étudiant en Génie Informatique — ENSAT
- 💼 [LinkedIn](https://www.linkedin.com/in/abdoulfatah-omar-hassan-a777a8338/)

---

## 📄 Licence

Ce projet a été développé dans le cadre d'un mini-projet académique à l'**ENSAT** — 2025/2026.

---

*Built with ☕ Java and lots of debugging*
