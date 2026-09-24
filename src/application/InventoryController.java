package application;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InventoryController {

    private DatabaseManager dbManager;
    private TableView<Book> stockTable;
    private TableView<InventoryUpdate> updatesTable;
    
    private double currentAverageStock = 0.0;

    public InventoryController(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.stockTable = new TableView<>();
        this.updatesTable = new TableView<>();
    }

    public VBox getView() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));

        Label title = new Label("Inventory Management");
        title.getStyleClass().add("section-title");

        // --- Controls ---
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Book> bookFilter = new ComboBox<>();
        bookFilter.setPromptText("Select book to update");
        bookFilter.setPrefWidth(250);
        
        try { bookFilter.setItems(FXCollections.observableArrayList(dbManager.getAllBooks())); } 
        catch (SQLException e) { e.printStackTrace(); }

        bookFilter.setConverter(new StringConverter<>() {
            @Override public String toString(Book b) { return b != null ? b.getTitle() : ""; }
            @Override public Book fromString(String s) { return null; }
        });

        Button updateStockBtn = new Button("Update Stock");
        updateStockBtn.getStyleClass().add("primary-button");
        
        Button viewHistoryBtn = new Button("View Full History");

        controls.getChildren().addAll(new Label("Book:"), bookFilter, updateStockBtn, viewHistoryBtn);

        // --- Tables ---
        Label updatesTitle = new Label("Latest Inventory Logs");
        updatesTitle.getStyleClass().add("subsection-title");
        updatesTitle.setPadding(new Insets(10, 0, 5, 0));

        setupTables();
        refreshData(); // Initial data load

        // --- Events ---
        updateStockBtn.setOnAction(e -> {
            Book selected = bookFilter.getValue();
            if (selected == null) {
                showError("Please select a book first.");
                return;
            }
            showUpdateStockDialog(selected);
        });

        viewHistoryBtn.setOnAction(e -> showFullHistoryWindow());

        section.getChildren().addAll(title, controls, stockTable, updatesTitle, updatesTable);
        return section;
    }

    private void setupTables() {
        // 1. Stock Table
        stockTable.setPrefHeight(250);
        stockTable.getColumns().clear();

        TableColumn<Book, String> tCol = new TableColumn<>("Book Title");
        tCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        tCol.setPrefWidth(300);

        TableColumn<Book, Integer> sCol = new TableColumn<>("Current Stock");
        sCol.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        sCol.setPrefWidth(120);

        TableColumn<Book, String> stCol = new TableColumn<>("Status");
        stCol.setPrefWidth(120);
        stCol.setCellValueFactory(cd -> {
            int qty = cd.getValue().getStockQuantity();
            double threshold = currentAverageStock * 0.5;
            String status = qty <= threshold ? "LOW ⚠️" : "OK";
            return new SimpleStringProperty(status);
        });

        stockTable.getColumns().addAll(tCol, sCol, stCol);

        // 2. Updates Table
        updatesTable.setPrefHeight(250);
        updatesTable.getColumns().clear();
        
        TableColumn<InventoryUpdate, String> ubCol = new TableColumn<>("Book");
        ubCol.setPrefWidth(300);
        ubCol.setCellValueFactory(cd -> new SimpleStringProperty(getBookTitleById(cd.getValue().getBookId())));

        TableColumn<InventoryUpdate, String> udCol = new TableColumn<>("Date");
        udCol.setCellValueFactory(new PropertyValueFactory<>("updateDate"));
        udCol.setPrefWidth(200);

        TableColumn<InventoryUpdate, Integer> uqCol = new TableColumn<>("Change");
        uqCol.setCellValueFactory(new PropertyValueFactory<>("quantityChange"));
        uqCol.setPrefWidth(100);

        updatesTable.getColumns().addAll(ubCol, udCol, uqCol);
    }

    private Map<Integer, String> bookTitleCache;

    public void refreshData() {
        try {
            List<Book> books = dbManager.getAllBooks();
            //calculate current average stock
            currentAverageStock = books.stream()
                    .mapToInt(Book::getStockQuantity)
                    .average()
                    .orElse(0);

            stockTable.setItems(FXCollections.observableArrayList(books));
            stockTable.refresh(); // Refresh to update status column
            
            bookTitleCache = books.stream().collect(Collectors.toMap(Book::getId, Book::getTitle));
            updatesTable.setItems(FXCollections.observableArrayList(dbManager.getAllInventoryUpdates()));
            updatesTable.refresh();
            
        } catch (SQLException e) {
            showError("Failed to refresh data: " + e.getMessage());
        }
    }

    private String getBookTitleById(int id) {
        if (bookTitleCache != null && bookTitleCache.containsKey(id)) {
            return bookTitleCache.get(id);
        }
        return "ID: " + id;
    }

    private void showUpdateStockDialog(Book book) {
        Dialog<InventoryUpdate> dialog = new Dialog<>();
        dialog.setTitle("Update Stock");
        dialog.setHeaderText("Adjust stock for: " + book.getTitle());

        ButtonType updateBtnType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField changeField = new TextField(); 
        changeField.setPromptText("+10 or -5");
        
        TextField reasonField = new TextField(); 
        reasonField.setPromptText("e.g. New Shipment, Damaged...");

        grid.add(new Label("Quantity Change:"), 0, 0); grid.add(changeField, 1, 0);
        grid.add(new Label("Reason:"), 0, 1);          grid.add(reasonField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        // Enable/Disable update button based on input
        dialog.setResultConverter(btn -> {
            if (btn == updateBtnType) {
                try {
                    int delta = Integer.parseInt(changeField.getText().trim());
                    if (delta == 0) return null;

                    String formattedDate = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    
                    return new InventoryUpdate(0, book.getId(), formattedDate, delta);
                } catch (NumberFormatException e) {
                    showError("Invalid Number");
                    return null;
                }
            }
            return null;
        });
        // Handle the result
        dialog.showAndWait().ifPresent(update -> {
            try {
                String reason = reasonField.getText() == null ? "" : reasonField.getText().trim();
                dbManager.addInventoryUpdate(update, reason);
                
                int newStock = book.getStockQuantity() + update.getQuantityChange();
                if (newStock < 0) newStock = 0;
                dbManager.updateBookStock(book.getId(), newStock);

                refreshData();
                showSuccessMessage("Stock updated successfully.");
            } catch (SQLException e) {
                showError("Update failed: " + e.getMessage());
            }
        });
    }
    //show full history window
    private void showFullHistoryWindow() {
        Stage stage = new Stage();
        stage.setTitle("Full Inventory History");

        TableView<InventoryUpdate> fullTable = new TableView<>();
        
        TableColumn<InventoryUpdate, String> ubCol = new TableColumn<>("Book");
        ubCol.setPrefWidth(250);
        ubCol.setCellValueFactory(cd -> new SimpleStringProperty(getBookTitleById(cd.getValue().getBookId())));

        TableColumn<InventoryUpdate, String> udCol = new TableColumn<>("Date");
        udCol.setCellValueFactory(new PropertyValueFactory<>("updateDate"));
        udCol.setPrefWidth(180);

        TableColumn<InventoryUpdate, Integer> uqCol = new TableColumn<>("Change");
        uqCol.setCellValueFactory(new PropertyValueFactory<>("quantityChange"));
        uqCol.setPrefWidth(100);

        fullTable.getColumns().addAll(ubCol, udCol, uqCol);

        try {
            fullTable.setItems(FXCollections.observableArrayList(dbManager.getAllInventoryUpdates()));
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }

        VBox root = new VBox(fullTable);
        root.setPadding(new Insets(20));
        
        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("alert-error");
        alert.showAndWait();
    }
    
    private void showSuccessMessage(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("alert-success");
        alert.showAndWait();
    }
}