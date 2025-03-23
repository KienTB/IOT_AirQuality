package com.example.iot.AirGuard.Repository;

import com.example.iot.AirGuard.Model.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {
}
