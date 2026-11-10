package com.chickenmonitor.service;

import com.chickenmonitor.entity.Device;
import com.chickenmonitor.entity.Notification;
import com.chickenmonitor.entity.Sensor;
import com.chickenmonitor.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/** Create actionable alerts from persisted telemetry, with per-device/type cooldown. */
@Service
public class SensorAlertService {
    private static final Logger log = LoggerFactory.getLogger(SensorAlertService.class);
    private final NotificationRepository notifications;
    private final double maxTemperature;
    private final double minTemperature;
    private final double maxHumidity;
    private final double minHumidity;
    private final int cooldownMinutes;

    public SensorAlertService(NotificationRepository notifications,
            @Value("${app.alerts.temperature.max:35}") double maxTemperature,
            @Value("${app.alerts.temperature.min:18}") double minTemperature,
            @Value("${app.alerts.humidity.max:80}") double maxHumidity,
            @Value("${app.alerts.humidity.min:40}") double minHumidity,
            @Value("${app.alerts.cooldown-minutes:30}") int cooldownMinutes) {
        this.notifications = notifications;
        this.maxTemperature = maxTemperature;
        this.minTemperature = minTemperature;
        this.maxHumidity = maxHumidity;
        this.minHumidity = minHumidity;
        this.cooldownMinutes = Math.max(0, cooldownMinutes);
    }

    public void check(Sensor sensor, Float value, String unit) {
        if (sensor == null || value == null || !Float.isFinite(value)) return;
        Device device = sensor.getDevice();
        if (device == null || device.getFarm() == null || device.getFarm().getUser() == null) return;
        String type = String.valueOf(sensor.getTypeSensor()).toLowerCase(Locale.ROOT);
        String code = String.valueOf(sensor.getExternalCode()).toLowerCase(Locale.ROOT);
        String u = unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
        boolean temperature = type.contains("temp") || type.contains("nhiệt") || code.contains("temp")
                || u.equals("°c") || u.equals("celsius");
        boolean humidity = !temperature && (type.contains("humid") || type.contains("độ ẩm")
                || code.contains("humid") || code.contains("dht_hum"));
        if (!temperature && !humidity) return;
        double min = temperature ? minTemperature : minHumidity;
        double max = temperature ? maxTemperature : maxHumidity;
        if (value >= min && value <= max) return;
        Long userId = device.getFarm().getUser().getUserId();
        String kind = temperature ? "nhiệt độ" : "độ ẩm";
        String direction = value > max ? "cao" : "thấp";
        // Stable prefix identifies one alert category for one device and direction.
        String prefix = "Cảnh báo " + kind + " " + direction + " [" + device.getDeviceId() + "]:";
        try {
            boolean recent = notifications
                    .findFirstByUserUserIdAndMessageStartingWithOrderByCreatedAtDesc(userId, prefix)
                    .filter(last -> last.getCreatedAt() != null &&
                            Duration.between(last.getCreatedAt(), LocalDateTime.now()).toMinutes() < cooldownMinutes)
                    .isPresent();
            if (recent) return;
            String message = prefix + " Chuồng " + device.getFarm().getName() + ", thiết bị " + device.getName()
                    + " đo " + value + (unit == null ? "" : unit) + ", "
                    + (value > max ? "vượt ngưỡng " + max : "dưới ngưỡng " + min) + ".";
            if (message.length() > 255) message = message.substring(0, 255);
            notifications.save(Notification.builder().user(device.getFarm().getUser())
                    .message(message).status(0).build());
        } catch (RuntimeException e) {
            // Telemetry must never stop if the notification table is unavailable.
            log.warn("Không thể lưu cảnh báo cảm biến {}", sensor.getSensorId(), e);
        }
    }
}
