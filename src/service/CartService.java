package service;

import dao.CartDAO;
import model.Cart;
import model.CartItem;

public class CartService {

    // DAO utilisé pour accéder à la base de données
    private final CartDAO cartDAO;

    public CartService() {
        // Initialisation du DAO
        this.cartDAO = new CartDAO();
    }

    // Récupérer le panier d’un client
    public Cart getCartByClient(int clientId) {
        // Appel direct au DAO
        return cartDAO.getCartByClient(clientId);
    }

    // Ajouter un article au panier
    public boolean addItemToCart(int clientId, CartItem item) {
        // On récupère le panier du client
        Cart cart = cartDAO.getCartByClient(clientId);

        // Vérification : panier existe
        if (cart == null) {
            System.out.println("Panier introuvable.");
            return false;
        }

        // Vérification : item valide
        if (item == null || item.getProduct() == null) {
            System.out.println("Article invalide.");
            return false;
        }

        // Vérification : quantité correcte
        if (item.getQuantity() <= 0) {
            System.out.println("Quantité invalide.");
            return false;
        }

        // Appel au DAO pour ajouter l’article en base
        return cartDAO.addItem(cart.getId(), item);
    }

    // Supprimer un article du panier via son productId
    public boolean removeItemFromCart(int clientId, int productId) {
        // Récupération du panier
        Cart cart = cartDAO.getCartByClient(clientId);

        // Vérification : panier existe
        if (cart == null) {
            System.out.println("Panier introuvable.");
            return false;
        }

        // Suppression via DAO
        return cartDAO.removeItem(cart.getId(), productId);
    }

    // Supprimer un article via le nom du produit
    public boolean removeItemFromCartByName(int clientId, String productName) {
        // Récupération du panier
        Cart cart = cartDAO.getCartByClient(clientId);

        // Vérification : panier existe
        if (cart == null) {
            System.out.println("Panier introuvable.");
            return false;
        }

        // Vérification : nom valide
        if (productName == null || productName.trim().isEmpty()) {
            System.out.println("Nom produit invalide.");
            return false;
        }

        // Suppression via DAO avec le nom du produit
        return cartDAO.removeItemByProductName(cart.getId(), productName);
    }

    // Vider complètement le panier
    public boolean clearCart(int clientId) {
        // Récupération du panier
        Cart cart = cartDAO.getCartByClient(clientId);

        // Vérification : panier existe
        if (cart == null) {
            System.out.println("Panier introuvable.");
            return false;
        }

        // Suppression de tous les items
        return cartDAO.clear(cart.getId());
    }

    // Calculer le total du panier
    public double calculateCartTotal(int clientId) {
        // Récupération du panier
        Cart cart = cartDAO.getCartByClient(clientId);

        // Si panier vide ou inexistant → total = 0
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return 0.0;
        }

        // Calcul du total via la méthode du modèle Cart
        return cart.calculateTotal();
    }

    // Vérifier si le panier est vide
    public boolean isCartEmpty(int clientId) {
        // Récupération du panier
        Cart cart = cartDAO.getCartByClient(clientId);

        // Retourne true si panier inexistant ou sans items
        return cart == null || cart.getItems() == null || cart.getItems().isEmpty();
    }
}