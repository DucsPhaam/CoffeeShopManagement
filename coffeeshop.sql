SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

-- Create database
DROP DATABASE IF EXISTS `coffeeshop`;
CREATE DATABASE `coffeeshop` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `coffeeshop`;

-- Users (Admin + Staff)
CREATE TABLE `users` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) UNIQUE NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  `role` ENUM('manager', 'staff') NOT NULL DEFAULT 'staff',
  `status` ENUM('online', 'offline') DEFAULT 'offline',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE `user_shifts` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME DEFAULT NULL,
  `status` ENUM('working', 'off-duty', 'on-break') DEFAULT 'off-duty',
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Tables
CREATE TABLE `tables` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(20) NOT NULL,
  `floor` INT NOT NULL DEFAULT 1,
  `seats` INT NOT NULL DEFAULT 2,
  `status` ENUM('available', 'occupied', 'reserved', 'cleaning') DEFAULT 'available'
) ENGINE=InnoDB;

-- Products / Drinks
CREATE TABLE `products` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `price` DECIMAL(10,2) NOT NULL,
  `drink_types` SET('hot','iced','frappe') NOT NULL,
  `image` VARCHAR(255) DEFAULT 'img/temp_icon.png'
) ENGINE=InnoDB;

-- Inventory (Nguyên liệu)
CREATE TABLE `inventory` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `unit` VARCHAR(20) DEFAULT 'kg',
  `cost_per_unit` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `quantity` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `min_stock` DECIMAL(10,2) DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Product Ingredients (Liên kết sản phẩm ↔ nguyên liệu)
CREATE TABLE `product_ingredients` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `product_id` INT NOT NULL,
  `inventory_id` INT NOT NULL,
  `quantity` DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (`product_id`) REFERENCES `products`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`inventory_id`) REFERENCES `inventory`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Orders
CREATE TABLE `orders` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `table_id` INT NULL,
  `staff_id` INT NOT NULL,
  `order_type` ENUM('dine-in','takeaway') NOT NULL,
  `status` ENUM('unpaid','paid') DEFAULT 'unpaid',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Order Items
CREATE TABLE `order_items` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `order_id` INT NOT NULL,
  `product_id` INT NOT NULL,
  `drink_type` ENUM('hot','iced','frappe') NOT NULL,
  `quantity` INT NOT NULL DEFAULT 1,
  `price` DECIMAL(10,2) NOT NULL,
  `note` VARCHAR(255) DEFAULT '',
  FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`product_id`) REFERENCES `products`(`id`)
) ENGINE=InnoDB;

-- Payments
CREATE TABLE `payments` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `order_id` INT NOT NULL,
  `total_price` DECIMAL(10,2) NOT NULL,
  `vat` DECIMAL(10,2) DEFAULT 0,
  `amount_received` DECIMAL(10,2) NOT NULL,
  `change_returned` DECIMAL(10,2) NOT NULL,
  `paid_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`)
) ENGINE=InnoDB;

-- Income/Expense Tracking
CREATE TABLE `transactions` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `type` ENUM('income','expense') NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `reason` VARCHAR(255),
  `created_by` INT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`created_by`) REFERENCES `users`(`id`)
) ENGINE=InnoDB;

-----------------------------
-- SAMPLE DATA
-----------------------------

-- Users (5 staff members + 1 manager)
INSERT INTO `users` (`id`, `username`, `password`, `role`, `status`, `created_at`) VALUES
(1, 'admin', '123', 'manager', 'offline', '2024-01-01 08:00:00'),
(2, 'staff1', '123', 'staff', 'offline', '2024-01-15 08:00:00'),
(3, 'staff2', '123', 'staff', 'offline', '2024-01-20 08:00:00'),
(4, 'mike_brown', '123', 'staff', 'offline', '2024-02-01 08:00:00'),
(5, 'sarah_wilson', '123', 'staff', 'offline', '2024-02-10 08:00:00'),
(6, 'david_lee', '123', 'staff', 'offline', '2024-03-01 08:00:00');

