# **Food Ordering System 🍲**

This is a comprehensive food ordering system developed as a university project for an Advanced Programming course. It features a RESTful API built with Java that serves different roles: **customers**, **sellers** (restaurants), **couriers**, and an **administrator**.

## **Features**

### **🧍 Customer**

* **User Management**: Register, log in, and manage user profiles.  
* **Browse & Order**: Browse restaurants, view menus, and place food orders.  
* **Favorites**: Mark and manage a list of favorite restaurants.  
* **Order History**: View past and current orders.  
* **Ratings & Reviews**: Rate and review completed orders.

### **🏪 Seller (Restaurant)**

* **Restaurant Management**: Register and manage restaurant information (name, address, logo, etc.).  
* **Menu Management**: Create and manage menus for each restaurant.  
* **Food Item Management**: Add, update, and delete food items from the menu.  
* **Order Fulfillment**: View and manage incoming orders from customers.

### **🛵 Courier**

* **Delivery Management**: View a list of available deliveries and accept jobs.  
* **Status Updates**: Update the status of a delivery from "accepted" to "delivered".  
* **Delivery History**: View a history of all completed deliveries.

### **👨‍💻 Admin**

* **User Approval**: Approve or reject new seller and courier registrations.  
* **Restaurant Approval**: Approve or reject new restaurant submissions.  
* **System-Wide Visibility**: View and manage all users, orders, and transactions in the system.

## **Getting Started**

To get a local copy up and running, follow these simple steps.

### **Prerequisites**

* **Java Development Kit (JDK)**: Version 21 or higher.  
* **Maven**: To manage project dependencies.  
* **MySQL**: A running instance of the MySQL database.

### **Installation**

1. **Clone the repo**  
   git clone https://github.com/fshahriari/food-ordering-system-using-java-and-restfull-api-back-end.git

2. **Database Setup**  
   * Create a MySQL database named snappfood.  
   * Update the database credentials in src/main/java/com/snappfood/database/DatabaseManager.java.  
3. **Run the application**  
   * The main entry point of the application is the Server class. Run the main method in src/main/java/com/snappfood/server/Server.java to start the server.

## **API Endpoints**

The API is structured around REST principles. Here is a summary of the available endpoints from the aut\_food.yaml file:

* **Authentication**: /auth/register, /auth/login, /auth/profile, /auth/logout  
* **Restaurants (Sellers)**: /restaurants, /restaurants/mine, /restaurants/{id}, /restaurants/{id}/item, /restaurants/{id}/menu  
* **Vendors (Customers)**: /vendors, /vendors/{id}  
* **Items (Customers)**: /items, /items/{id}  
* **Orders**: /orders, /orders/{id}, /orders/history  
* **Favorites**: /favorites, /favorites/{restaurantId}  
* **Ratings**: /ratings, /ratings/items/{order\_id}  
* **Deliveries (Couriers)**: /deliveries/available, /deliveries/{order\_id}, /deliveries/history  
* **Admin**: /admin/users, /admin/pending-users, /admin/pending-restaurants, /admin/orders

## **Dependencies**

This project relies on the following external libraries, as defined in the pom.xml file:

* **mysql-connector-java**: For connecting to the MySQL database.  
* **gson**: For JSON serialization and deserialization.  
* **jbcrypt**: For hashing passwords.  
* **HikariCP**: For high-performance JDBC connection pooling.  
* **slf4j-simple** and **slf4j-api**: For logging.

## **License**

This project is licensed under the **BSD 3-Clause License**. See the LICENSE file for more details.
