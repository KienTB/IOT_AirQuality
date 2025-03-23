package com.example.iot.AirGuard.Config;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

@Configuration
public class DialogflowConfig {

    @Bean
    public GoogleCredentials dialogflowCredentials() throws IOException {
        // Đọc file JSON key từ thư mục resources
        return GoogleCredentials.fromStream(
                        new FileInputStream("E:\\Dowloads\\Dowloads\\IOT\\AirGuard\\AirGuard\\src\\main\\resources\\dialogflow-credentials.json"))
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));
    }
}