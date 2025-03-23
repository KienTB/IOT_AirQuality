package com.example.iot.AirGuard.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AirQualityData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private float temperature;
    private float humidity;
    private int dustLevel;

    @Column(name = "fan_state")
    private Boolean fanState;
    private String mode;

    private LocalDateTime timestamp = LocalDateTime.now();
}
