-- V2: Thêm mã phần cứng, ngưỡng điều khiển và ngày chạy lịch gần nhất
-- (gộp V1__add_hardware_codes.sql và V2__add_telemetry_controls.sql cũ;
--  vì chạy trên schema sạch từ V1 nên không cần kiểm tra information_schema)

-- device: mã thiết bị, cho phép chưa gán farm
ALTER TABLE `device`
    ADD COLUMN `device_code` VARCHAR(100) NULL,
    ADD UNIQUE KEY `uq_device_device_code` (`device_code`),
    MODIFY COLUMN `farm_id` BIGINT NULL;

-- sensor: mã cảm biến theo từng thiết bị
ALTER TABLE `sensor`
    ADD COLUMN `external_code` VARCHAR(100) NULL,
    ADD UNIQUE KEY `uq_sensor_device_external_code` (`device_id`, `external_code`);

-- actor: mã chấp hành, ngưỡng tự động và cảm biến nguồn
ALTER TABLE `actor`
    ADD COLUMN `external_code` VARCHAR(100) NULL,
    ADD COLUMN `threshold_min` DOUBLE NULL,
    ADD COLUMN `threshold_max` DOUBLE NULL,
    ADD COLUMN `source_sensor_code` VARCHAR(100) NULL,
    ADD UNIQUE KEY `uq_actor_device_external_code` (`device_id`, `external_code`);

-- schedule: ngày đã thực thi gần nhất
ALTER TABLE `schedule`
    ADD COLUMN `last_executed_date` DATE NULL;
