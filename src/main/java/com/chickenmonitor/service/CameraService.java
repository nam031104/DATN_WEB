
package com.chickenmonitor.service;

import com.chickenmonitor.dto.response.CameraStreamResponse;
import com.chickenmonitor.entity.Device;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;

import java.time.Duration;
import java.util.Map;

@Service
public class CameraService {

    private static final Logger log =
            LoggerFactory.getLogger(CameraService.class);

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private final String serviceUrl;

    private final String serviceToken;


    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public CameraService(
            @Value("${app.camera.service-url:}") String serviceUrl,
            @Value("${app.camera.service-token:}") String serviceToken,
            ObjectMapper objectMapper
    ) {

        this.serviceUrl = serviceUrl;
        this.serviceToken = serviceToken;
        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newBuilder()

                // Force HTTP/1.1
                .version(HttpClient.Version.HTTP_1_1)

                // Connection timeout
                .connectTimeout(Duration.ofSeconds(15))

                .build();

        log.info("====================================");
        log.info("CAMERA SERVICE INITIALIZED");
        log.info("Camera URL: {}", serviceUrl);
        log.info("Token configured: {}",
                serviceToken != null && !serviceToken.isBlank());
        log.info("HTTP Client: JDK HttpClient HTTP/1.1");
        log.info("====================================");
    }


    // ==========================================
    // REQUEST CAMERA STREAM
    // ==========================================

    public CameraStreamResponse requestStream(Device device) {

        // Check configuration
        if (serviceUrl == null || serviceUrl.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Camera service URL chưa được cấu hình."
            );
        }

        // Check token
        if (serviceToken == null || serviceToken.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Camera service token chưa được cấu hình."
            );
        }

        // Check device
        if (device == null ||
                device.getDeviceCode() == null ||
                device.getDeviceCode().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Device hoặc DeviceCode không hợp lệ."
            );
        }

        try {

            String deviceCode = device.getDeviceCode();

            // ==========================================
            // 1. BUILD JSON BODY
            // ==========================================

            Map<String, String> requestBody = Map.of(
                    "deviceCode",
                    deviceCode
            );

            String jsonBody = objectMapper.writeValueAsString(
                    requestBody
            );

            String endpoint = serviceUrl.replaceAll("/+$", "")
                    + "/api/streams";

            log.info("========== CAMERA REQUEST ==========");
            log.info("Endpoint: {}", endpoint);
            log.info("Device ID: {}", device.getDeviceId());
            log.info("Device Code: {}", deviceCode);
            log.info("JSON Body: {}", jsonBody);
            log.info("Body bytes: {}",
                    jsonBody.getBytes(StandardCharsets.UTF_8).length);
            log.info("====================================");


            // ==========================================
            // 2. BUILD HTTP REQUEST
            // ==========================================

            HttpRequest request = HttpRequest.newBuilder()

                    .uri(URI.create(endpoint))

                    .timeout(Duration.ofSeconds(90))

                    .header(
                            "Authorization",
                            "Bearer " + serviceToken
                    )

                    .header(
                            "Content-Type",
                            "application/json; charset=UTF-8"
                    )

                    .header(
                            "Accept",
                            "application/json"
                    )

                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    jsonBody,
                                    StandardCharsets.UTF_8
                            )
                    )

                    .build();


            // ==========================================
            // 3. SEND HTTP REQUEST
            // ==========================================

            HttpResponse<String> httpResponse = httpClient.send(

                    request,

                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );


            int statusCode = httpResponse.statusCode();

            String responseBody = httpResponse.body();


            log.info("========== CAMERA RESPONSE ==========");
            log.info("HTTP Status: {}", statusCode);
            log.info("Response Body: {}", responseBody);
            log.info("=====================================");


            // ==========================================
            // 4. CHECK HTTP STATUS
            // ==========================================

            if (statusCode < 200 || statusCode >= 300) {

                log.error(
                        "Camera API returned HTTP {}: {}",
                        statusCode,
                        responseBody
                );

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Camera API trả lỗi HTTP "
                                + statusCode
                                + ": "
                                + responseBody
                );
            }


            // ==========================================
            // 5. PARSE JSON RESPONSE
            // ==========================================

            CameraStreamResponse response =
                    objectMapper.readValue(
                            responseBody,
                            CameraStreamResponse.class
                    );


            // ==========================================
            // 6. VALIDATE STREAM URL
            // ==========================================

            if (response == null ||
                    response.streamUrl() == null ||
                    response.streamUrl().isBlank()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Camera service không trả stream URL."
                );
            }

            String streamUrl = response.streamUrl();

            if (!streamUrl.startsWith("http://")
                    && !streamUrl.startsWith("https://")) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Camera service trả URL không hợp lệ."
                );
            }


            // ==========================================
            // 7. SUCCESS
            // ==========================================

            log.info("====================================");
            log.info("CAMERA STREAM CREATED SUCCESSFULLY");
            log.info("Device Code: {}", deviceCode);
            log.info("Stream URL: {}", streamUrl);
            log.info("====================================");

            return response;


        } catch (ResponseStatusException exception) {

            throw exception;


        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            log.error("Camera request interrupted.", exception);

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Yêu cầu camera bị gián đoạn.",
                    exception
            );


        } catch (Exception exception) {

            log.error(
                    "Camera service communication failed.",
                    exception
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể giao tiếp với Camera Server: "
                            + exception.getMessage(),
                    exception
            );
        }
    }
}
