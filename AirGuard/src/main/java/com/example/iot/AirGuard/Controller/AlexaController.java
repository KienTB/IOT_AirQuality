package com.example.iot.AirGuard.Controller;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.v2.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/alexa")
public class AlexaController {

    private static final Logger logger = Logger.getLogger(AlexaController.class.getName());
    private static final String PROJECT_ID = "airguardassistant";
    private static final String SESSION_ID = "alexa-session";

    @Autowired
    private GoogleCredentials dialogflowCredentials;

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleAlexaRequest(@RequestBody Map<String, Object> request) {
        logger.info("===> Alexa request received: " + request);

        Map<String, Object> requestMap = (Map<String, Object>) request.get("request");
        String requestType = (String) requestMap.get("type");

        String responseText;
        if ("LaunchRequest".equals(requestType)) {
            responseText = "Welcome to AirGuard! You can say things like 'turn on the fan' or 'check the air quality'.";
        } else if ("IntentRequest".equals(requestType)) {
            Map<String, Object> intentMap = (Map<String, Object>) requestMap.get("intent");
            String intentName = intentMap.get("name").toString();
            logger.info("👉 Detected Alexa Intent: " + intentName);

            String queryText;
            switch (intentName) {
                case "TurnOnFanIntent":
                    queryText = "turn on the fan";
                    break;
                case "TurnOffFanIntent":
                    queryText = "turn off the fan";
                    break;
                case "CheckAirQualityIntent":
                    queryText = "how is the air quality";
                    break;
                case "GetFanHistoryIntent":
                    queryText = "get fan history";
                    break;
                case "CheckForAlertsIntent":
                    queryText = "check for alerts";
                    break;
                case "SetFanModeIntent":
                    // Lấy giá trị slot "mode" nếu có
                    Map<String, Object> slots = (Map<String, Object>) intentMap.get("slots");
                    String mode = "auto"; // Giá trị mặc định
                    if (slots != null) {
                        Map<String, Object> modeSlot = (Map<String, Object>) slots.get("mode");
                        if (modeSlot != null) {
                            Map<String, Object> valueMap = (Map<String, Object>) modeSlot.get("value");
                            if (valueMap != null) {
                                mode = valueMap.get("value").toString().toLowerCase();
                            }
                        }
                    }
                    queryText = "set fan mode to " + mode;
                    break;
                case "GetAlertHistoryIntent":
                    queryText = "get alert history";
                    break;
                case "GetAirQualityHistoryIntent":
                    queryText = "get air quality history";
                    break;
                default:
                    queryText = intentName;
            }

            responseText = sendToDialogflow(queryText);
        } else {
            responseText = "Sorry, I couldn't understand your request.";
        }

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> outputSpeech = new HashMap<>();
        outputSpeech.put("type", "PlainText");
        outputSpeech.put("text", responseText);

        response.put("version", "1.0");
        response.put("response", new HashMap<String, Object>() {{
            put("outputSpeech", outputSpeech);
            put("shouldEndSession", false);
        }});

        logger.info("✅ Alexa response sent: " + response);
        return ResponseEntity.ok(response);
    }

    private String sendToDialogflow(String queryText) {
        try {
            SessionsClient sessionsClient = SessionsClient.create(
                    SessionsSettings.newBuilder()
                            .setCredentialsProvider(() -> dialogflowCredentials)
                            .build()
            );

            SessionName session = SessionName.of(PROJECT_ID, SESSION_ID);

            TextInput.Builder textInput = TextInput.newBuilder()
                    .setText(queryText)
                    .setLanguageCode("en-US");

            QueryInput queryInput = QueryInput.newBuilder()
                    .setText(textInput)
                    .build();

            DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);

            return response.getQueryResult().getFulfillmentText();
        } catch (Exception e) {
            logger.severe("❌ Error sending to Dialogflow: " + e.getMessage());
            return "Sorry, I couldn't process your request.";
        }
    }
}