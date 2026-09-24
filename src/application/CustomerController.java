package application;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CustomerController {

    private DatabaseManager dbManager;
    private TableView<Customer> customersTable;

    public CustomerController(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.customersTable = new TableView<>();
    }

    public VBox getView() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));

        Label title = new Label("Customers Management");
        title.getStyleClass().add("section-title");

        // --- Controls & Filters ---
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name...");
        searchField.setPrefWidth(200);

        ComboBox<String> membershipFilter = new ComboBox<>();
        membershipFilter.setPromptText("Membership Type");
        membershipFilter.getItems().addAll("All", "Regular", "Gold", "Premium", "VIP");
        membershipFilter.setPrefWidth(150);

        Button searchBtn = new Button("Search");
        Button clearBtn = new Button("Clear");
        Button addBtn = new Button("Add New Customer");
        addBtn.getStyleClass().add("primary-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        controls.getChildren().addAll(new Label("Name:"), searchField, 
                                      new Label("Membership:"), membershipFilter,
                                      searchBtn, clearBtn, spacer, addBtn);

        // --- Table Setup ---
        setupTable();
        refreshTableData();

        // --- Event Handlers ---
        searchBtn.setOnAction(e -> handleSearch(searchField.getText(), membershipFilter.getValue()));
        // Live search as user types
        searchField.textProperty().addListener((obs, oldVal, newVal) -> 
            handleSearch(newVal, membershipFilter.getValue())
        );

        clearBtn.setOnAction(e -> {
            searchField.clear();
            membershipFilter.setValue(null);
            refreshTableData();
        });

        addBtn.setOnAction(e -> showAddCustomerDialog());

        section.getChildren().addAll(title, controls, customersTable);
        return section;
    }

    private void setupTable() {
        customersTable.setPrefHeight(520);
        customersTable.getColumns().clear();

        TableColumn<Customer, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(60);

        TableColumn<Customer, String> nameCol = new TableColumn<>("First Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        nameCol.setPrefWidth(160);

        TableColumn<Customer, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(220);

        TableColumn<Customer, String> addressCol = new TableColumn<>("Address");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        addressCol.setPrefWidth(200);

        TableColumn<Customer, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        phoneCol.setPrefWidth(120);

        TableColumn<Customer, String> membershipCol = new TableColumn<>("Membership");
        membershipCol.setCellValueFactory(new PropertyValueFactory<>("membershipType"));
        membershipCol.setPrefWidth(100);

        TableColumn<Customer, Void> actionsCol = new TableColumn<>("Actions");
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
                    Customer c = getTableView().getItems().get(getIndex());
                    showEditCustomerDialog(c);
                });

                deleteBtn.setOnAction(e -> {
                    Customer c = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Delete Customer");
                    alert.setHeaderText("Confirm Deletion");
                    alert.setContentText("Are you sure you want to delete: " + c.getFirstName() + "?");
                    
                    alert.showAndWait().ifPresent(type -> {
                        if (type == ButtonType.OK) {
                            try {
                                dbManager.deleteCustomer(c.getId());
                                refreshTableData();
                                showSuccessMessage("Customer deleted successfully.");
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

        customersTable.getColumns().addAll(idCol, nameCol, emailCol, addressCol, phoneCol, membershipCol, actionsCol);
    }

    public void refreshTableData() {
        try {
            customersTable.setItems(FXCollections.observableArrayList(dbManager.getAllCustomers()));
        } catch (SQLException e) {
            showError("Failed to load customers: " + e.getMessage());
        }
    }

    private void handleSearch(String nameQuery, String membershipType) {
        String q = nameQuery == null ? "" : nameQuery.toLowerCase().trim();
        try {
            List<Customer> all = dbManager.getAllCustomers();
            List<Customer> filtered = all.stream()
                    .filter(c -> q.isEmpty() || (c.getFirstName() != null && c.getFirstName().toLowerCase().contains(q)))
                    .filter(c -> membershipType == null || membershipType.equals("All")
                            || (c.getMembershipType() != null && c.getMembershipType().equals(membershipType)))
                    .collect(Collectors.toList());
            customersTable.setItems(FXCollections.observableArrayList(filtered));
        } catch (SQLException ex) {
            showError("Search failed: " + ex.getMessage());
        }
    }

    // --- Dialogs ---

    private void showAddCustomerDialog() {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Add New Customer");
        dialog.setHeaderText("Enter customer details");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // UI Controls
        TextField firstName = new TextField(); firstName.setPromptText("First name");
        TextField email = new TextField(); email.setPromptText("Email");
        TextField address = new TextField(); address.setPromptText("Address");
        TextField phone = new TextField(); phone.setPromptText("Phone");
        ComboBox<String> membership = new ComboBox<>(FXCollections.observableArrayList("Regular", "Gold", "Premium", "VIP"));
        membership.setPromptText("Select Membership");

        GridPane grid = createCustomerFormGrid(firstName, email, address, phone, membership);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == addButtonType) {
                if (isInvalid(firstName, email, membership)) {
                    showError("First name, email, and membership are required.");
                    return null;
                }
                return new Customer(0, firstName.getText().trim(), email.getText().trim(),
                        address.getText().trim(), phone.getText().trim(), membership.getValue());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(c -> {
            try {
                dbManager.addCustomer(c);
                refreshTableData();
                showSuccessMessage("Customer added successfully.");
            } catch (SQLException e) {
                showError("Add failed: " + e.getMessage());
            }
        });
    }

    private void showEditCustomerDialog(Customer customer) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Edit Customer");
        dialog.setHeaderText("Edit: " + customer.getFirstName());

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Pre-fill data
        TextField firstName = new TextField(customer.getFirstName());
        TextField email = new TextField(customer.getEmail());
        TextField address = new TextField(customer.getAddress());
        TextField phone = new TextField(customer.getPhone());
        ComboBox<String> membership = new ComboBox<>(FXCollections.observableArrayList("Regular", "Gold", "Premium", "VIP"));
        membership.setValue(customer.getMembershipType());

        GridPane grid = createCustomerFormGrid(firstName, email, address, phone, membership);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                if (isInvalid(firstName, email, membership)) {
                    showError("First name, email, and membership are required.");
                    return null;
                }
                return new Customer(customer.getId(), firstName.getText().trim(), email.getText().trim(),
                        address.getText().trim(), phone.getText().trim(), membership.getValue());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            try {
                dbManager.updateCustomer(updated);
                refreshTableData();
                showSuccessMessage("Customer updated successfully.");
            } catch (SQLException e) {
                showError("Update failed: " + e.getMessage());
            }
        });
    }

    // --- Helpers ---

    private GridPane createCustomerFormGrid(TextField fn, TextField em, TextField ad, TextField ph, ComboBox<String> mem) {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        
        grid.add(new Label("First name:"), 0, 0); grid.add(fn, 1, 0);
        grid.add(new Label("Email:"), 0, 1);      grid.add(em, 1, 1);
        grid.add(new Label("Address:"), 0, 2);    grid.add(ad, 1, 2);
        grid.add(new Label("Phone:"), 0, 3);      grid.add(ph, 1, 3);
        grid.add(new Label("Membership:"), 0, 4); grid.add(mem, 1, 4);
        return grid;
    }

    private boolean isInvalid(TextField fn, TextField em, ComboBox<String> mem) {
        return fn.getText().trim().isEmpty() || em.getText().trim().isEmpty() || mem.getValue() == null;
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
    }
}