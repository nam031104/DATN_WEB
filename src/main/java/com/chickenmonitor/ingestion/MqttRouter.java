package com.chickenmonitor.ingestion;

import com.chickenmonitor.ingestion.MqttHandle.*;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.util.Map;

@Component
@Service
public class MqttRouter {

    private static final Logger log = LoggerFactory.getLogger(MqttRouter.class);
    private final Map<String, MqttMessageHandler> handlers;

    public MqttRouter(
            TelemetryHandler telemetry,
            AckHandler ackHandler,
            AckThresholdHandler ackThresholdHandler,
            FeedHandler feedHandler,
            ResponseHandler responseHandler
    ) {
        this.handlers = Map.of(
                "telemetry", telemetry,
                "ack", ackHandler,
                "ackthreshold", ackThresholdHandler,
                "feed", feedHandler,
                "response", responseHandler
        );
    }

    public void route(String topic, String payload) {

        String category = extractCategory(topic);

        MqttMessageHandler handler =
                this.handlers.get(category);

        if (handler == null) {
            log.warn("Unknown MQTT topic: {}", topic);
            return;
        }

        handler.handle(topic, payload);
    }

    private String extractCategory(String topic) {

        String prefix = "chickenmonitor/";

        if (!topic.startsWith(prefix)) {
            return null;
        }

        String remaining = topic.substring(prefix.length());

        int firstSlash = remaining.indexOf('/');

        if (firstSlash < 0) {
            return null;
        }

        return remaining.substring(firstSlash + 1);
    }

}

