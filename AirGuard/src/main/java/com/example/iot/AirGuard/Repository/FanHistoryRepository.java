package com.example.iot.AirGuard.Repository;

import com.example.iot.AirGuard.Model.FanHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FanHistoryRepository extends JpaRepository<FanHistory, Long> {
}
