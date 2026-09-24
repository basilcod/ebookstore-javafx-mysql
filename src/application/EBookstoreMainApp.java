package application;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

public class EBookstoreMainApp extends Application {

    private DatabaseManager dbManager;
    private TabPane tabPane;

    // Controllers
    private BookController bookController;
    private CustomerController customerController;
    private SupplierController supplierController;
    private OrderController orderController;
    private InventoryController inventoryController;

    // Dashboard UI Components (Class-Level for Refresh Access)
    private Label totalBooksLabel = new Label("0");
    private Label totalCustomersLabel = new Label("0");
    private Label totalOrdersLabel = new Label("0");
    private Label monthlyRevenueLabel = new Label("$0.00");
    
    // *** NEW: Make these accessible for refreshing ***
    private TableView<Book> lowStockTable = new TableView<>();
    private Label lowStockTitle = new Label("Low Stock Alert");
    private javafx.scene.chart.BarChart<String, Number> salesChart;

    @Override
    public void start(Stage primaryStage) throws SQLException {
        // 1. Initialize Database & Controllers
        dbManager = new DatabaseManager();
        bookController = new BookController(dbManager);
        customerController = new CustomerController(dbManager);
        supplierController = new SupplierController(dbManager);
        orderController = new OrderController(dbManager);
        inventoryController = new InventoryController(dbManager);

        // 2. Setup Main Layout
        primaryStage.setTitle("E-Bookstore Management System (v2.0)");
        primaryStage.setMaximized(true);

        BorderPane root = new BorderPane();
        root.setTop(createHeader());
        
        tabPane = createMainTabs();
        root.setCenter(tabPane);
        
        root.setBottom(createStatusBar());

        // 3. Setup Scene
        Scene scene = new Scene(root, 1200, 800);
        try {
            if (getClass().getResource("/resources/styles.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
            }
        } catch (Exception ignored) {}

        primaryStage.setScene(scene);
        primaryStage.show();

        // 4. Initial Data Load
        refreshAllData();
    }

    // ---------------------------------------------------------
    // Layout Components
    // ---------------------------------------------------------

    private VBox createHeader() {
        VBox header = new VBox();
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #2c3e50;");

        Label title = new Label("E-Bookstore Management System");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        MenuBar menuBar = createMenuBar();
        header.getChildren().addAll(title, menuBar);
        return header;
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem refreshItem = new MenuItem("Refresh All Data");
        refreshItem.setOnAction(e -> refreshAllData());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> System.exit(0));
        fileMenu.getItems().addAll(refreshItem, new SeparatorMenuItem(), exitItem);
        
        // Reports Menu (Shortcuts)
        Menu reportsMenu = new Menu("Reports");
        MenuItem salesItem = new MenuItem("Sales Report");
        salesItem.setOnAction(e -> showSalesReport());
        MenuItem bestSellerItem = new MenuItem("Best Sellers");
        bestSellerItem.setOnAction(e -> showBestSellersReport());
        reportsMenu.getItems().addAll(salesItem, bestSellerItem);

        menuBar.getMenus().addAll(fileMenu, reportsMenu);
        return menuBar;
    }

