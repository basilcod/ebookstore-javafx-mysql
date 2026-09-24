package application;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class SupplierController {

    private DatabaseManager dbManager;
    private TableView<Supplier> suppliersTable;

    public SupplierController(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.suppliersTable = new TableView<>();
    }

    public VBox getView() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));

        Label title = new Label("Suppliers Management");
        title.getStyleClass().add("section-title");

        // --- Controls ---
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search supplier name...");

        Button addBtn = new Button("Add New Supplier");
        addBtn.getStyleClass().add("primary-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        controls.getChildren().addAll(new Label("Name:"), searchField, spacer, addBtn);

        // --- Table Setup ---
        setupTable();
        refreshTableData();

        // --- Event Handlers ---
        // ÇáÈÍË ÇáÊáÞÇÆí ÚäÏ ÇáßÊÇÈÉ (Live Search)
        searchField.textProperty().addListener((obs, oldV, newV) -> handleSearch(newV));

        addBtn.setOnAction(e -> showAddSupplierDialog());

        section.getChildren().addAll(title, controls, suppliersTable);
        return section;
    }

    private void setupTable() {
        suppliersTable.setPrefHeight(520);
        suppliersTable.getColumns().clear();

        TableColumn<Supplier, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(80);

        TableColumn<Supplier, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<Supplier, String> emailCol = new TableColumn<>("Email/Contact");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(220);

        TableColumn<Supplier, String> phoneCol = new TableColumn<>("Phone/Address");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        phoneCol.setPrefWidth(200);

        TableColumn<Supplier, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(180);

        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(8, editBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().add("edit-button");
                deleteBtn.getStyleClass().add("delete-button");

                editBtn.setOnAction(e -> {
                    Supplier s = getTableView().getItems().get(getIndex());
                    showEditSupplierDialog(s);
                });

                deleteBtn.setOnAction(e -> {
                    Supplier s = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Delete Supplier");
                    alert.setContentText("Delete supplier: " + s.getName() + "?");
                    
                    alert.showAndWait().ifPresent(type -> {
                        if (type == ButtonType.OK) {
                            try {
                                dbManager.deleteSupplier(s.getId());
                                refreshTableData();
                                showSuccessMessage("Supplier deleted successfully.");
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

        suppliersTable.getColumns().addAll(idCol, nameCol, emailCol, phoneCol, actionsCol);
    }

    public void refreshTableData() {
        try {
            suppliersTable.setItems(FXCollections.observableArrayList(dbManager.getAllSuppliers()));
        } catch (SQLException e) {
            showError("Failed to load suppliers: " + e.getMessage());
        }
    }

    private void handleSearch(String query) {
        String q = query == null ? "" : query.toLowerCase().trim();
        try {
            List<Supplier> all = dbManager.getAllSuppliers();
            List<Supplier> filtered = all.stream()
                    .filter(s -> q.isEmpty() || (s.getName() != null && s.getName().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
            suppliersTable.setItems(FXCollections.observableArrayList(filtered));
        } catch (SQLException ex) {
            showError("Search failed: " + ex.getMessage());
        }
    }

    // --- Dialogs ---

    private void showAddSupplierDialog() {
        Dialog<Supplier> dialog = new Dialog<>();
        dialog.setTitle("Add New Supplier");
        dialog.setHeaderText("Enter supplier details");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Controls
        TextField name = new TextField(); name.setPromptText("Name");
        TextField email = new TextField(); email.setPromptText("Contact info / email");
        TextField phone = new TextField(); phone.setPromptText("Address / phone");

        GridPane grid = createSupplierFormGrid(name, email, phone);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == addButtonType) {
                if (name.getText().trim().isEmpty()) {
                    showError("Supplier name is required.");
                    return null;
                }
                return new Supplier(0, name.getText().trim(), 
                        email.getText().trim(), phone.getText().trim());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(s -> {
            try {
                dbManager.addSupplier(s);
                refreshTableData();
                showSuccessMessage("Supplier added successfully.");
            } catch (SQLException e) {
                showError("Add failed: " + e.getMessage());
            }
        });
    }

    private void showEditSupplierDialog(Supplier supplier) {
        Dialog<Supplier> dialog = new Dialog<>();
        dialog.setTitle("Edit Supplier");
        dialog.setHeaderText("Edit: " + supplier.getName());

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Controls
        TextField name = new TextField(supplier.getName());
        TextField email = new TextField(supplier.getEmail());
        TextField phone = new TextField(supplier.getPhone());

        GridPane grid = createSupplierFormGrid(name, email, phone);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                if (name.getText().trim().isEmpty()) {
                    showError("Supplier name is required.");
                    return null;
                }
                return new Supplier(supplier.getId(), name.getText().trim(),
                        email.getText().trim(), phone.getText().trim());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            try {
                dbManager.updateSupplier(updated);
                refreshTableData();
                showSuccessMessage("Supplier updated successfully.");
            } catch (SQLException e) {
                showError("Update failed: " + e.getMessage());
            }
        });
    }

    // --- Helpers ---

    private GridPane createSupplierFormGrid(TextField name, TextField email, TextField phone) {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        
        grid.add(new Label("Name:"), 0, 0);            grid.add(name, 1, 0);
        grid.add(new Label("Contact:"), 0, 1);         grid.add(email, 1, 1);
        grid.add(new Label("Address/Phone:"), 0, 2);   grid.add(phone, 1, 2);
        
        return grid;
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