-- User Shifts (Recent shift history)
INSERT INTO `user_shifts` (`user_id`, `start_time`, `end_time`, `status`) VALUES
(2, '2025-10-14 08:00:00', '2025-10-14 16:00:00', 'off-duty'),
(3, '2025-10-14 14:00:00', '2025-10-14 22:00:00', 'off-duty'),
(4, '2025-10-15 08:00:00', '2025-10-15 16:00:00', 'off-duty'),
(5, '2025-10-15 14:00:00', '2025-10-15 22:00:00', 'off-duty'),
(2, '2025-10-16 08:00:00', NULL, 'working'),
(6, '2025-10-16 08:00:00', NULL, 'working');

-- Tables (Floor 1-3, 30 tables total)
INSERT INTO `tables` (`name`, `floor`, `seats`, `status`) VALUES
('T1-01',1,2,'available'),('T1-02',1,2,'available'),('T1-03',1,4,'occupied'),
('T1-04',1,4,'reserved'),('T1-05',1,6,'cleaning'),('T1-06',1,2,'available'),
('T1-07',1,4,'occupied'),('T1-08',1,4,'available'),('T1-09',1,6,'available'),
('T1-10',1,2,'reserved'),
('T2-01',2,2,'available'),('T2-02',2,2,'occupied'),('T2-03',2,4,'available'),
('T2-04',2,4,'reserved'),('T2-05',2,6,'cleaning'),('T2-06',2,2,'available'),
('T2-07',2,4,'available'),('T2-08',2,6,'occupied'),('T2-09',2,4,'cleaning'),
('T2-10',2,2,'available'),
('T3-01',3,2,'available'),('T3-02',3,4,'reserved'),('T3-03',3,6,'available'),
('T3-04',3,4,'available'),('T3-05',3,2,'occupied'),('T3-06',3,2,'cleaning'),
('T3-07',3,4,'available'),('T3-08',3,6,'available'),('T3-09',3,4,'reserved'),
('T3-10',3,2,'available');

-- Products (Keep existing)
INSERT INTO `products` (`id`, `name`, `price`, `drink_types`, `image`) VALUES
(1, 'Latte', 2.80, 'hot,iced,frappe', 'img/Latte.png'),
(2, 'Cappuccino', 2.50, 'hot,iced,frappe', 'img/Cappuccino.png'),
(3, 'Americano', 2.25, 'hot,iced', 'img/Americano.png'),
(4, 'Caramel Macchiato', 2.80, 'hot,iced,frappe', 'img/Macchiato.png'),
(5, 'Mocha', 2.15, 'hot,iced,frappe', 'img/Mocha.png'),
(6, 'Condensed Milk Coffee', 2.50, 'hot,iced', 'img/Milk.png');

-- Inventory (Extended list with costs)
INSERT INTO `inventory` (`id`, `name`, `unit`, `cost_per_unit`, `quantity`, `min_stock`) VALUES
(1, 'Coffee Beans', 'kg', 15.00, 50.00, 10.00),
(2, 'Milk', 'liter', 3.50, 40.00, 15.00),
(3, 'Sugar', 'kg', 2.00, 25.00, 5.00),
(4, 'Caramel Syrup', 'liter', 8.00, 12.00, 3.00),
(5, 'Chocolate Syrup', 'liter', 8.50, 10.00, 3.00),
(6, 'Whipped Cream', 'can', 4.50, 20.00, 5.00),
(7, 'Vanilla Syrup', 'liter', 7.50, 8.00, 2.00),
(8, 'Ice', 'kg', 0.50, 100.00, 20.00),
(9, 'Paper Cups (Small)', 'pack', 12.00, 15.00, 3.00),
(10, 'Paper Cups (Medium)', 'pack', 15.00, 12.00, 3.00),
(11, 'Paper Cups (Large)', 'pack', 18.00, 10.00, 3.00),
(12, 'Straws', 'pack', 5.00, 25.00, 5.00),
(13, 'Napkins', 'pack', 8.00, 20.00, 5.00);

