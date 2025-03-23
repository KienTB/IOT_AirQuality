package com.example.iot.AirGuard.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DialogflowRequest {
    private String responseId;
    private QueryResult queryResult;
    private Map<String, Object> originalDetectIntentRequest;
    private String session;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryResult {
        private String queryText;
        private String action;
        private Map<String, Object> parameters;
        private boolean allRequiredParamsPresent;
        private String fulfillmentText;
        private List<Map<String, Object>> fulfillmentMessages;
        private Intent intent;
        private double intentDetectionConfidence;
        private String languageCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Intent {
        private String name;
        private String displayName;
    }
}