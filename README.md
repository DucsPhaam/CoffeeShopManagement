# ☕ CoffeeShop Management System

A complete database schema for a real-world coffee shop management system. It supports sales operations, staff management, inventory tracking, expense logging, reporting, and transaction history. Designed to handle both **dine-in and takeaway workflows**.

---

## 📌 Core Modules

| Module          | Description                                                                |
|-----------------|----------------------------------------------------------------------------|
| 👤 Users         | User accounts with roles (manager / staff), login status tracking          |
| 🕒 Shifts        | Work shift logging (check-in, check-out, break status)                     |
| 🍽️ Tables       | Table management by floor: available / occupied / reserved / cleaning      |
| ☕ Products      | Fixed product list (coffee drinks) — **kept unchanged**                    |
| 📦 Inventory     | Ingredient tracking with low-stock alert support                          |
| 🔗 Ingredients   | Recipe mapping between products and ingredients                            |
| 🧾 Orders        | Order management (dine-in / takeaway, paid / unpaid)                      |
| 🛒 Order Items   | Order details per product                                                  |
| 💵 Payments      | Final payment record with VAT and change returned                          |
| 📚 Transactions  | Income / Expense logging (cash in-out)                                    |

---

## 🏗️ Database Structure
```
coffeeshop
│
├── users → User accounts
├── user_shifts → Staff shift tracking
├── tables → Physical table list
├── products → Product catalog (unchanged)
├── inventory → Ingredient stock
├── product_ingredients → Recipe mapping
├── orders → Orders
├── order_items → Order line items
├── payments → Payment records
└── transactions → Income / Expense log
```

---

## 🧪 Sample Data Included

- Predefined User Accounts:

| Username | Password | Role    |
|----------|----------|---------|
| admin    | 123      | manager |
| staff1   | 123      | staff   |
| staff2   | 123      | staff   |
| (Optional) staff3, staff4 — can be added |

- Tables across **3 floors, 10 tables each**
- Coffee product list already set
- Ingredient recipes linked via `product_ingredients`
- Includes **1 initial order + payment + transaction**

---

## ⚙️ Setup Guide

### 1️⃣ Import the SQL File
SOURCE coffeeshop.sql;
### 2️⃣ Configure Database Connection
```
DB_HOST=localhost
DB_NAME=coffeeshop
DB_USER=your_user
DB_PASS=your_pass
```
### 3️⃣ Configure JVM Run Options (IntelliJ IDEA)
Go to:
```Run → Edit Configurations → VM Options```

Then add:
```--module-path "path\to\CoffeeShopManagement\lib" --add-modules javafx.controls,javafx.fxml``` </br>
eg. ```--module-path "E:\CoffeeShopManagement\lib" --add-modules javafx.controls,javafx.fxml``` </br>

(Note: The lib folder is already included in the Project. Use Oracle OpenJDK 20.0.2 as the SDK.)

## Contact
> For support or questions, feel free to contact ducsphaam@gmail.com.</br>
> Facebook: [Đức Phạm](https://www.facebook.com/cerfx/)</br>
> Youtube: [DucsPhaam](https://www.youtube.com/@ducsphaam)</br>

## Special thanks for the collaboration:
> [bakugo2234](https://github.com/bakugo2234)</br>
> [kitty1657](https://github.com/kitty1657)</br>
> [TrungVD3006](https://github.com/TrungVD3006)</br>

**© Copyright**
