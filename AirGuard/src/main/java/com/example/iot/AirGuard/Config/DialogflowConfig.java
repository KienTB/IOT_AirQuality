package com.example.iot.AirGuard.Config;

import com.google.api.client.util.Value;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

@Configuration
public class DialogflowConfig {
    @Value("${dialogflow.credentials.path}")
    private String credentialsPath;

    @Bean
    public GoogleCredentials dialogflowCredentials() throws IOException {
        return GoogleCredentials.fromStream(
                        new FileInputStream("E:\\Dowloads\\IOT\\AirGuard\\AirGuard\\src\\main\\resources\\dialogflow-credentials.json"))
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));
    }
}