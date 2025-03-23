package com.example.iot.AirGuard.Controller;

import com.example.iot.AirGuard.Model.DialogflowRequest;
import com.example.iot.AirGuard.Model.AirQualityData;
import com.example.iot.AirGuard.Model.FanControlCommand;
import com.example.iot.AirGuard.Model.FanHistory;
import com.example.iot.AirGuard.Model.AlertHistory;
import com.example.iot.AirGuard.Service.AirQualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/webhook")
public class DialogflowWebhookController {

    private static final Logger logger = Logger.getLogger(DialogflowWebhookController.class.getName());
    private static final double PM25_THRESHOLD = 50.0;

    @Autowired
    private AirQualityService airQualityService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private FanControlCommand lastFanState = new FanControlCommand(false, "AUTO");

    @PostMapping
    public Map<String, Object> handleWebhook(@RequestBody DialogflowRequest request) {
        logger.info("===> Webhook received a request: " + request);

        String responseText = "Sorry, I couldn't understand your request.";

        try {
            if (request != null && request.getQueryResult() != null) {
                String intentName = request.getQueryResult().getIntent().getDisplayName();
                logger.info("👉 Detected intent: " + intentName);

                if ("get_air_quality".equals(intentName)) {
                    AirQualityData latestData = airQualityService.getLatestAirQualityData();
                    int dustLevel = latestData.getDustLevel();
                    float temperature = latestData.getTemperature();
                    float humidity = latestData.getHumidity();

                    responseText = "The current PM2.5 level is " + dustLevel + " micrograms per cubic meter. " +
                            "Temperature is " + temperature + " degrees Celsius, and humidity is " + humidity + " percent. " +
                            "Air quality is " + (dustLevel < 25 ? "good." : "poor.");
                    if (dustLevel > PM25_THRESHOLD) {
                        responseText += " Warning: High PM2.5 levels detected! Please take precautions.";
                    }
                } else if ("turn_on_fan".equals(intentName)) {
                    lastFanState.setFanState(true);
                    lastFanState.setMode("MANUAL");
                    messagingTemplate.convertAndSend("/topic/fanControl", lastFanState);
                    responseText = "The fan has been turned on.";
                } else if ("turn_off_fan".equals(intentName)) {
                    lastFanState.setFanState(false);
                    lastFanState.setMode("MANUAL");
                    messagingTemplate.convertAndSend("/topic/fanControl", lastFanState);
                    responseText = "The fan has been turned off.";
                } else if ("get_fan_history".equals(intentName)) {
                    try {
                        RestTemplate restTemplate = new RestTemplate();
                        String url = "http://localhost:8080/api/sensor/fanHistory";
                        FanHistory[] fanHistoryArray = restTemplate.getForObject(url, FanHistory[].class);
                        List<FanHistory> fanHistory = fanHistoryArray != null ? List.of(fanHistoryArray) : null;

                        if (fanHistory != null && !fanHistory.isEmpty()) {
                            // Sắp xếp theo thời gian giảm dần
                            fanHistory = fanHistory.stream()
                                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                                    .collect(Collectors.toList());

                            StringBuilder historyText = new StringBuilder("Here are the recent fan states: ");
                            for (int i = 0; i < Math.min(3, fanHistory.size()); i++) {
                                FanHistory history = fanHistory.get(i);
                                boolean fanState = history.isFanState();
                                String mode = history.getMode() != null ? history.getMode() : "UNKNOWN";
                                String timestamp = history.getTimestamp() != null ? history.getTimestamp().toString() : "UNKNOWN";
                                historyText.append("On ").append(timestamp).append(": The fan was ")
                                        .append(fanState ? "on" : "off").append(" in ").append(mode.toLowerCase()).append(" mode. ");
                            }
                            responseText = historyText.toString();
                        } else {
                            responseText = "There are no recent fan state changes.";
                        }
                    } catch (Exception e) {
                        logger.severe("Error in get_fan_history: " + e.getMessage());
                        responseText = "Error retrieving fan history. Please try again later.";
                    }
                } else if ("check_for_alerts".equals(intentName)) {
                    try {
                        RestTemplate restTemplate = new RestTemplate();
                        String url = "http://localhost:8080/api/sensor/checkAlert";
                        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                        if (response != null) {
                            String alertMessage = response.get("alertMessage") != null ? response.get("alertMessage").toString() : null;
                            responseText = (alertMessage != null && !alertMessage.isEmpty()) ? alertMessage : "There are no air quality alerts at the moment.";
                        } else {
                            responseText = "Error retrieving alerts. Please try again later.";
                        }
                    } catch (Exception e) {
                        logger.severe("Error in check_for_alerts: " + e.getMessage());
                        responseText = "Error checking for alerts. Please try again later.";
                    }
                } else if ("set_fan_mode".equals(intentName)) {
                    try {
                        Map<String, Object> parameters = request.getQueryResult().getParameters();
                        if (parameters != null && parameters.containsKey("mode")) {
                            String mode = parameters.get("mode") != null ? parameters.get("mode").toString().toUpperCase() : null;
                            if ("AUTO".equals(mode) || "MANUAL".equals(mode)) {
                                lastFanState.setMode(mode);
                                messagingTemplate.convertAndSend("/topic/fanControl", lastFanState);
                                responseText = "The fan mode has been set to " + mode.toLowerCase() + ".";
                            } else {
                                responseText = "Invalid mode. Please choose auto or manual.";
                            }
                        } else {
                            responseText = "Please specify a mode (auto or manual).";
                        }
                    } catch (Exception e) {
                        logger.severe("Error in set_fan_mode: " + e.getMessage());
                        responseText = "Error setting fan mode. Please try again later.";
                    }
                } else if ("get_alert_history".equals(intentName)) {
                    try {
                        RestTemplate restTemplate = new RestTemplate();
                        String url = "http://localhost:8080/api/sensor/alertHistory";
                        AlertHistory[] alertHistoryArray = restTemplate.getForObject(url, AlertHistory[].class);
                        List<AlertHistory> alertHistory = alertHistoryArray != null ? List.of(alertHistoryArray) : null;

                        if (alertHistory != null && !alertHistory.isEmpty()) {
                            // Sắp xếp theo thời gian giảm dần
                            alertHistory = alertHistory.stream()
                                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                                    .collect(Collectors.toList());

                            StringBuilder historyText = new StringBuilder("Here are the recent air quality alerts: ");
                            for (int i = 0; i < Math.min(3, alertHistory.size()); i++) {
                                AlertHistory alert = alertHistory.get(i);
                                String message = alert.getMessage() != null ? alert.getMessage() : "No message";
                                String timestamp = alert.getTimestamp() != null ? alert.getTimestamp().toString() : "UNKNOWN";
                                historyText.append("On ").append(timestamp).append(": ").append(message).append(". ");
                            }
                            responseText = historyText.toString();
                        } else {
                            responseText = "There are no recent air quality alerts.";
                        }
                    } catch (Exception e) {
                        logger.severe("Error in get_alert_history: " + e.getMessage());
                        responseText = "Error retrieving alert history. Please try again later.";
                    }
                } else if ("get_air_quality_history".equals(intentName)) {
                    try {
                        RestTemplate restTemplate = new RestTemplate();
                        String url = "http://localhost:8080/api/sensor/latest";
                        AirQualityData[] airQualityHistoryArray = restTemplate.getForObject(url, AirQualityData[].class);
                        List<AirQualityData> airQualityHistory = airQualityHistoryArray != null ? List.of(airQualityHistoryArray) : null;

                        if (airQualityHistory != null && !airQualityHistory.isEmpty()) {
                            // Sắp xếp theo thời gian giảm dần
                            airQualityHistory = airQualityHistory.stream()
                                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                                    .collect(Collectors.toList());

                            StringBuilder historyText = new StringBuilder("Here are the recent air quality data: ");
                            for (int i = 0; i < Math.min(3, airQualityHistory.size()); i++) {
                                AirQualityData data = airQualityHistory.get(i);
                                int dustLevel = data.getDustLevel();
                                float temperature = data.getTemperature();
                                float humidity = data.getHumidity();
                                String timestamp = data.getTimestamp() != null ? data.getTimestamp().toString() : "UNKNOWN";
                                historyText.append("On ").append(timestamp).append(": PM2.5 was ")
                                        .append(dustLevel).append(" micrograms per cubic meter, temperature was ")
                                        .append(temperature).append(" degrees Celsius, and humidity was ")
                                        .append(humidity).append(" percent. ");
                            }
                            responseText = historyText.toString();
                        } else {
                            responseText = "There are no recent air quality data.";
                        }
                    } catch (Exception e) {
                        logger.severe("Error in get_air_quality_history: " + e.getMessage());
                        responseText = "Error retrieving air quality history. Please try again later.";
                    }
                } else {
                    logger.warning("⚠️ Unrecognized intent: " + intentName);
                    responseText = "I don't understand your request.";
                }
            } else {
                logger.warning("⚠️ Request or QueryResult is null: " + request);
                responseText = "Invalid request.";
            }
        } catch (Exception e) {
            logger.severe("❌ Error processing webhook request: " + e.getMessage());
            responseText = "System error, please try again later.";
        }

        Map<String, Object> response = new HashMap<>();
        response.put("fulfillmentText", responseText);
        response.put("source", "AirGuard");
        logger.info("✅ Response sent: " + response);
        return response;
    }
}