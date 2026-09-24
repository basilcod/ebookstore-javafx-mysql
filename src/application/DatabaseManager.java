package application;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import com.ebookstore.EBookstoreMainApp.SalesRecord;
//import com.ebookstore.EBookstoreMainApp.SalesSummary;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DatabaseManager {

    private static final String DB_URL = System.getenv("EBOOKSTORE_DB_URL");
    private static final String DB_USER = System.getenv("EBOOKSTORE_DB_USER");
    private static final String DB_PASSWORD = System.getenv("EBOOKSTORE_DB_PASSWORD");


    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // ------------------------------------------------------------
    // BOOK
    // Schema: Book(book_id, title, price, stock_quantity, book_type, category_id, supplier_id)
    // ------------------------------------------------------------
    public List<Book> getAllBooks() throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT book_id, title, price, stock_quantity, supplier_id FROM Book";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getDouble("price"),
                        rs.getInt("stock_quantity"),
                        rs.getInt("supplier_id")
                ));
            }
        }
        return books;
    }

    public void addBook(Book book) throws SQLException {
        String insertBookSQL = "INSERT INTO Book (title, price, stock_quantity, supplier_id) VALUES (?, ?, ?, ?)";
        String insertLogSQL = "INSERT INTO inventory_log (book_id, update_date, quantity_change, reason) VALUES (?, NOW(), ?, ?)";

        Connection conn = null;
        PreparedStatement psBook = null;
        PreparedStatement psLog = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);// Start Transaction

            psBook = conn.prepareStatement(insertBookSQL, Statement.RETURN_GENERATED_KEYS);
            psBook.setString(1, book.getTitle());
            psBook.setDouble(2, book.getPrice());
            psBook.setInt(3, book.getStockQuantity());
            psBook.setInt(4, book.getSupplierId());
            psBook.executeUpdate();

            int newBookId = 0;
            try (ResultSet keys = psBook.getGeneratedKeys()) {
                if (keys.next()) {
                    newBookId = keys.getInt(1);
                    book.setId(newBookId); 
                } else {
                    throw new SQLException("Failed to create book, no ID obtained.");
                }
            }

            psLog = conn.prepareStatement(insertLogSQL);
            psLog.setInt(1, newBookId);
            psLog.setInt(2, book.getStockQuantity()); 
            psLog.setString(3, "Initial Stock - New Book"); 
            psLog.executeUpdate();

            conn.commit(); 

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (psBook != null) psBook.close();
            if (psLog != null) psLog.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public void updateBook(Book book) throws SQLException {
        String sql = "UPDATE Book SET title = ?, price = ?, stock_quantity = ?, supplier_id = ? WHERE book_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, book.getTitle());
            pstmt.setDouble(2, book.getPrice());
            pstmt.setInt(3, book.getStockQuantity());
            pstmt.setInt(4, book.getSupplierId());
            pstmt.setInt(5, book.getId());
            pstmt.executeUpdate();
        }
    }

    public void updateBookStock(int bookId, int newStockQuantity) throws SQLException {
        String sql = "UPDATE Book SET stock_quantity = ? WHERE book_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newStockQuantity);
            pstmt.setInt(2, bookId);
            pstmt.executeUpdate();
        }
    }

    public void deleteBook(int bookId) throws SQLException {
    	// check if book has inventory log entries
        String checkSql = "SELECT COUNT(*) FROM inventory_log WHERE book_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setInt(1, bookId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("Cannot delete book: it has inventory log entries.");
                }
            }
        }

        String sql = "DELETE FROM Book WHERE book_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bookId);
            pstmt.executeUpdate();
        }
    }

    // ------------------------------------------------------------
    // CUSTOMER
    // Schema: Customer(customer_id, first_name, email, address, phone, membership_type, loyalty_points)
    // ------------------------------------------------------------
    public List<Customer> getAllCustomers() throws SQLException {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT customer_id, first_name, email, address, phone, membership_type FROM Customer";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                customers.add(new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("first_name"),
                        rs.getString("email"),
                        rs.getString("address"),
                        rs.getString("phone"),
                        rs.getString("membership_type")
                ));
            }
        }
        return customers;
    }

    public void addCustomer(Customer customer) throws SQLException {
        String sql = "INSERT INTO Customer (first_name, email, address, phone, membership_type) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, customer.getFirstName());
            pstmt.setString(2, customer.getEmail());
            pstmt.setString(3, customer.getAddress());
            pstmt.setString(4, customer.getPhone());
            pstmt.setString(5, customer.getMembershipType());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) customer.setId(keys.getInt(1));
            }
        }
    }

    public void updateCustomer(Customer customer) throws SQLException {
        String sql = "UPDATE Customer SET first_name = ?, email = ?, address = ?, phone = ?, membership_type = ? " +
                     "WHERE customer_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, customer.getFirstName());
            pstmt.setString(2, customer.getEmail());
            pstmt.setString(3, customer.getAddress());
            pstmt.setString(4, customer.getPhone());
            pstmt.setString(5, customer.getMembershipType());
            pstmt.setInt(6, customer.getId());
            pstmt.executeUpdate();
        }
    }

    public void deleteCustomer(int customerId) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM `Order` WHERE customer_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setInt(1, customerId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("Cannot delete customer: they have orders.");
                }
            }
        }

        String sql = "DELETE FROM Customer WHERE customer_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, customerId);
            pstmt.executeUpdate();
        }
    }

    // ------------------------------------------------------------
    // SUPPLIER
    // Schema: Supplier(supplier_id, supplier_name, contact_info, address)
    // NOTE: mapping contact_info -> Supplier.email , address -> Supplier.phone (UI compatibility)
    // ------------------------------------------------------------
    public List<Supplier> getAllSuppliers() throws SQLException {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT supplier_id, supplier_name, contact_info, address FROM Supplier";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                suppliers.add(new Supplier(
                        rs.getInt("supplier_id"),
                        rs.getString("supplier_name"),
                        rs.getString("contact_info"), // email field in class
                        rs.getString("address")       // phone field in class
                ));
            }
        }
        return suppliers;
    }

    public void addSupplier(Supplier supplier) throws SQLException {
        String sql = "INSERT INTO Supplier (supplier_name, contact_info, address) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, supplier.getName());
            pstmt.setString(2, supplier.getEmail()); // stored in contact_info
            pstmt.setString(3, supplier.getPhone()); // stored in address
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) supplier.setId(keys.getInt(1));
            }
        }
    }

    public void updateSupplier(Supplier supplier) throws SQLException {
        String sql = "UPDATE Supplier SET supplier_name = ?, contact_info = ?, address = ? WHERE supplier_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, supplier.getName());
            pstmt.setString(2, supplier.getEmail());
            pstmt.setString(3, supplier.getPhone());
            pstmt.setInt(4, supplier.getId());
            pstmt.executeUpdate();
        }
    }

    public void deleteSupplier(int supplierId) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM Book WHERE supplier_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setInt(1, supplierId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("Cannot delete supplier: they have associated books.");
                }
            }
        }

        String sql = "DELETE FROM Supplier WHERE supplier_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, supplierId);
            pstmt.executeUpdate();
        }
    }

    // ------------------------------------------------------------
    // ORDER
    // Schema: Order(order_id, order_date, total_amount, status, customer_id, discount_id)
    // NOTE: No OrderDetails in your schema, so order is header-only.
    // ------------------------------------------------------------
    public List<Order> getAllOrders() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT order_id, customer_id, order_date, total_amount FROM `Order` ORDER BY order_date DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("order_date");
                String dateStr = (ts != null) ? ts.toLocalDateTime().toString() : "";
                orders.add(new Order(
                        rs.getInt("order_id"),
                        rs.getInt("customer_id"),
                        dateStr,
                        rs.getDouble("total_amount")
                ));
            }
        }
        return orders;
    }

    public void addOrder(Order order) throws SQLException {
        String sql = "INSERT INTO `Order` (order_date, total_amount, status, customer_id, discount_id) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // إذا orderDate فارغ، خليه NOW
            Timestamp orderTs;
            try {
                // expecting something like 2025-12-17T10:20:30
                orderTs = Timestamp.valueOf(order.getOrderDate().replace('T', ' '));
            } catch (Exception e) {
                orderTs = new Timestamp(System.currentTimeMillis());
            }

            pstmt.setTimestamp(1, orderTs);
            pstmt.setDouble(2, order.getTotalAmount());
            pstmt.setString(3, "New");
            pstmt.setInt(4, order.getCustomerId());
            pstmt.setNull(5, Types.INTEGER);

            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) order.setId(keys.getInt(1));
            }
        }
    }
    public void addOrder(Order order, String status, Integer discountId) throws SQLException {
        String sql = "INSERT INTO `Order` (order_date, total_amount, status, customer_id, discount_id) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // 1) order_date
            Timestamp orderTs;
            try {
                // expecting: 2025-12-17T10:20:30  -> convert T to space
                String dateStr = order.getOrderDate();
                orderTs = Timestamp.valueOf(dateStr.replace('T', ' '));
            } catch (Exception e) {
                orderTs = new Timestamp(System.currentTimeMillis());
            }
            pstmt.setTimestamp(1, orderTs);

            // 2) total_amount
            pstmt.setDouble(2, order.getTotalAmount());

            // 3) status (إذا فاضي خليه "Pending")
            String finalStatus = (status == null || status.trim().isEmpty()) ? "Pending" : status.trim();
            pstmt.setString(3, finalStatus);

            // 4) customer_id
            pstmt.setInt(4, order.getCustomerId());

            // 5) discount_id (nullable)
            if (discountId == null) {
                pstmt.setNull(5, Types.INTEGER);
            } else {
                pstmt.setInt(5, discountId);
            }

            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    order.setId(keys.getInt(1));
                }
            }
        }
    }


    public void deleteOrder(int orderId) throws SQLException {
        String sql = "DELETE FROM `Order` WHERE order_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            pstmt.executeUpdate();
        }
    }

    // ------------------------------------------------------------
    // INVENTORY LOG
    // Schema: inventory_log(log_id, update_date, quantity_change, reason, book_id)
    // ------------------------------------------------------------
    public void addInventoryUpdate(InventoryUpdate update, String reason) throws SQLException {
        String sql = "INSERT INTO inventory_log (book_id, update_date, quantity_change, reason) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, update.getBookId());
            pstmt.setTimestamp(2, java.sql.Timestamp.valueOf(update.getUpdateDate())); 
            // If updateDate is String like "2025-12-17T20:10:00", Timestamp.valueOf works.

            pstmt.setInt(3, update.getQuantityChange());
            pstmt.setString(4, reason);

            pstmt.executeUpdate();
        }
    }


    public List<InventoryUpdate> getAllInventoryUpdates() throws SQLException {
        List<InventoryUpdate> updates = new ArrayList<>();
        String sql = "SELECT log_id, book_id, update_date, quantity_change FROM inventory_log ORDER BY update_date DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("update_date");
                String dateStr = (ts != null) ? ts.toLocalDateTime().toString() : "";
                updates.add(new InventoryUpdate(
                        rs.getInt("log_id"),
                        rs.getInt("book_id"),
                        dateStr,
                        rs.getInt("quantity_change")
                ));
            }
        }
        return updates;
    }

    // ------------------------------------------------------------
    // REPORTING (based on Order header only)
    // ------------------------------------------------------------
    public application.EBookstoreMainApp.SalesSummary getSalesSummary(LocalDate fromDate, LocalDate toDate) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_amount),0) AS total_sales, COUNT(*) AS total_orders " +
                     "FROM `Order` WHERE DATE(order_date) BETWEEN ? AND ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(fromDate));
            stmt.setDate(2, java.sql.Date.valueOf(toDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double totalSales = rs.getDouble("total_sales");
                    int totalOrders = rs.getInt("total_orders");
                    // no OrderDetails => totalBooksSold unknown; return 0
                    return new EBookstoreMainApp.SalesSummary(totalSales, totalOrders, 0);
                }
            }
        }
        return new EBookstoreMainApp.SalesSummary(0.0, 0, 0);
    }

    public ObservableList<application.EBookstoreMainApp.SalesRecord> getSalesRecords(LocalDate fromDate, LocalDate toDate) throws SQLException {
        ObservableList<application.EBookstoreMainApp.SalesRecord> salesData = FXCollections.observableArrayList();

        String sql = "SELECT o.order_date, c.first_name, o.total_amount " +
                     "FROM `Order` o JOIN Customer c ON o.customer_id = c.customer_id " +
                     "WHERE DATE(o.order_date) BETWEEN ? AND ? " +
                     "ORDER BY o.order_date";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(fromDate));
            stmt.setDate(2, java.sql.Date.valueOf(toDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("order_date");
                    String dateStr = (ts != null) ? ts.toLocalDateTime().toString() : "";
                    salesData.add(new application.EBookstoreMainApp.SalesRecord(dateStr, rs.getString("first_name"), rs.getDouble("total_amount")));
                }
            }
        }
        return salesData;
    }
    // method to place order with deatails and stock update
    public void placeOrder(Order order, List<OrderDetail> details) throws SQLException {
        String insertOrderSQL = "INSERT INTO `Order` (order_date, total_amount, status, customer_id, discount_id) VALUES (?, ?, ?, ?, ?)";
        String insertDetailSQL = "INSERT INTO OrderDetail (order_id, book_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        String updateStockSQL = "UPDATE Book SET stock_quantity = stock_quantity - ? WHERE book_id = ?";
        
        Connection conn = null;
        PreparedStatement psOrder = null;
        PreparedStatement psDetail = null;
        PreparedStatement psStock = null;

        try {
            conn = getConnection();
            // start transaction
            conn.setAutoCommit(false);

            psOrder = conn.prepareStatement(insertOrderSQL, Statement.RETURN_GENERATED_KEYS);
            psOrder.setTimestamp(1, java.sql.Timestamp.valueOf(order.getOrderDate().replace("T", " ")));
            psOrder.setDouble(2, order.getTotalAmount());
            psOrder.setString(3, "Completed");
            psOrder.setInt(4, order.getCustomerId());
            
            psOrder.setObject(5, null); // discount as null for siplicity

            psOrder.executeUpdate();

            int newOrderId = 0;
            try (ResultSet keys = psOrder.getGeneratedKeys()) {
                if (keys.next()) {
                    newOrderId = keys.getInt(1);
                } else {
                    throw new SQLException("فشل إنشاء الطلب، لم يتم استرجاع ID.");
                }
            }
            
            //inter the details and update stock in batch mode
            psDetail = conn.prepareStatement(insertDetailSQL);
            psStock = conn.prepareStatement(updateStockSQL);

            for (OrderDetail item : details) {
                // inter the  Detail
                psDetail.setInt(1, newOrderId);
                psDetail.setInt(2, item.getBookId());
                psDetail.setInt(3, item.getQuantity());
                psDetail.setDouble(4, item.getUnitPrice());
                psDetail.addBatch(); // add to batch
                // refresh the stock
                psStock.setInt(1, item.getQuantity());
                psStock.setInt(2, item.getBookId());
                psStock.addBatch();
            }
            // do the patch execution 
            psDetail.executeBatch();
            psStock.executeBatch();

            // submit the transaction
            conn.commit();
            System.out.println("Transaction Committed Successfully!");

        } catch (SQLException e) {
        	// if any error occurs during transaction , rollback
            if (conn != null) {
                try {
                    System.err.println("Transaction failed! Rolling back...");
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e; // rethrow the exception for futher handling 
        } finally {
            // close resources
            if (psOrder != null) psOrder.close();
            if (psDetail != null) psDetail.close();
            if (psStock != null) psStock.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
                
            }
        }
    }
 // ============================================================
    // TRANSACTION: PLACE ORDER (Header + Details + Stock Update)
    // ============================================================
    public void placeOrder(int customerId, List<OrderDetail> cartItems) throws SQLException {
        double totalAmount = 0;
        for (OrderDetail item : cartItems) {
            totalAmount += item.getQuantity() * item.getUnitPrice();
        }

        String insertOrderSQL = "INSERT INTO `Order` (order_date, total_amount, status, customer_id) VALUES (NOW(), ?, 'Completed', ?)";
        String insertDetailSQL = "INSERT INTO OrderDetail (order_id, book_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        String updateStockSQL = "UPDATE Book SET stock_quantity = stock_quantity - ? WHERE book_id = ?";
        String logStockSQL = "INSERT INTO inventory_log (book_id, update_date, quantity_change, reason) VALUES (?, NOW(), ?, 'Sale Order')";

        Connection conn = null;
        PreparedStatement psOrder = null;
        PreparedStatement psDetail = null;
        PreparedStatement psStock = null;
        PreparedStatement psLog = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start Transaction

            // A. Insert Order Header
            psOrder = conn.prepareStatement(insertOrderSQL, Statement.RETURN_GENERATED_KEYS);
            psOrder.setDouble(1, totalAmount);
            psOrder.setInt(2, customerId);
            psOrder.executeUpdate();

            int newOrderId = 0;
            try (ResultSet keys = psOrder.getGeneratedKeys()) {
                if (keys.next()) newOrderId = keys.getInt(1);
                else throw new SQLException("Failed to create order ID.");
            }
            // B. Insert Details & Update Stock (Batch Processing)
            psDetail = conn.prepareStatement(insertDetailSQL);
            psStock = conn.prepareStatement(updateStockSQL);
            psLog = conn.prepareStatement(logStockSQL);
            // loop through cart items
            for (OrderDetail item : cartItems) {
                // Add Detail
                psDetail.setInt(1, newOrderId);
                psDetail.setInt(2, item.getBookId());
                psDetail.setInt(3, item.getQuantity());
                psDetail.setDouble(4, item.getUnitPrice());
                psDetail.addBatch();
                // Update Stock
                psStock.setInt(1, item.getQuantity());
                psStock.setInt(2, item.getBookId());
                psStock.addBatch();
                // Log Inventory Change
                psLog.setInt(1, item.getBookId());
                psLog.setInt(2, -item.getQuantity()); // Negative change
                psLog.addBatch();
            }
            psDetail.executeBatch();
            psStock.executeBatch();
            psLog.executeBatch();
            conn.commit(); // Commit Transaction

        } catch (SQLException e) {
            if (conn != null) conn.rollback(); // Rollback on error
            throw e;
        } finally {
            if (psOrder != null) psOrder.close();
            if (psDetail != null) psDetail.close();
            if (psStock != null) psStock.close();
            if (psLog != null) psLog.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
 // ============================================================
    // REPORT: GET BEST SELLING BOOKS
    // ============================================================
    public Map<String, Integer> getBestSellingBooks() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        // SQL to get top 5 best selling pooks based on OrderDetail
        String sql = "SELECT b.title, SUM(od.quantity) as total_sold " +
                     "FROM OrderDetail od " +
                     "JOIN Book b ON od.book_id = b.book_id " +
                     "GROUP BY b.title " +
                     "ORDER BY total_sold DESC " +
                     "LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String title = rs.getString("title");
                int total = rs.getInt("total_sold");
                stats.put(title, total);
            }
        }
        return stats;
    }

    public Map<String, Double> getMonthlySalesStats() throws SQLException {
        Map<String, Double> stats = new java.util.LinkedHashMap<>(); 
        // Monthly Stats Chart for last 6 months
        String query = "SELECT DATE_FORMAT(order_date, '%b') as month, SUM(total_amount) as revenue " +
                       "FROM `Order` " +
                       "WHERE order_date >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH) " +
                       "GROUP BY DATE_FORMAT(order_date, '%Y-%m'), month " +
                       "ORDER BY MIN(order_date) ASC";
                       
        try (
        		Connection conn = getConnection();
        		Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                stats.put(rs.getString("month"), rs.getDouble("revenue"));
            }
        }
        return stats;
    }
}