-- Product Ingredients
INSERT INTO `product_ingredients` (`product_id`, `inventory_id`, `quantity`) VALUES
-- Latte
(1, 1, 0.03), (1, 2, 0.2), (1, 3, 0.01),
-- Cappuccino
(2, 1, 0.03), (2, 2, 0.15), (2, 3, 0.01), (2, 6, 0.05),
-- Americano
(3, 1, 0.025), (3, 2, 0.05), (3, 3, 0.01),
-- Caramel Macchiato
(4, 1, 0.03), (4, 2, 0.2), (4, 3, 0.01), (4, 4, 0.03), (4, 6, 0.05),
-- Mocha
(5, 1, 0.03), (5, 2, 0.15), (5, 3, 0.01), (5, 5, 0.03), (5, 6, 0.05),
-- Condensed Milk Coffee
(6, 1, 0.03), (6, 2, 0.1), (6, 3, 0.02);

-- Orders (Historical data - Last 3 months)
INSERT INTO `orders` (`id`, `table_id`, `staff_id`, `order_type`, `status`, `created_at`) VALUES
-- September 2025
(1, 3, 2, 'dine-in', 'paid', '2025-09-01 09:15:00'),
(2, 7, 3, 'dine-in', 'paid', '2025-09-01 10:30:00'),
(3, NULL, 2, 'takeaway', 'paid', '2025-09-01 11:45:00'),
(4, 12, 4, 'dine-in', 'paid', '2025-09-01 14:20:00'),
(5, NULL, 3, 'takeaway', 'paid', '2025-09-01 16:00:00'),
(6, 5, 2, 'dine-in', 'paid', '2025-09-02 09:00:00'),
(7, 18, 5, 'dine-in', 'paid', '2025-09-02 10:15:00'),
(8, NULL, 4, 'takeaway', 'paid', '2025-09-02 11:30:00'),
(9, 25, 3, 'dine-in', 'paid', '2025-09-02 13:45:00'),
(10, 8, 2, 'dine-in', 'paid', '2025-09-02 15:20:00'),
(11, NULL, 5, 'takeaway', 'paid', '2025-09-03 09:30:00'),
(12, 14, 4, 'dine-in', 'paid', '2025-09-03 11:00:00'),
(13, 22, 3, 'dine-in', 'paid', '2025-09-03 14:15:00'),
(14, NULL, 2, 'takeaway', 'paid', '2025-09-03 16:30:00'),
(15, 9, 5, 'dine-in', 'paid', '2025-09-04 10:00:00'),
(16, 16, 4, 'dine-in', 'paid', '2025-09-04 12:15:00'),
(17, NULL, 3, 'takeaway', 'paid', '2025-09-04 14:45:00'),
(18, 27, 2, 'dine-in', 'paid', '2025-09-04 16:00:00'),
(19, 6, 5, 'dine-in', 'paid', '2025-09-05 09:15:00'),
(20, NULL, 4, 'takeaway', 'paid', '2025-09-05 11:30:00'),

-- October 2025 - Week 1
(21, 11, 3, 'dine-in', 'paid', '2025-10-01 08:30:00'),
(22, NULL, 2, 'takeaway', 'paid', '2025-10-01 09:45:00'),
(23, 19, 5, 'dine-in', 'paid', '2025-10-01 11:00:00'),
(24, 4, 4, 'dine-in', 'paid', '2025-10-01 13:30:00'),
(25, NULL, 3, 'takeaway', 'paid', '2025-10-01 15:15:00'),
(26, 13, 2, 'dine-in', 'paid', '2025-10-02 08:45:00'),
(27, 21, 5, 'dine-in', 'paid', '2025-10-02 10:30:00'),
(28, NULL, 4, 'takeaway', 'paid', '2025-10-02 12:00:00'),
(29, 28, 3, 'dine-in', 'paid', '2025-10-02 14:15:00'),
(30, 7, 2, 'dine-in', 'paid', '2025-10-02 16:45:00'),

