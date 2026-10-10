# EBookstore — JavaFX & MySQL

A Java desktop application for bookstore operations: books, suppliers, customers, orders, inventory changes, and sales reports. The source uses JavaFX for the interface and JDBC prepared statements for MySQL access.

**Source project:** the repository does not yet include a Maven/Gradle build or an executable SQL schema. Running it requires an IDE/dependency setup and a compatible database. It is not a one-command demo.

## Features in the source

- Book, customer, and supplier management screens.
- Cart-based order placement with order details and stock updates.
- Transaction commit/rollback for multi-step order operations.
- Inventory change logs and low-stock display.
- Sales totals, monthly revenue, and best-selling-book queries.

## Architecture

| Path | Responsibility |
| --- | --- |
| `src/application/EBookstoreMainApp.java` | Main JavaFX application and reporting UI |
| `src/application/*Controller.java` | Book, customer, supplier, inventory, and order UI helpers |
| `src/application/DatabaseManager.java` | JDBC queries and transaction boundaries |
| `src/application/Book.java`, `Customer.java`, `Order.java`, etc. | Data models |
| `src/resources/styles.css` | Shared application styling |
| `src/module-info.java` | Java module named `newbase` |
| `.env.example` | Environment-variable reference; not loaded automatically |

## Requirements

- JDK 19, matching the original project target described in this repository.
- JavaFX SDK compatible with the chosen JDK.
- MySQL Connector/J (the original setup used 9.5.0).
- A MySQL database prepared for the queries in `DatabaseManager.java`.

## IDE setup

1. Clone the repository and import `src` as a Java source folder.
2. Add JavaFX controls/FXML and MySQL Connector/J to your run configuration. The application declares module `newbase`; ensure the JDBC driver is resolved in your IDE’s module/classpath configuration.
3. Mark/copy CSS resources so `/resources/styles.css` is available at runtime. Preserve `application/application.css` for the alternate `Main` entry point.
4. Prepare a local database using the schema requirements below.
5. Set the three environment variables below in the IDE run configuration.
6. Run **`application.EBookstoreMainApp`**. `Main` is an alternate starter window; `lun` is a launcher helper.

| Variable | Example |
| --- | --- |
| `EBOOKSTORE_DB_URL` | `jdbc:mysql://localhost:3306/ebookstore` |
| `EBOOKSTORE_DB_USER` | `ebookstore_app` |
| `EBOOKSTORE_DB_PASSWORD` | Your local database password |

The application uses `System.getenv()`. Copying `.env.example` to `.env` alone does not configure Java. Do not commit real credentials.

## Database requirements

These names are taken from the current SQL queries; this is a compatibility checklist, **not a migration script**. Preserve table capitalization on case-sensitive MySQL installations.

| Table | Columns referenced by the application |
| --- | --- |
| `Book` | `book_id`, `title`, `price`, `stock_quantity`, `supplier_id` |
| `Customer` | `customer_id`, `first_name`, `email`, `address`, `phone`, `membership_type` |
| `Supplier` | `supplier_id`, `supplier_name`, `contact_info`, `address` |
| `Order` | `order_id`, `order_date`, `total_amount`, `status`, `customer_id`, `discount_id` |
| `OrderDetail` | `order_id`, `book_id`, `quantity`, `unit_price` |
| `inventory_log` | `log_id`, `book_id`, `update_date`, `quantity_change`, `reason` |

Generated identifiers, column types/defaults, foreign keys, and delete behavior must be defined consistently before running. `Order` is quoted with backticks in the SQL because it is a reserved word. Supplier UI fields named email/phone map to database `contact_info`/`address` respectively.

## Manual verification once configured

Use a disposable local database. Create a supplier, a book with initial stock, and a customer. Place a small order and inspect its header/details, stock change, inventory log, and sales report. Force a failing database operation to check rollback before relying on transaction behavior.

## Current boundaries and next steps

- Add a reproducible build and a reviewed SQL schema/seed dataset first.
- Add database integration tests; the repository currently has no automated test suite.
- Stock deduction queries do not enforce sufficient stock atomically; concurrent orders need server/database validation.
- Currency is represented with `double` in the Java model; use an explicit decimal-money policy for financial accuracy.
- Some database work runs on UI event paths; background execution would improve responsiveness.

This source review does not certify a successful JavaFX/MySQL end-to-end run.