    private TabPane createMainTabs() throws SQLException {
        TabPane pane = new TabPane();
        pane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Create Tabs from Controllers
        Tab dashboardTab = new Tab("Dashboard", createDashboard());
        Tab booksTab = new Tab("Books", bookController.getView());
        Tab customersTab = new Tab("Customers", customerController.getView());
        Tab ordersTab = new Tab("Orders", orderController.getView());
        Tab suppliersTab = new Tab("Suppliers", supplierController.getView());
        Tab inventoryTab = new Tab("Inventory", inventoryController.getView());
        Tab reportsTab = new Tab("Reports", createReportsSection());

        pane.getTabs().addAll(dashboardTab, booksTab, customersTab, ordersTab, suppliersTab, inventoryTab, reportsTab);

        // Add Listener to refresh specific tab when selected
        pane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == booksTab) bookController.refreshTableData();
            if (newTab == customersTab) customerController.refreshTableData();
            if (newTab == suppliersTab) supplierController.refreshTableData();
            if (newTab == ordersTab) orderController.refreshTableData();
            if (newTab == inventoryTab) inventoryController.refreshData();
            if (newTab == dashboardTab) refreshDashboardStats(); // This will now refresh the table too!
        });

        return pane;
    }

    private HBox createStatusBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(5));
        bar.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1 0 0 0;");
        Label status = new Label("Ready. Connected to Database.");
        Label date = new Label(LocalDate.now().toString());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bar.getChildren().addAll(status, spacer, date);
        return bar;
    }

    // ---------------------------------------------------------
    // Dashboard Logic
    // ---------------------------------------------------------

    private VBox createDashboard() throws SQLException {
        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(30));
        dashboard.getStyleClass().add("dashboard");

        Label title = new Label("System Dashboard");
        title.getStyleClass().add("section-title");
        playEntranceAnimation(title, 0);

        // --- 1. Top Cards (Stats) ---
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        VBox totalBooksCard = createStatCard("Total Books", totalBooksLabel.getText(), "card-blue");
        VBox totalCustomersCard = createStatCard("Total Customers", totalCustomersLabel.getText(), "card-green");
        VBox totalOrdersCard = createStatCard("Total Orders", totalOrdersLabel.getText(), "card-orange");
        VBox monthlyRevenueCard = createStatCard("Monthly Revenue", monthlyRevenueLabel.getText(), "card-purple");

        // Binding labels
        ((Label) totalBooksCard.getChildren().get(1)).textProperty().bind(totalBooksLabel.textProperty());
        ((Label) totalCustomersCard.getChildren().get(1)).textProperty().bind(totalCustomersLabel.textProperty());
        ((Label) totalOrdersCard.getChildren().get(1)).textProperty().bind(totalOrdersLabel.textProperty());
        ((Label) monthlyRevenueCard.getChildren().get(1)).textProperty().bind(monthlyRevenueLabel.textProperty());

        statsRow.getChildren().addAll(totalBooksCard, totalCustomersCard, totalOrdersCard, monthlyRevenueCard);
        
        playEntranceAnimation(totalBooksCard, 200);
        playEntranceAnimation(totalCustomersCard, 300);
        playEntranceAnimation(totalOrdersCard, 400);
        playEntranceAnimation(monthlyRevenueCard, 500);


        // --- 2. Middle Section (Charts & Tables) ---
        HBox middleSection = new HBox(20);
        middleSection.setPrefHeight(400);
        HBox.setHgrow(middleSection, Priority.ALWAYS);

        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        xAxis.setLabel("Month");
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        yAxis.setLabel("Revenue ($)");

        javafx.scene.chart.BarChart<String, Number> salesChart = new javafx.scene.chart.BarChart<>(xAxis, yAxis);
        salesChart.setTitle("Sales Overview (Last 6 Months)");
        salesChart.setLegendVisible(false);

        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Revenue");
        
        try {
            Map<String, Double> realData = dbManager.getMonthlySalesStats();

            java.time.YearMonth currentMonth = java.time.YearMonth.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM", java.util.Locale.ENGLISH);

            for (int i = 5; i >= 0; i--) {
                java.time.YearMonth monthToCheck = currentMonth.minusMonths(i);
                String monthName = monthToCheck.format(formatter); // مثلاً: "Aug", "Sep"

                Double revenue = realData.getOrDefault(monthName, 0.0);
                
                series.getData().add(new javafx.scene.chart.XYChart.Data<>(monthName, revenue));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        salesChart.getData().add(series);
        salesChart.setPrefWidth(600);
        
        playEntranceAnimation(salesChart, 600);


        VBox lowStockBox = new VBox(10);
        lowStockBox.setPrefWidth(400);

        List<Book> allBooks = dbManager.getAllBooks();
        double avgStock = allBooks.stream().mapToInt(Book::getStockQuantity).average().orElse(0);
        double threshold = avgStock * 0.5;

        lowStockTitle.setText(String.format("⚠️ Low Stock Alert (<= %.1f)", threshold));
        lowStockTitle.setStyle("-fx-text-fill: #ff416c; -fx-font-weight: bold; -fx-font-size: 16px;");
        
        playPulseAnimation(lowStockTitle);

        lowStockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Book, String> titleCol = new TableColumn<>("Book");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        TableColumn<Book, Integer> stockCol = new TableColumn<>("Qty");
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        stockCol.setStyle("-fx-alignment: CENTER; -fx-text-fill: #ff416c; -fx-font-weight: bold;");
        lowStockTable.getColumns().setAll(titleCol, stockCol);

        List<Book> lowStockBooks = allBooks.stream()
                .filter(b -> b.getStockQuantity() <= threshold)
                .collect(Collectors.toList());
        lowStockTable.setItems(FXCollections.observableArrayList(lowStockBooks));

        lowStockBox.getChildren().addAll(lowStockTitle, lowStockTable);
        
        playEntranceAnimation(lowStockBox, 700);


        HBox.setHgrow(salesChart, Priority.ALWAYS);
        middleSection.getChildren().addAll(salesChart, lowStockBox);

        dashboard.getChildren().addAll(title, statsRow, middleSection);
        return dashboard;
    }

    private VBox createStatCard(String title, String value, String colorClass) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("stat-card", colorClass);
        card.setPadding(new Insets(20));
        card.setPrefWidth(220);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    // ---------------------------------------------------------
    // Reports Logic 
    // ---------------------------------------------------------

    private VBox createReportsSection() {
        VBox section = new VBox(20);
        section.setPadding(new Insets(20));

        Label title = new Label("Reports & Analytics");
        title.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(20);

        Button salesBtn = new Button("Sales Report (Date Range)");
        salesBtn.setPrefWidth(250);
        salesBtn.setOnAction(e -> showSalesReport());

        Button bestSellerBtn = new Button("Best Selling Books (Chart)");
        bestSellerBtn.setPrefWidth(250);
        bestSellerBtn.setOnAction(e -> showBestSellersReport());

        Button customerStatsBtn = new Button("Customer Membership Stats");
        customerStatsBtn.setPrefWidth(250);
        customerStatsBtn.setOnAction(e -> showCustomerStatsReport());

        Button lowStockBtn = new Button("Low Stock Alert");
        lowStockBtn.setPrefWidth(250);
        lowStockBtn.setOnAction(e -> {
            try { showLowStockReport(); } 
            catch (SQLException e1) { e1.printStackTrace(); }
        });

        grid.add(new Label("Sales & Revenue:"), 0, 0);
        grid.add(salesBtn, 0, 1);
        grid.add(bestSellerBtn, 0, 2);

        grid.add(new Label("Inventory & Customers:"), 1, 0);
        grid.add(lowStockBtn, 1, 1);
        grid.add(customerStatsBtn, 1, 2);

        section.getChildren().addAll(title, grid);
        return section;
    }

    private void showSalesReport() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Sales Report");
        dialog.setHeaderText("Select Date Range");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
        ButtonType genBtnType = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(genBtnType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        DatePicker from = new DatePicker(LocalDate.now().withDayOfMonth(1));
        DatePicker to = new DatePicker(LocalDate.now());
        grid.add(new Label("From:"), 0, 0); grid.add(from, 1, 0);
        grid.add(new Label("To:"), 0, 1);   grid.add(to, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == genBtnType && from.getValue() != null && to.getValue() != null) {
                generateSalesWindow(from.getValue(), to.getValue());
            }
            return null;
        });
        dialog.showAndWait();
    }
    
    private void generateSalesWindow(LocalDate from, LocalDate to) {
        Stage stage = new Stage();
        stage.setTitle("Sales Report: " + from + " - " + to);

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        try {
            SalesSummary summary = dbManager.getSalesSummary(from, to);
            ObservableList<SalesRecord> records = dbManager.getSalesRecords(from, to);

            Label sumLabel = new Label(String.format("Total Sales: $%.2f  |  Orders: %d", 
                    summary.getTotalSales(), summary.getTotalOrders()));
            sumLabel.getStyleClass().add("section-title"); 

            TableView<SalesRecord> table = new TableView<>(records);
            
            TableColumn<SalesRecord, String> dateCol = new TableColumn<>("Date");
            dateCol.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
            
            TableColumn<SalesRecord, String> custCol = new TableColumn<>("Customer");
            custCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
            
            TableColumn<SalesRecord, Double> amtCol = new TableColumn<>("Amount");
            amtCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

            table.getColumns().addAll(dateCol, custCol, amtCol);
            
            root.getChildren().addAll(sumLabel, table);
            
            Scene scene = new Scene(root, 600, 400);

            if (getClass().getResource("/resources/styles.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
            }

            stage.setScene(scene);
            stage.show();

        } catch (SQLException e) {
            showAlert("Error generating report: " + e.getMessage());
        }
    }
    
    private void showBestSellersReport() {
        try {
            Map<String, Integer> data = dbManager.getBestSellingBooks();
            if (data.isEmpty()) { showAlert("No sales data available."); return; }
            Stage stage = new Stage();
            stage.setTitle("Best Selling Books");
            PieChart pieChart = new PieChart();
            data.forEach((title, qty) -> pieChart.getData().add(new PieChart.Data(title, qty)));
            pieChart.setTitle("Top 5 Books");
            VBox root = new VBox(pieChart);
            root.setPadding(new Insets(20));
            stage.setScene(new Scene(root, 600, 500));
            stage.show();
        } catch (SQLException e) { showAlert("Error: " + e.getMessage()); }
    }
    
    private void showCustomerStatsReport() {
        try {
            List<Customer> customers = dbManager.getAllCustomers();
            Map<String, Long> stats = customers.stream()
                .collect(Collectors.groupingBy(c -> c.getMembershipType() == null ? "None" : c.getMembershipType(), Collectors.counting()));
            PieChart chart = new PieChart();
            stats.forEach((k, v) -> chart.getData().add(new PieChart.Data(k + " (" + v + ")", v)));
            chart.setTitle("Customer Memberships");
            Stage stage = new Stage();
            VBox root = new VBox(chart);
            stage.setScene(new Scene(root, 600, 500));
            stage.show();
        } catch(SQLException e) { e.printStackTrace(); }
    }

    private void showLowStockReport() throws SQLException {
        Stage stage = new Stage();
        stage.setTitle("Low Stock Report (Dynamic 50%)");
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        List<Book> allBooks = dbManager.getAllBooks();
        double averageStock = allBooks.stream().mapToInt(Book::getStockQuantity).average().orElse(0);
        double lowStockThreshold = averageStock * 0.5;
        List<Book> lowStockBooks = allBooks.stream().filter(b -> b.getStockQuantity() <= lowStockThreshold).collect(Collectors.toList());
        Label title = new Label(String.format("Books Below 50%% of Average (Threshold: %.1f)", lowStockThreshold));
        title.getStyleClass().add("section-title");
        TableView<Book> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(lowStockBooks));
        TableColumn<Book, Integer> idCol = new TableColumn<>("Book ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(100);
        TableColumn<Book, String> tCol = new TableColumn<>("Title");
        tCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        tCol.setPrefWidth(340);
        TableColumn<Book, Integer> sCol = new TableColumn<>("Stock");
        sCol.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        sCol.setPrefWidth(140);
        sCol.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        table.getColumns().addAll(idCol, tCol, sCol);
        root.getChildren().addAll(title, table);
        Scene scene = new Scene(root, 720, 520);
        try { if (getClass().getResource("/resources/styles.css") != null) { scene.getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm()); } } catch(Exception e) {}
        stage.setScene(scene);
        stage.show();
    }

    // ---------------------------------------------------------
    // Global Refresh & Helpers
    // ---------------------------------------------------------

    private void refreshAllData() {
        bookController.refreshTableData();
        customerController.refreshTableData();
        supplierController.refreshTableData();
        orderController.refreshTableData();
        inventoryController.refreshData();
        refreshDashboardStats();
    }

    private void refreshDashboardStats() {
        try {
            List<Book> books = dbManager.getAllBooks();
            List<Customer> customers = dbManager.getAllCustomers();
            List<Order> orders = dbManager.getAllOrders();

            totalBooksLabel.setText(String.valueOf(books.size()));
            totalCustomersLabel.setText(String.valueOf(customers.size()));
            totalOrdersLabel.setText(String.valueOf(orders.size()));

            // Calculate monthly revenue
            double revenue = orders.stream()
                .filter(o -> {
                    try {
                        String d = o.getOrderDate();
                        if (d.contains("T")) d = d.split("T")[0];
                        LocalDate ld = LocalDate.parse(d.split(" ")[0]);
                        return ld.getMonth() == LocalDate.now().getMonth() && 
                               ld.getYear() == LocalDate.now().getYear();
                    } catch(Exception e) { return false; }
                })
                .mapToDouble(Order::getTotalAmount)
                .sum();
            
            monthlyRevenueLabel.setText(String.format("$%.2f", revenue));
            
            // *** CRITICAL FIX: Refresh the Low Stock Table Here ***
            refreshLowStockTable();

        } catch (SQLException e) {
            System.err.println("Dashboard refresh failed: " + e.getMessage());
        }
    }
    
    // *** HELPER METHOD TO REFRESH THE TABLE DYNAMICALLY ***
    private void refreshLowStockTable() {
        try {
            List<Book> allBooks = dbManager.getAllBooks();
            
            // 1. Recalculate Average & Threshold
            double avgStock = allBooks.stream().mapToInt(Book::getStockQuantity).average().orElse(0);
            double threshold = avgStock * 0.5;

            // 2. Update Title Label
            if (lowStockTitle != null) {
                lowStockTitle.setText(String.format(" Low Stock Alert (<= %.1f)", threshold));
            }

            // 3. Filter Data
            List<Book> lowStockBooks = allBooks.stream()
                    .filter(b -> b.getStockQuantity() <= threshold)
                    .collect(Collectors.toList());

            // 4. Update Table Items
            if (lowStockTable != null) {
                lowStockTable.setItems(FXCollections.observableArrayList(lowStockBooks));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.show();
    }
    
    public static class SalesRecord {
        private String orderDate, customerName;
        private double amount;
        public SalesRecord(String d, String c, double a) { this.orderDate=d; this.customerName=c; this.amount=a; }
        public String getOrderDate() { return orderDate; }
        public String getCustomerName() { return customerName; }
        public double getAmount() { return amount; }
    }
    
    public static class SalesSummary {
        private double totalSales;
        private int totalOrders;
        private int totalBooksSold;
        public SalesSummary(double s, int o, int b) { this.totalSales=s; this.totalOrders=o; this.totalBooksSold=b; }
        public double getTotalSales() { return totalSales; }
        public int getTotalOrders() { return totalOrders; }
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    // 🔥 EXPLOSIVE ANIMATIONS SECTION 🔥
    // ---------------------------------------------------------

    private void playEntranceAnimation(javafx.scene.Node node, int delay) {
        node.setOpacity(0);
        node.setTranslateY(50);

        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(800), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        javafx.animation.TranslateTransition slide = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(800), node);
        slide.setFromY(50);
        slide.setToY(0);
        slide.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        javafx.animation.ParallelTransition transition = new javafx.animation.ParallelTransition(fade, slide);
        transition.setDelay(javafx.util.Duration.millis(delay));
        transition.play();
    }

    private void playPulseAnimation(javafx.scene.Node node) {
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(800), node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.1); 
        st.setToY(1.1);
        st.setAutoReverse(true); 
        st.setCycleCount(javafx.animation.Animation.INDEFINITE); 
        st.play();
    }
}