-- October 2025 - Week 2
(31, NULL, 5, 'takeaway', 'paid', '2025-10-08 09:00:00'),
(32, 15, 4, 'dine-in', 'paid', '2025-10-08 10:30:00'),
(33, 23, 3, 'dine-in', 'paid', '2025-10-08 12:45:00'),
(34, NULL, 2, 'takeaway', 'paid', '2025-10-08 14:00:00'),
(35, 10, 5, 'dine-in', 'paid', '2025-10-08 16:30:00'),
(36, 17, 4, 'dine-in', 'paid', '2025-10-09 08:15:00'),
(37, NULL, 3, 'takeaway', 'paid', '2025-10-09 10:00:00'),
(38, 26, 2, 'dine-in', 'paid', '2025-10-09 12:30:00'),
(39, 5, 5, 'dine-in', 'paid', '2025-10-09 14:45:00'),
(40, NULL, 4, 'takeaway', 'paid', '2025-10-09 16:15:00'),

-- October 2025 - Current Week
(41, 12, 3, 'dine-in', 'paid', '2025-10-14 08:30:00'),
(42, NULL, 2, 'takeaway', 'paid', '2025-10-14 09:45:00'),
(43, 20, 5, 'dine-in', 'paid', '2025-10-14 11:15:00'),
(44, 8, 4, 'dine-in', 'paid', '2025-10-14 13:00:00'),
(45, NULL, 3, 'takeaway', 'paid', '2025-10-14 15:30:00'),
(46, 14, 2, 'dine-in', 'paid', '2025-10-15 08:45:00'),
(47, 22, 5, 'dine-in', 'paid', '2025-10-15 10:15:00'),
(48, NULL, 4, 'takeaway', 'paid', '2025-10-15 12:30:00'),
(49, 29, 3, 'dine-in', 'paid', '2025-10-15 14:00:00'),
(50, 6, 2, 'dine-in', 'paid', '2025-10-15 16:45:00'),

-- Today (October 16, 2025)
(51, 11, 2, 'dine-in', 'paid', '2025-10-16 08:30:00'),
(52, NULL, 6, 'takeaway', 'paid', '2025-10-16 09:15:00'),
(53, 18, 2, 'dine-in', 'paid', '2025-10-16 10:45:00'),
(54, 3, 6, 'dine-in', 'paid', '2025-10-16 12:00:00'),
(55, NULL, 2, 'takeaway', 'unpaid', '2025-10-16 13:30:00');

