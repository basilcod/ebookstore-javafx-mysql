# EBookstore

A JavaFX desktop app for managing a bookstore with a MySQL database.

## Features

- Manage books, suppliers, customers, and orders.
- Track stock changes and show low stock alerts.
- View sales, best sellers, and customer reports.

## Requirements

- JDK 19 (the original Eclipse project targets JavaSE-19)
- JavaFX SDK
- MySQL Connector/J (the original project used version 9.5.0)
- A compatible MySQL database

## Setup

1. Import `src` into a Java project in Eclipse or another Java IDE.
2. Add JavaFX and MySQL Connector/J to the project's dependencies.
3. Set these environment variables in the run configuration:
   - `EBOOKSTORE_DB_URL`: a JDBC URL for your MySQL database.
   - `EBOOKSTORE_DB_USER`: your database user.
   - `EBOOKSTORE_DB_PASSWORD`: your database password.
4. Run `application.EBookstoreMainApp`.

The original project directory did not include a SQL schema or migration files. A compatible database must be prepared separately. Do not commit database credentials.
