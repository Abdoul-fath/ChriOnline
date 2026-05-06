package dao;

import database.DatabaseConnection;
import model.Cart;
import model.CartItem;
import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartDAO {

    // Connexion à la base de données
    private final Connection conn;

    public CartDAO() {
        // On récupère la connexion via la classe DatabaseConnection
        this.conn = DatabaseConnection.getConnection();
    }

    public Cart getCartByClient(int clientId) {
        // Requête pour chercher le panier du client
        String sqlCart = "SELECT * FROM carts WHERE client_id = ?";

        // Requête pour récupérer les articles du panier
        // On fait un LEFT JOIN avec products pour obtenir aussi les infos du produit
        String sqlItems = "SELECT ci.id, ci.quantity, p.id_product, p.name, p.description, p.image, p.price, p.stock " +
                          "FROM cart_items ci " +
                          "LEFT JOIN products p ON ci.product_id = p.id_product " +
                          "WHERE ci.cart_id = ?";

        try (PreparedStatement psCart = conn.prepareStatement(sqlCart)) {
            // Remplacement du ? par l'id du client
            psCart.setInt(1, clientId);
            ResultSet rsCart = psCart.executeQuery();

            Cart cart = null;

            // Si le panier existe déjà dans la base
            if (rsCart.next()) {
                cart = new Cart();
                cart.setId(rsCart.getInt("id"));
                cart.setClientId(rsCart.getInt("client_id"));

                // Récupération de la date de création du panier
                Timestamp createdAt = rsCart.getTimestamp("created_at");
                if (createdAt != null) {
                    cart.setCreatedAt(createdAt.toString());
                }
            } else {
                // Si aucun panier n'existe pour ce client, on en crée un nouveau
                int newCartId = createCart(clientId);
                if (newCartId != -1) {
                    cart = new Cart();
                    cart.setId(newCartId);
                    cart.setClientId(clientId);
                }
            }

            // Si le panier a bien été trouvé ou créé
            if (cart != null) {
                List<CartItem> items = new ArrayList<>();

                try (PreparedStatement psItems = conn.prepareStatement(sqlItems)) {
                    // On récupère tous les items liés à ce panier
                    psItems.setInt(1, cart.getId());
                    ResultSet rsItems = psItems.executeQuery();

                    // On parcourt chaque ligne du résultat
                    while (rsItems.next()) {
                        CartItem item = new CartItem();
                        item.setId(rsItems.getInt("id"));
                        item.setQuantity(rsItems.getInt("quantity"));

                        // On récupère l'id du produit
                        int productId = rsItems.getInt("id_product");

                        // Vérifie si la colonne id_product n'est pas NULL
                        if (!rsItems.wasNull()) {
                            // Création de l'objet Product à partir des données SQL
                            Product product = new Product(
                                    productId,
                                    rsItems.getString("name"),
                                    rsItems.getString("description"),
                                    rsItems.getString("image"),
                                    rsItems.getDouble("price"),
                                    rsItems.getInt("stock")
                            );
                            item.setProduct(product);
                        }

                        // Ajout de l'article à la liste du panier
                        items.add(item);
                    }
                }

                // On affecte la liste des items au panier
                cart.setItems(items);
            }

            // Retourne le panier complet
            return cart;

        } catch (SQLException e) {
            // En cas d'erreur SQL
            System.out.println("Erreur getCartByClient : " + e.getMessage());
            return null;
        }
    }

    private int createCart(int clientId) {
        // Requête pour créer un nouveau panier pour un client
        String sql = "INSERT INTO carts (client_id) VALUES (?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // On remplace le ? par l'id du client
            ps.setInt(1, clientId);
            ps.executeUpdate();

            // Récupération de l'id auto-généré du nouveau panier
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.out.println("Erreur createCart : " + e.getMessage());
        }

        // Si erreur, on retourne -1
        return -1;
    }

    public boolean addItem(int cartId, CartItem item) {
        // Vérification de sécurité : l'article et le produit doivent exister
        if (item == null || item.getProduct() == null) {
            System.out.println("Erreur addItem : produit invalide.");
            return false;
        }

        // Vérifie si le produit existe déjà dans le panier
        String checkSql = "SELECT * FROM cart_items WHERE cart_id = ? AND product_id = ?";

        // Si le produit existe déjà, on augmente sa quantité
        String updateSql = "UPDATE cart_items SET quantity = quantity + ? WHERE cart_id = ? AND product_id = ?";

        // Sinon, on insère un nouvel article dans le panier
        String insertSql = "INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (?, ?, ?)";

        try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            psCheck.setInt(1, cartId);
            psCheck.setInt(2, item.getProduct().getIdProduct());

            ResultSet rs = psCheck.executeQuery();

            // Si le produit est déjà présent
            if (rs.next()) {
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setInt(1, item.getQuantity());
                    psUpdate.setInt(2, cartId);
                    psUpdate.setInt(3, item.getProduct().getIdProduct());
                    psUpdate.executeUpdate();
                }
            } else {
                // Sinon on ajoute une nouvelle ligne
                try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                    psInsert.setInt(1, cartId);
                    psInsert.setInt(2, item.getProduct().getIdProduct());
                    psInsert.setInt(3, item.getQuantity());
                    psInsert.executeUpdate();
                }
            }

            return true;

        } catch (SQLException e) {
            System.out.println("Erreur addItem : " + e.getMessage());
            return false;
        }
    }

    public boolean removeItem(int cartId, int productId) {
        // Requête pour supprimer un produit précis du panier
        String sql = "DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.setInt(2, productId);

            // executeUpdate retourne le nombre de lignes supprimées
            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.out.println("Erreur removeItem : " + e.getMessage());
            return false;
        }
    }

    public boolean removeItemByProductName(int cartId, String productName) {
        // Requête pour supprimer un produit du panier à partir de son nom
        // On joint cart_items et products pour retrouver le produit par son nom
        String sql = "DELETE ci FROM cart_items ci " +
                     "JOIN products p ON ci.product_id = p.id_product " +
                     "WHERE ci.cart_id = ? AND p.name = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.setString(2, productName);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.out.println("Erreur removeItemByProductName : " + e.getMessage());
            return false;
        }
    }

    public boolean clear(int cartId) {
        // Requête pour supprimer tous les articles d’un panier
        String sql = "DELETE FROM cart_items WHERE cart_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cartId);

            // On exécute la suppression de tous les items
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Erreur clear : " + e.getMessage());
            return false;
        }
    }
}