-- Order Items (Multiple items per order)
INSERT INTO `order_items` (`order_id`, `product_id`, `drink_type`, `quantity`, `price`, `note`) VALUES
-- Order 1
(1, 1, 'iced', 2, 2.80, ''), (1, 3, 'hot', 1, 2.25, ''),
-- Order 2
(2, 2, 'hot', 1, 2.50, ''), (2, 4, 'iced', 1, 2.80, 'Extra caramel'),
-- Order 3
(3, 1, 'frappe', 3, 2.80, ''), (3, 5, 'iced', 1, 2.15, ''),
-- Order 4
(4, 3, 'hot', 2, 2.25, ''), (4, 6, 'iced', 2, 2.50, ''),
-- Order 5
(5, 2, 'iced', 1, 2.50, ''), (5, 1, 'hot', 2, 2.80, ''),
-- Order 6
(6, 4, 'frappe', 1, 2.80, ''), (6, 3, 'hot', 1, 2.25, ''),
-- Order 7
(7, 5, 'iced', 2, 2.15, ''), (7, 2, 'hot', 1, 2.50, ''),
-- Order 8
(8, 1, 'iced', 1, 2.80, ''), (8, 6, 'hot', 1, 2.50, ''),
-- Order 9
(9, 3, 'iced', 3, 2.25, ''), (9, 4, 'frappe', 1, 2.80, ''),
-- Order 10
(10, 2, 'hot', 1, 2.50, ''), (10, 5, 'iced', 2, 2.15, ''),
-- Orders 11-20
(11, 1, 'frappe', 2, 2.80, ''), (11, 3, 'hot', 1, 2.25, ''),
(12, 4, 'iced', 1, 2.80, ''), (12, 2, 'hot', 2, 2.50, ''),
(13, 5, 'frappe', 1, 2.15, ''), (13, 1, 'iced', 1, 2.80, ''),
(14, 6, 'hot', 2, 2.50, ''), (14, 3, 'iced', 1, 2.25, ''),
(15, 2, 'iced', 1, 2.50, ''), (15, 4, 'hot', 1, 2.80, ''),
(16, 1, 'hot', 3, 2.80, ''), (16, 5, 'iced', 1, 2.15, ''),
(17, 3, 'iced', 2, 2.25, ''), (17, 2, 'frappe', 1, 2.50, ''),
(18, 4, 'frappe', 1, 2.80, ''), (18, 6, 'iced', 2, 2.50, ''),
(19, 5, 'hot', 1, 2.15, ''), (19, 1, 'iced', 2, 2.80, ''),
(20, 2, 'hot', 2, 2.50, ''), (20, 3, 'hot', 1, 2.25, ''),
-- Orders 21-30
(21, 1, 'iced', 1, 2.80, ''), (21, 4, 'frappe', 1, 2.80, ''),
(22, 3, 'hot', 2, 2.25, ''), (22, 5, 'iced', 1, 2.15, ''),
(23, 2, 'iced', 1, 2.50, ''), (23, 6, 'hot', 1, 2.50, ''),
(24, 1, 'frappe', 2, 2.80, ''), (24, 3, 'iced', 1, 2.25, ''),
(25, 4, 'hot', 1, 2.80, ''), (25, 2, 'frappe', 2, 2.50, ''),
(26, 5, 'iced', 1, 2.15, ''), (26, 1, 'hot', 1, 2.80, ''),
(27, 3, 'hot', 2, 2.25, ''), (27, 6, 'iced', 1, 2.50, ''),
(28, 2, 'iced', 1, 2.50, ''), (28, 4, 'frappe', 1, 2.80, ''),
(29, 1, 'hot', 2, 2.80, ''), (29, 5, 'iced', 1, 2.15, ''),
(30, 3, 'iced', 1, 2.25, ''), (30, 2, 'hot', 2, 2.50, ''),
-- Orders 31-40
(31, 6, 'hot', 2, 2.50, ''), (31, 1, 'iced', 1, 2.80, ''),
(32, 4, 'frappe', 1, 2.80, ''), (32, 3, 'hot', 1, 2.25, ''),
(33, 2, 'iced', 2, 2.50, ''), (33, 5, 'frappe', 1, 2.15, ''),
(34, 1, 'hot', 1, 2.80, ''), (34, 6, 'iced', 2, 2.50, ''),
(35, 3, 'iced', 2, 2.25, ''), (35, 4, 'hot', 1, 2.80, ''),
(36, 2, 'frappe', 1, 2.50, ''), (36, 1, 'iced', 2, 2.80, ''),
(37, 5, 'hot', 1, 2.15, ''), (37, 3, 'hot', 1, 2.25, ''),
(38, 4, 'iced', 2, 2.80, ''), (38, 2, 'hot', 1, 2.50, ''),
(39, 1, 'frappe', 1, 2.80, ''), (39, 6, 'iced', 1, 2.50, ''),
(40, 3, 'hot', 2, 2.25, ''), (40, 5, 'iced', 1, 2.15, ''),
-- Orders 41-50
(41, 2, 'iced', 1, 2.50, ''), (41, 1, 'hot', 2, 2.80, ''),
(42, 4, 'frappe', 1, 2.80, ''), (42, 3, 'iced', 1, 2.25, ''),
(43, 5, 'hot', 2, 2.15, ''), (43, 6, 'iced', 1, 2.50, ''),
(44, 1, 'iced', 1, 2.80, ''), (44, 2, 'frappe', 2, 2.50, ''),
(45, 3, 'hot', 2, 2.25, ''), (45, 4, 'iced', 1, 2.80, ''),
(46, 5, 'frappe', 1, 2.15, ''), (46, 1, 'hot', 1, 2.80, ''),
(47, 2, 'iced', 2, 2.50, ''), (47, 3, 'hot', 1, 2.25, ''),
(48, 6, 'iced', 1, 2.50, ''), (48, 4, 'frappe', 1, 2.80, ''),
(49, 1, 'hot', 2, 2.80, ''), (49, 5, 'iced', 1, 2.15, ''),
(50, 2, 'frappe', 1, 2.50, ''), (50, 3, 'iced', 2, 2.25, ''),
-- Orders 51-54 (Today)
(51, 1, 'iced', 2, 2.80, ''), (51, 4, 'frappe', 1, 2.80, ''),
(52, 3, 'hot', 1, 2.25, ''), (52, 2, 'iced', 2, 2.50, ''),
(53, 5, 'frappe', 1, 2.15, ''), (53, 1, 'hot', 1, 2.80, ''), (53, 6, 'iced', 1, 2.50, ''),
(54, 4, 'iced', 2, 2.80, ''), (54, 3, 'hot', 1, 2.25, ''),
-- Order 55 (Unpaid)
(55, 1, 'frappe', 2, 2.80, ''), (55, 2, 'hot', 1, 2.50, '');

