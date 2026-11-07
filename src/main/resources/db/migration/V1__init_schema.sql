-- V1: Khởi tạo toàn bộ schema ban đầu (chuyển từ database_web_server.sql)

-- 1. Bảng user
CREATE TABLE `user` (
    `user_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `fullname` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `role` VARCHAR(50) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng farm
CREATE TABLE `farm` (
    `farm_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `location` VARCHAR(255) NULL,
    `description` VARCHAR(255) NULL,
    CONSTRAINT `fk_farm_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE
);

-- 3. Bảng device
CREATE TABLE `device` (
    `device_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `farm_id` BIGINT NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `type_device` VARCHAR(255) NOT NULL,
    `note` VARCHAR(255) NULL,
    CONSTRAINT `fk_device_farm` FOREIGN KEY (`farm_id`) REFERENCES `farm`(`farm_id`) ON DELETE CASCADE
);

-- 4. Bảng actor (Thiết bị chấp hành)
CREATE TABLE `actor` (
    `actor_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `device_id` BIGINT NOT NULL,
    `type_actor` VARCHAR(50) NOT NULL,
    `mode` TINYINT DEFAULT 0,
    `status` TINYINT DEFAULT 0,
    CONSTRAINT `fk_actor_device` FOREIGN KEY (`device_id`) REFERENCES `device`(`device_id`) ON DELETE CASCADE
);

-- 5. Bảng sensor (Cảm biến)
CREATE TABLE `sensor` (
    `sensor_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `device_id` BIGINT NOT NULL,
    `type_sensor` VARCHAR(50) NOT NULL,
    CONSTRAINT `fk_sensor_device` FOREIGN KEY (`device_id`) REFERENCES `device`(`device_id`) ON DELETE CASCADE
);

-- 6. Bảng sensordata (Dữ liệu cảm biến)
CREATE TABLE `sensordata` (
    `sensordata_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `sensor_id` BIGINT NOT NULL,
    `value` FLOAT NOT NULL,
    `unit` VARCHAR(10) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_sensordata_sensor` FOREIGN KEY (`sensor_id`) REFERENCES `sensor`(`sensor_id`) ON DELETE CASCADE
);

-- 7. Bảng schedule (Lịch trình)
CREATE TABLE `schedule` (
    `schedule_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `actor_id` BIGINT NOT NULL,
    `execution_time` TIME NOT NULL,
    `status` TINYINT DEFAULT 0,
    `mode` TINYINT DEFAULT 0,
    `is_executed` TINYINT(1) DEFAULT 0,
    `note` VARCHAR(255) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_schedule_actor` FOREIGN KEY (`actor_id`) REFERENCES `actor`(`actor_id`) ON DELETE CASCADE
);

-- 8. Bảng notification (Thông báo)
CREATE TABLE `notification` (
    `notification_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `message` VARCHAR(255) NOT NULL,
    `status` TINYINT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE
);
