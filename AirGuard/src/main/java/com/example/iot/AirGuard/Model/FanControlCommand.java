package com.example.iot.AirGuard.Model;

import lombok.*;

@Data
@NoArgsConstructor
public class FanControlCommand {
    private Boolean fanState;
    private String mode;
    public FanControlCommand(Boolean fanState, String mode) {
        this.fanState = fanState;
        this.mode = mode;
    }
}
