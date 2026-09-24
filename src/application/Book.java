package application;

public class Book {
    private int id;
    private String title;
    private double price;
    private int stockQuantity;
    private int supplierId;

    public Book(int id, String title, double price, int stockQuantity, int supplierId) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.supplierId = supplierId;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) {
    	this.supplierId =supplierId;
    	}
}