package application;

public class InventoryUpdate {
    private int id;
    private int bookId;
    private String updateDate;
    private int quantityChange;

    public InventoryUpdate(int id, int bookId, String updateDate, int quantityChange) {
        this.id = id;
        this.bookId = bookId;
        this.updateDate = updateDate;
        this.quantityChange = quantityChange;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }
    public String getUpdateDate() { return updateDate; }
    public void setUpdateDate(String updateDate) { this.updateDate = updateDate; }
    public int getQuantityChange() { return quantityChange; }
    public void setQuantityChange(int quantityChange) { this.quantityChange = quantityChange;}
}