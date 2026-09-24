package application;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OrderController {

    private DatabaseManager dbManager;
    private TableView<Order> ordersTable;

    public OrderController(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.ordersTable = new TableView<>();
    }

    public VBox getView() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));

        Label title = new Label("Orders Management");
        title.getStyleClass().add("section-title");

        // --- Filters ---
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        DatePicker fromDate = new DatePicker(); fromDate.setPromptText("From date");
        DatePicker toDate = new DatePicker();   toDate.setPromptText("To date");

        ComboBox<Customer> customerFilter = new ComboBox<>();
        customerFilter.setPromptText("Filter by Customer");
        try {
            customerFilter.setItems(FXCollections.observableArrayList(dbManager.getAllCustomers()));
        } catch (SQLException e) { e.printStackTrace(); }

        // Customer Converter (Display Name instead of Object reference)
        customerFilter.setConverter(new StringConverter<>() {
            @Override public String toString(Customer c) { return c != null ? c.getFirstName() : ""; }
            @Override public Customer fromString(String s) { return null; }
        });

        Button filterBtn = new Button("Filter");
        Button clearBtn = new Button("Clear");
        Button newOrderBtn = new Button("New Sale Order");
        newOrderBtn.getStyleClass().add("primary-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        controls.getChildren().addAll(
            new Label("From:"), fromDate, 
            new Label("To:"), toDate, 
            new Label("Customer:"), customerFilter,
            filterBtn, clearBtn, spacer, newOrderBtn
        );

        // --- Table Setup ---
        setupTable();
        refreshTableData();

        // --- Event Handlers ---
        filterBtn.setOnAction(e -> handleFilter(fromDate.getValue(), toDate.getValue(), customerFilter.getValue()));
        
        clearBtn.setOnAction(e -> {
            fromDate.setValue(null);
            toDate.setValue(null);
            customerFilter.setValue(null);
            refreshTableData();
        });

        newOrderBtn.setOnAction(e -> showAddOrderDialog());

        section.getChildren().addAll(title, controls, ordersTable);
        return section;
    }

    private void setupTable() {
        ordersTable.setPrefHeight(520);
        ordersTable.getColumns().clear();

        TableColumn<Order, Integer> orderIdCol = new TableColumn<>("ID");
        orderIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        orderIdCol.setPrefWidth(80);

        // Custom Cell Value Factory to show Customer Name instead of ID
        TableColumn<Order, String> custNameCol = new TableColumn<>("Customer");
        custNameCol.setPrefWidth(200);
        custNameCol.setCellValueFactory(cell -> {
            int cid = cell.getValue().getCustomerId();
            try {
                // Note: In a larger app, we should cache customers map to avoid DB calls per row
                Customer c = dbManager.getAllCustomers().stream()
                        .filter(x -> x.getId() == cid).findFirst().orElse(null);
                return new SimpleStringProperty(c != null ? c.getFirstName() : "Unknown ID:" + cid);
            } catch (SQLException e) {
                return new SimpleStringProperty("Error");
            }
        });

        TableColumn<Order, String> dateCol = new TableColumn<>("Order Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        dateCol.setPrefWidth(180);

        TableColumn<Order, Double> totalCol = new TableColumn<>("Total ($)");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        totalCol.setPrefWidth(120);

        TableColumn<Order, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(150);
        
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(8, deleteBtn);
            {
                box.setAlignment(Pos.CENTER);
                deleteBtn.getStyleClass().add("delete-button");
                deleteBtn.setOnAction(e -> {
                    Order o = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setContentText("Delete Order ID: " + o.getId() + "?");
                    alert.showAndWait().ifPresent(type -> {
                        if (type == ButtonType.OK) {
                            try {
                                dbManager.deleteOrder(o.getId());
                                refreshTableData();
                                showSuccessMessage("Order deleted.");
                            } catch (SQLException ex) {
                                showError("Delete failed: " + ex.getMessage());
                            }
                        }
                    });
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        ordersTable.getColumns().addAll(orderIdCol, custNameCol, dateCol, totalCol, actionsCol);
    }

    public void refreshTableData() {
        try {
            ordersTable.setItems(FXCollections.observableArrayList(dbManager.getAllOrders()));
        } catch (SQLException e) {
            showError("Failed to load orders: " + e.getMessage());
        }
    }

    // --- Logic & Filtering ---

    private void handleFilter(LocalDate start, LocalDate end, Customer customer) {
        try {
            List<Order> all = dbManager.getAllOrders();
            List<Order> filtered = all.stream().filter(o -> {
                LocalDate d = parseOrderDateToLocalDate(o.getOrderDate());
                if (d == null) return false;
                
                boolean dateMatch = (start == null || !d.isBefore(start)) && 
                                    (end == null || !d.isAfter(end));
                boolean custMatch = (customer == null || o.getCustomerId() == customer.getId());                
                return dateMatch && custMatch;
            }).collect(Collectors.toList());
            
            ordersTable.setItems(FXCollections.observableArrayList(filtered));
        } catch (SQLException ex) {
            showError("Filter failed: " + ex.getMessage());
        }
    }

    // Helper to parse complex date strings safely
    private LocalDate parseOrderDateToLocalDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            if (value.contains("T")) return LocalDate.parse(value.split("T")[0]);
            if (value.contains(" ")) return LocalDate.parse(value.split(" ")[0]);
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) { return null; }
    }

    // --- The Shopping Cart Logic (New Sale Order) ---

    private void showAddOrderDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("New Sale Order");
        dialog.setHeaderText("Create new order with items");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());

        // 1. Customer Selection
        ComboBox<Customer> customerCombo = new ComboBox<>();
        try { customerCombo.setItems(FXCollections.observableArrayList(dbManager.getAllCustomers())); } 
        catch (SQLException e) { e.printStackTrace(); }
        customerCombo.setPromptText("Select Customer");
        customerCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Customer c) { return c != null ? c.getFirstName() : ""; }
            @Override public Customer fromString(String s) { return null; }
        });

        // 2. Cart Table (Local list for the dialog)
        TableView<OrderDetail> cartTable = new TableView<>();
        ObservableList<OrderDetail> cartItems = FXCollections.observableArrayList();
        cartTable.setItems(cartItems);
        cartTable.setPrefHeight(200);

        TableColumn<OrderDetail, Integer> idCol = new TableColumn<>("Book ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        
        TableColumn<OrderDetail, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<OrderDetail, Double> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        cartTable.getColumns().addAll(idCol, qtyCol, priceCol);

        // 3. Add Item Controls
        ComboBox<Book> bookCombo = new ComboBox<>();
        try { bookCombo.setItems(FXCollections.observableArrayList(dbManager.getAllBooks())); } 
        catch (SQLException e) { e.printStackTrace(); }
        bookCombo.setPromptText("Select Book");
        bookCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Book b) { return b != null ? b.getTitle() + " ($" + b.getPrice() + ")" : ""; }
            @Override public Book fromString(String s) { return null; }
        });

        TextField qtyField = new TextField("1");
        qtyField.setPrefWidth(60);
        Button addItemBtn = new Button("Add to Cart");

        // Add Item Logic
        addItemBtn.setOnAction(e -> {
            Book selBook = bookCombo.getValue();
            if (selBook == null) {
                showError("Select a book first."); return;
            }
            try {
                int qty = Integer.parseInt(qtyField.getText().trim());
                if (qty <= 0) { showError("Quantity must be > 0"); return; }
                if (qty > selBook.getStockQuantity()) {
                    showError("Not enough stock! Available: " + selBook.getStockQuantity()); return;
                }
                
                // Check if book already in cart (Simple check)
                boolean exists = cartItems.stream().anyMatch(item -> item.getBookId() == selBook.getId());
                if(exists) { showError("Book already in cart."); return; }

                cartItems.add(new OrderDetail(selBook.getId(), qty, selBook.getPrice()));
                
            } catch (NumberFormatException ex) {
                showError("Invalid Quantity");
            }
        });

        // Layout
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        HBox addBox = new HBox(10, bookCombo, new Label("Qty:"), qtyField, addItemBtn);
        addBox.setAlignment(Pos.CENTER_LEFT);
        
        layout.getChildren().addAll(new Label("Customer:"), customerCombo, new Separator(), new Label("Cart Items:"), cartTable, addBox);
        dialog.getDialogPane().setContent(layout);

        ButtonType checkoutBtn = new ButtonType("Checkout", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(checkoutBtn, ButtonType.CANCEL);

        // Checkout Logic (Transaction Call)
        dialog.setResultConverter(btn -> {
            if (btn == checkoutBtn) {
                if (customerCombo.getValue() == null) {
                    showError("Please select a customer."); return null;
                }
                if (cartItems.isEmpty()) {
                    showError("Cart is empty!"); return null;
                }
                
                try {
                    // Call the Transaction Method in DB Manager
                    dbManager.placeOrder(customerCombo.getValue().getId(), new ArrayList<>(cartItems));
                    refreshTableData(); // Refresh order list
                    showSuccessMessage("Order Placed Successfully!");
                } catch (SQLException ex) {
                    showError("Transaction Failed: " + ex.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    // --- Helpers ---
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