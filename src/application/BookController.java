package application;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BookController {

    private DatabaseManager dbManager;
    private TableView<Book> booksTable;

    public BookController(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.booksTable = new TableView<>();
    }

    public VBox getView() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));

        Label title = new Label("Books Management");
        title.getStyleClass().add("section-title");

        // --- Search Bar Setup ---
        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search by title...");
        searchField.setPrefWidth(220);

        ComboBox<Supplier> supplierFilter = new ComboBox<>();
        supplierFilter.setPromptText("Filter by supplier");
        supplierFilter.setPrefWidth(180);
        try {
            supplierFilter.setItems(FXCollections.observableArrayList(dbManager.getAllSuppliers()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Supplier Converter logic...
        supplierFilter.setConverter(new StringConverter<>() {
            @Override public String toString(Supplier s) { return s != null ? s.getName() : ""; }
            @Override public Supplier fromString(String string) { return null; }
        });

        Button searchBtn = new Button("Search");
        Button clearBtn = new Button("Clear");
        Button addBookBtn = new Button("Add New Book");
        addBookBtn.getStyleClass().add("primary-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchBar.getChildren().addAll(new Label("Title:"), searchField, new Label("Supplier:"), supplierFilter,
                searchBtn, clearBtn, spacer, addBookBtn);

        // --- Table Setup ---
        setupTable();
        refreshTableData();

        // --- Event Handlers (Controller Logic) ---
        searchBtn.setOnAction(e -> handleSearch(searchField.getText(), supplierFilter.getValue()));
        
        clearBtn.setOnAction(e -> {
            searchField.clear();
            supplierFilter.setValue(null);
            refreshTableData();
        });

        addBookBtn.setOnAction(e -> showAddBookDialog());

        section.getChildren().addAll(title, searchBar, booksTable);
        return section;
    }

    private void setupTable() {
        booksTable.setPrefHeight(520);
        booksTable.getColumns().clear();

        TableColumn<Book, Integer> idCol = new TableColumn<>("Book ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(90);

        TableColumn<Book, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(280);

        TableColumn<Book, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(110);

        TableColumn<Book, Integer> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        stockCol.setPrefWidth(90);

        TableColumn<Book, Integer> supplierIdCol = new TableColumn<>("Supplier ID");
        supplierIdCol.setCellValueFactory(new PropertyValueFactory<>("supplierId"));
        supplierIdCol.setPrefWidth(110);

        TableColumn<Book, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(220);
        
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(8, editBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().add("edit-button");
                deleteBtn.getStyleClass().add("delete-button");

                editBtn.setOnAction(e -> {
                    Book b = getTableView().getItems().get(getIndex());
                    showEditBookDialog(b);
                });

                deleteBtn.setOnAction(e -> {
                    Book b = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Delete Book");
                    alert.setContentText("Delete " + b.getTitle() + "?");
                    alert.showAndWait().ifPresent(type -> {
                        if (type == ButtonType.OK) {
                            try {
                                dbManager.deleteBook(b.getId());
                                refreshTableData();
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

        booksTable.getColumns().addAll(idCol, titleCol, priceCol, stockCol, supplierIdCol, actionsCol);
    }

    // --- Logic Methods ---

    public void refreshTableData() {
        try {
            booksTable.setItems(FXCollections.observableArrayList(dbManager.getAllBooks()));
        } catch (SQLException e) {
            showError("Failed to load books: " + e.getMessage());
        }
    }

    private void handleSearch(String titleQuery, Supplier supplier) {
        String q = titleQuery == null ? "" : titleQuery.toLowerCase().trim();
        try {
            List<Book> all = dbManager.getAllBooks();
            List<Book> filtered = all.stream()
                    .filter(b -> q.isEmpty() || (b.getTitle() != null && b.getTitle().toLowerCase().contains(q)))
                    .filter(b -> supplier == null || b.getSupplierId() == supplier.getId())
                    .collect(Collectors.toList());
            booksTable.setItems(FXCollections.observableArrayList(filtered));
        } catch (SQLException ex) {
            showError("Search failed: " + ex.getMessage());
        }
    }

    // --- Dialogs (Simplified for brevity, but logic is moved here) ---
    
    private void showAddBookDialog() {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle("Add New Book");
        dialog.setHeaderText("Enter book details");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // 1. Create Controls 
        TextField titleField = new TextField();
        titleField.setPromptText("Title");

        TextField priceField = new TextField();
        priceField.setPromptText("Price (e.g. 12.50)");

        TextField stockField = new TextField();
        stockField.setPromptText("Stock quantity");

        ComboBox<Supplier> supplierCombo = createSupplierComboBox();

        // 2. Setup Layout using Helper 
        GridPane grid = createBookFormGrid(titleField, priceField, stockField, supplierCombo);
        dialog.getDialogPane().setContent(grid);

        // 3. Result Converter 
        dialog.setResultConverter(btn -> {
            if (btn == addButtonType) {
                if (!validateBookInput(titleField.getText(), priceField.getText(), stockField.getText())
                        || supplierCombo.getValue() == null) {
                    showError("Please fill Title/Price/Stock and choose a Supplier.");
                    return null;
                }
                return new Book(0, titleField.getText().trim(), 
                        Double.parseDouble(priceField.getText().trim()),
                        Integer.parseInt(stockField.getText().trim()), 
                        supplierCombo.getValue().getId());
            }
            return null;
        });

        // 4. Handle Result («·Õ›Ÿ ›Ì ﬁ«⁄œ… «·»Ì«‰« )
        dialog.showAndWait().ifPresent(book -> {
            try {
                dbManager.addBook(book);
                refreshTableData(); //  ÕœÌÀ «·ÃœÊ· ›ﬁÿ
                showSuccessMessage("Book added successfully.");
            } catch (SQLException e) {
                showError("Add failed: " + e.getMessage());
            }
        });
    }

    private void showEditBookDialog(Book book) {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle("Edit Book");
        dialog.setHeaderText("Edit: " + book.getTitle());

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // 1. Create Controls with existing data ( ⁄»∆… «·»Ì«‰«  «·ﬁœÌ„…)
        TextField titleField = new TextField(book.getTitle());
        TextField priceField = new TextField(String.valueOf(book.getPrice()));
        TextField stockField = new TextField(String.valueOf(book.getStockQuantity()));
        
        ComboBox<Supplier> supplierCombo = createSupplierComboBox();
        try {
            List<Supplier> allSup = dbManager.getAllSuppliers();
            supplierCombo.setItems(FXCollections.observableArrayList(allSup));
            supplierCombo.setValue(allSup.stream()
                    .filter(s -> s.getId() == book.getSupplierId())
                    .findFirst()
                    .orElse(null));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 2. Setup Layout
        GridPane grid = createBookFormGrid(titleField, priceField, stockField, supplierCombo);
        dialog.getDialogPane().setContent(grid);

        // 3. Result Converter
        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                if (!validateBookInput(titleField.getText(), priceField.getText(), stockField.getText())
                        || supplierCombo.getValue() == null) {
                    showError("Please enter valid Title/Price/Stock and select Supplier.");
                    return null;
                }
                // create updated Book object
                return new Book(book.getId(), titleField.getText().trim(),
                        Double.parseDouble(priceField.getText().trim()), 
                        Integer.parseInt(stockField.getText().trim()),
                        supplierCombo.getValue().getId());
            }
            return null;
        });

        // 4. Handle Result
        dialog.showAndWait().ifPresent(updated -> {
            try {
                dbManager.updateBook(updated);
                refreshTableData(); 
                showSuccessMessage("Book updated successfully.");
            } catch (SQLException e) {
                showError("Update failed: " + e.getMessage());
            }
        });
    }


    private GridPane createBookFormGrid(TextField title, TextField price, TextField stock, ComboBox<Supplier> supplier) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Title:"), 0, 0);
        grid.add(title, 1, 0);
        
        grid.add(new Label("Price:"), 0, 1);
        grid.add(price, 1, 1);
        
        grid.add(new Label("Stock:"), 0, 2);
        grid.add(stock, 1, 2);
        
        grid.add(new Label("Supplier:"), 0, 3);
        grid.add(supplier, 1, 3);
        
        return grid;
    }

    // œ«·… „”«⁄œ… ·≈‰‘«¡ «·ﬁ«∆„… «·„‰”œ·… ··„Ê—œÌ‰
    private ComboBox<Supplier> createSupplierComboBox() {
        ComboBox<Supplier> combo = new ComboBox<>();
        try {
            combo.setItems(FXCollections.observableArrayList(dbManager.getAllSuppliers()));
        } catch (SQLException e) {
            showError("Failed to load suppliers: " + e.getMessage());
        }
        combo.setPromptText("Select supplier");
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Supplier s) { return s != null ? s.getName() : ""; }
            @Override
            public Supplier fromString(String string) { return null; }
        });
        return combo;
    }

    private boolean validateBookInput(String title, String price, String stock) {
        if (title == null || title.trim().isEmpty()) return false;
        try {
            double p = Double.parseDouble(price);
            int s = Integer.parseInt(stock);
            return p >= 0 && s >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("alert-error");
        alert.showAndWait();
        
    }

    private void showSuccessMessage(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("alert-success");
        alert.showAndWait();
    }}