-- Payments (Tiếp tục từ order 9 đến 54)
INSERT INTO `payments` (`order_id`, `total_price`, `vat`, `amount_received`, `change_returned`, `paid_at`) VALUES
(9, 9.43, 0.70, 10.00, 0.57, '2025-09-02 13:50:00'),
(10, 6.57, 0.49, 10.00, 3.43, '2025-09-02 15:25:00'),
(11, 8.44, 0.63, 10.00, 1.56, '2025-09-03 09:35:00'),
(12, 8.14, 0.61, 10.00, 1.86, '2025-09-03 11:05:00'),
(13, 6.60, 0.49, 10.00, 3.40, '2025-09-03 14:20:00'),
(14, 7.75, 0.58, 10.00, 2.42, '2025-09-03 16:35:00'),
(15, 5.81, 0.43, 10.00, 4.19, '2025-09-04 10:05:00'),
(16, 10.10, 0.75, 15.00, 4.90, '2025-09-04 12:20:00'),
(17, 7.01, 0.52, 10.00, 2.99, '2025-09-04 14:50:00'),
(18, 9.24, 0.69, 10.00, 0.76, '2025-09-04 16:05:00'),
(19, 6.60, 0.49, 10.00, 3.40, '2025-09-05 09:20:00'),
(20, 7.75, 0.58, 10.00, 2.42, '2025-09-05 11:35:00'),
(21, 6.93, 0.52, 10.00, 3.07, '2025-10-01 08:35:00'),
(22, 7.75, 0.58, 10.00, 2.42, '2025-10-01 09:50:00'),
(23, 7.75, 0.58, 10.00, 2.42, '2025-10-01 11:05:00'),
(24, 8.44, 0.63, 10.00, 1.56, '2025-10-01 13:35:00'),
(25, 8.10, 0.60, 10.00, 1.90, '2025-10-01 15:20:00'),
(26, 6.60, 0.49, 10.00, 3.40, '2025-10-02 08:50:00'),
(27, 7.75, 0.58, 10.00, 2.42, '2025-10-02 10:35:00'),
(28, 5.67, 0.42, 10.00, 4.33, '2025-10-02 12:05:00'),
(29, 8.44, 0.63, 10.00, 1.56, '2025-10-02 14:20:00'),
(30, 7.75, 0.58, 10.00, 2.42, '2025-10-02 16:50:00'),
(31, 7.10, 0.53, 10.00, 2.90, '2025-10-08 09:05:00'),
(32, 5.40, 0.40, 10.00, 4.60, '2025-10-08 10:35:00'),
(33, 7.30, 0.54, 10.00, 2.70, '2025-10-08 12:50:00'),
(34, 7.30, 0.54, 10.00, 2.70, '2025-10-08 14:05:00'),
(35, 8.14, 0.61, 10.00, 1.86, '2025-10-08 16:35:00'),
(36, 8.10, 0.60, 10.00, 1.90, '2025-10-09 08:20:00'),
(37, 5.81, 0.43, 10.00, 4.19, '2025-10-09 10:05:00'),
(38, 8.56, 0.64, 10.00, 1.44, '2025-10-09 12:35:00'),
(39, 6.35, 0.47, 10.00, 3.65, '2025-10-09 14:50:00'),
(40, 7.01, 0.52, 10.00, 2.99, '2025-10-09 16:20:00'),
(41, 8.10, 0.61, 10.00, 1.90, '2025-10-14 08:35:00'),
(42, 5.72, 0.43, 10.00, 4.28, '2025-10-14 09:50:00'),
(43, 7.30, 0.54, 10.00, 2.70, '2025-10-14 11:20:00'),
(44, 8.10, 0.61, 10.00, 1.90, '2025-10-14 13:05:00'),
(45, 8.44, 0.63, 10.00, 1.56, '2025-10-14 15:35:00'),
(46, 6.60, 0.49, 10.00, 3.40, '2025-10-15 08:50:00'),
(47, 7.75, 0.58, 10.00, 2.42, '2025-10-15 10:20:00'),
(48, 5.67, 0.42, 10.00, 4.33, '2025-10-15 12:35:00'),
(49, 8.44, 0.63, 10.00, 1.56, '2025-10-15 14:05:00'),
(50, 7.75, 0.58, 10.00, 2.42, '2025-10-15 16:50:00'),
(51, 8.93, 0.67, 10.00, 1.07, '2025-10-16 08:35:00'),
(52, 7.75, 0.58, 10.00, 2.42, '2025-10-16 09:20:00'),
(53, 8.93, 0.67, 10.00, 1.07, '2025-10-16 10:50:00'),
(54, 9.24, 0.69, 10.00, 0.76, '2025-10-16 12:05:00');

