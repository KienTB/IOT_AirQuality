package com.example.iot.AirGuard.Repository;

import com.example.iot.AirGuard.Model.AirQualityData;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AirQualityRepository extends JpaRepository<AirQualityData, Long> {
    List<AirQualityData> findTop10ByOrderByTimestampDesc();

    @Query("SELECT a.id FROM AirQualityData a ORDER BY a.timestamp ASC LIMIT ?1")
    List<Long> findOldestRecords(int count);

    @Transactional
    @Modifying
    @Query("DELETE FROM AirQualityData a WHERE a.id IN ?1")
    void deleteByIds(List<Long> ids);
}
