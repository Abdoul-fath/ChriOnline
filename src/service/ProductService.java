package service;

import dao.ProductDAO;
import model.Product;

import java.util.List;

public class ProductService {

    private final ProductDAO productDAO;

    public ProductService() {
        this.productDAO = new ProductDAO();
    }

    public List<Product> getAllProducts() {
        return productDAO.findAll();
    }

    public Product getProductById(int id) {
        return productDAO.findById(id);
    }

    public Product getProductByName(String name) {
        return productDAO.findByName(name);
    }

    public boolean addProduct(Product product) {
        return productDAO.save(product);
    }

    public boolean updateProduct(Product product) {
        return productDAO.update(product);
    }

    public boolean updateProductStock(int productId, int newStock) {
        return productDAO.updateStock(productId, newStock);
    }

    public boolean deleteProduct(int id) {
        return productDAO.delete(id);
    }
}