-- Transactions (Income & Expense records)
INSERT INTO `transactions` (`type`, `amount`, `reason`, `created_by`, `created_at`) VALUES
-- Income transactions (từ các orders)
('income', 8.44, 'Sales - Order 1', 2, '2025-09-01 09:20:00'),
('income', 5.72, 'Sales - Order 2', 3, '2025-09-01 10:35:00'),
('income', 10.55, 'Sales - Order 3 (Takeaway)', 2, '2025-09-01 11:50:00'),
('income', 9.50, 'Sales - Order 4', 4, '2025-09-01 14:25:00'),
('income', 8.10, 'Sales - Order 5 (Takeaway)', 3, '2025-09-01 16:05:00'),
('income', 5.40, 'Sales - Order 6', 2, '2025-09-02 09:05:00'),
('income', 7.30, 'Sales - Order 7', 5, '2025-09-02 10:20:00'),
('income', 5.67, 'Sales - Order 8 (Takeaway)', 4, '2025-09-02 11:35:00'),
('income', 9.43, 'Sales - Order 9', 3, '2025-09-02 13:50:00'),
('income', 6.57, 'Sales - Order 10', 2, '2025-09-02 15:25:00'),
('income', 8.44, 'Sales - Order 11 (Takeaway)', 5, '2025-09-03 09:35:00'),
('income', 8.14, 'Sales - Order 12', 4, '2025-09-03 11:05:00'),
('income', 6.60, 'Sales - Order 13', 3, '2025-09-03 14:20:00'),
('income', 7.75, 'Sales - Order 14 (Takeaway)', 2, '2025-09-03 16:35:00'),
('income', 5.81, 'Sales - Order 15', 5, '2025-09-04 10:05:00'),
('income', 10.10, 'Sales - Order 16', 4, '2025-09-04 12:20:00'),
('income', 7.01, 'Sales - Order 17 (Takeaway)', 3, '2025-09-04 14:50:00'),
('income', 9.24, 'Sales - Order 18', 2, '2025-09-04 16:05:00'),
('income', 6.60, 'Sales - Order 19', 5, '2025-09-05 09:20:00'),
('income', 7.75, 'Sales - Order 20 (Takeaway)', 4, '2025-09-05 11:35:00'),
('income', 6.93, 'Sales - Order 21', 3, '2025-10-01 08:35:00'),
('income', 7.75, 'Sales - Order 22 (Takeaway)', 2, '2025-10-01 09:50:00'),
('income', 7.75, 'Sales - Order 23', 5, '2025-10-01 11:05:00'),
('income', 8.44, 'Sales - Order 24', 4, '2025-10-01 13:35:00'),
('income', 8.10, 'Sales - Order 25 (Takeaway)', 3, '2025-10-01 15:20:00'),
('income', 6.60, 'Sales - Order 26', 2, '2025-10-02 08:50:00'),
('income', 7.75, 'Sales - Order 27', 5, '2025-10-02 10:35:00'),
('income', 5.67, 'Sales - Order 28 (Takeaway)', 4, '2025-10-02 12:05:00'),
('income', 8.44, 'Sales - Order 29', 3, '2025-10-02 14:20:00'),
('income', 7.75, 'Sales - Order 30', 2, '2025-10-02 16:50:00'),
('income', 7.10, 'Sales - Order 31 (Takeaway)', 5, '2025-10-08 09:05:00'),
('income', 5.40, 'Sales - Order 32', 4, '2025-10-08 10:35:00'),
('income', 7.30, 'Sales - Order 33', 3, '2025-10-08 12:50:00'),
('income', 7.30, 'Sales - Order 34 (Takeaway)', 2, '2025-10-08 14:05:00'),
('income', 8.14, 'Sales - Order 35', 5, '2025-10-08 16:35:00'),
('income', 8.10, 'Sales - Order 36', 4, '2025-10-09 08:20:00'),
('income', 5.81, 'Sales - Order 37 (Takeaway)', 3, '2025-10-09 10:05:00'),
('income', 8.56, 'Sales - Order 38', 2, '2025-10-09 12:35:00'),
('income', 6.35, 'Sales - Order 39', 5, '2025-10-09 14:50:00'),
('income', 7.01, 'Sales - Order 40 (Takeaway)', 4, '2025-10-09 16:20:00'),
('income', 8.10, 'Sales - Order 41', 3, '2025-10-14 08:35:00'),
('income', 5.72, 'Sales - Order 42 (Takeaway)', 2, '2025-10-14 09:50:00'),
('income', 7.30, 'Sales - Order 43', 5, '2025-10-14 11:20:00'),
('income', 8.10, 'Sales - Order 44', 4, '2025-10-14 13:05:00'),
('income', 8.44, 'Sales - Order 45 (Takeaway)', 3, '2025-10-14 15:35:00'),
('income', 6.60, 'Sales - Order 46', 2, '2025-10-15 08:50:00'),
('income', 7.75, 'Sales - Order 47', 5, '2025-10-15 10:20:00'),
('income', 5.67, 'Sales - Order 48 (Takeaway)', 4, '2025-10-15 12:35:00'),
('income', 8.44, 'Sales - Order 49', 3, '2025-10-15 14:05:00'),
('income', 7.75, 'Sales - Order 50', 2, '2025-10-15 16:50:00'),
('income', 8.93, 'Sales - Order 51', 2, '2025-10-16 08:35:00'),
('income', 7.75, 'Sales - Order 52 (Takeaway)', 6, '2025-10-16 09:20:00'),
('income', 8.93, 'Sales - Order 53', 2, '2025-10-16 10:50:00'),
('income', 9.24, 'Sales - Order 54', 6, '2025-10-16 12:05:00'),

-- Expense transactions
('expense', 150.00, 'Coffee Beans - Restock 10kg', 1, '2025-09-15 10:00:00'),
('expense', 45.00, 'Milk - Restock 10 liters', 1, '2025-09-18 11:30:00'),
('expense', 50.00, 'Equipment Maintenance', 1, '2025-09-20 14:00:00'),
('expense', 75.00, 'Cleaning Supplies', 1, '2025-09-25 09:00:00'),
('expense', 120.00, 'Coffee Beans - Restock 10kg', 1, '2025-10-05 10:30:00'),
('expense', 40.00, 'Paper Cups & Straws - Restock', 1, '2025-10-08 15:00:00'),
('expense', 60.00, 'Utilities Payment', 1, '2025-10-10 09:00:00'),
('expense', 100.00, 'Staff Bonus - Performance', 1, '2025-10-12 16:00:00');
ALTER TABLE transactions ADD COLUMN display_order INT DEFAULT 0;
COMMIT;