package com.example.iot.AirGuard.Controller;

import com.example.iot.AirGuard.Model.AlertHistory;
import com.example.iot.AirGuard.Model.FanHistory;
import com.example.iot.AirGuard.Repository.AirQualityRepository;
import com.example.iot.AirGuard.Model.AirQualityData;
import com.example.iot.AirGuard.Model.FanControlCommand;
import com.example.iot.AirGuard.Repository.AlertHistoryRepository;
import com.example.iot.AirGuard.Repository.FanHistoryRepository;
import com.example.iot.AirGuard.Service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sensor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AirQualityController {
    private final AirQualityRepository repository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final AlertHistoryRepository alertHistoryRepository;
    private final FanHistoryRepository fanHistoryRepository;

    private FanControlCommand lastFanState = new FanControlCommand(false, "AUTO");
    private static final double PM25_THRESHOLD = 50.0;
    private String alertMessage = null;
    private boolean alertSent = false;

    private Instant lastEmailSentTime = Instant.MIN;

    @PostMapping("/data")
    public ResponseEntity<String> receiveSensorData(@RequestBody AirQualityData data) {
        try {
            if (data == null) {
                return ResponseEntity.badRequest().body("Dữ liệu rỗng");
            }

            if (data.getTemperature() < -50 || data.getHumidity() < 0 || data.getDustLevel() < 0) {
                return ResponseEntity.badRequest().body("Giá trị cảm biến không hợp lệ");
            }

            if (data.getTimestamp() == null) data.setTimestamp(LocalDateTime.now());

            // Chuẩn hóa fanState trước khi lưu
            boolean newFanState = false;
            Object fanStateObj = data.getFanState();
            if (fanStateObj != null) {
                if (fanStateObj instanceof Boolean) {
                    newFanState = (Boolean) fanStateObj;
                } else if (fanStateObj instanceof String) {
                    String fanStateStr = ((String) fanStateObj).trim().toUpperCase();
                    newFanState = "ON".equals(fanStateStr) || "TRUE".equals(fanStateStr);
                } else {
                    System.err.println("⚠️ FanState không phải Boolean hoặc String. Nhận: " + fanStateObj.getClass().getName());
                }
            }
            data.setFanState(newFanState); // Cập nhật fanState thành Boolean trước khi lưu

            String newMode = data.getMode() != null ? data.getMode().toUpperCase() : "AUTO";
            data.setMode(newMode);

            int dustLevel = data.getDustLevel();
            if ("AUTO".equals(newMode)) {
                newFanState = dustLevel > PM25_THRESHOLD;
            }
            data.setFanState(newFanState);

            // Kiểm tra PM2.5 và gửi email nếu vượt ngưỡng
            Instant now = Instant.now();
            if (dustLevel > PM25_THRESHOLD && !alertSent &&
                    Duration.between(lastEmailSentTime, now).getSeconds() >= 20) {

                alertMessage = "High PM2.5 levels detected! The current level is " + dustLevel + " micrograms per cubic meter. Please take precautions.";
                emailService.sendEmail(
                        "kien14122003@gmail.com",
                        "Air Quality Alert",
                        alertMessage
                );
                // Lưu vào lịch sử cảnh báo
                AlertHistory alertHistory = new AlertHistory();
                alertHistory.setMessage(alertMessage);
                alertHistory.setTimestamp(LocalDateTime.now());
                alertHistoryRepository.save(alertHistory);
                alertSent = true;
                lastEmailSentTime = now; // Cập nhật thời điểm gửi email gần nhất
            } else if (dustLevel <= PM25_THRESHOLD) {
                alertMessage = null;
                alertSent = false;
            }

            repository.save(data);

            if (lastFanState.getFanState() != newFanState || !lastFanState.getMode().equals(newMode)) {
                lastFanState.setFanState(newFanState);
                lastFanState.setMode(newMode);
                messagingTemplate.convertAndSend("/topic/fanControl", lastFanState);
                System.out.println("Cập nhật từ sensor - Quạt: " +
                        (newFanState ? "BẬT" : "TẮT") + ", Chế độ: " + newMode);
                // Lưu vào lịch sử trạng thái quạt
                FanHistory fanHistory = new FanHistory();
                fanHistory.setFanState(newFanState);
                fanHistory.setMode(newMode);
                fanHistory.setTimestamp(LocalDateTime.now());
                fanHistoryRepository.save(fanHistory);
            }

            messagingTemplate.convertAndSend("/topic/airQuality", data);
            return ResponseEntity.ok("Dữ liệu nhận thành công");

        } catch (Exception e) {
            System.err.println("Lỗi xử lý dữ liệu: " + e.getMessage());
            return ResponseEntity.status(500).body("Lỗi server: " + e.getMessage());
        }
    }

    @GetMapping("/latest")
    public List<AirQualityData> getLatestData() {
        return repository.findTop10ByOrderByTimestampDesc();
    }

    @GetMapping("/fanState")
    public FanControlCommand getFanState() {
        return lastFanState;
    }

    @PostMapping("/fanState")
    public ResponseEntity<FanControlCommand> setFanState(@RequestBody FanControlCommand command) {
        try {
            if (command == null || command.getFanState() == null) {
                return ResponseEntity.badRequest().body(null);
            }

            String mode = command.getMode() != null ? command.getMode().toUpperCase() : lastFanState.getMode();
            lastFanState.setFanState(command.getFanState());
            lastFanState.setMode(mode);

            System.out.println("Cập nhật từ UI - Quạt: " +
                    (command.getFanState() ? "BẬT" : "TẮT") + ", Chế độ: " + mode);
            messagingTemplate.convertAndSend("/topic/fanControl", lastFanState);

            // Lưu vào lịch sử trạng thái quạt
            FanHistory fanHistory = new FanHistory();
            fanHistory.setFanState(command.getFanState());
            fanHistory.setMode(mode);
            fanHistory.setTimestamp(LocalDateTime.now());
            fanHistoryRepository.save(fanHistory);

            return ResponseEntity.ok(lastFanState);

        } catch (Exception e) {
            System.err.println("Lỗi cập nhật trạng thái quạt: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/fanHistory")
    public List<FanHistory> getFanHistory() {
        return fanHistoryRepository.findAll();
    }

    @GetMapping("/alertHistory")
    public List<AlertHistory> getAlertHistory() {
        return alertHistoryRepository.findAll();
    }

}