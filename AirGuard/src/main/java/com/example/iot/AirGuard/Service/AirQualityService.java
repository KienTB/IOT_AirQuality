package com.example.iot.AirGuard.Service;

import com.example.iot.AirGuard.Model.AirQualityData;
import com.example.iot.AirGuard.Repository.AirQualityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AirQualityService {
    @Autowired
    private AirQualityRepository repository;

    public AirQualityData getLatestAirQualityData(){
        List<AirQualityData> latestData = repository.findTop10ByOrderByTimestampDesc();
        if (latestData.isEmpty()){
            throw new RuntimeException("No air quality data available");
        }
        return latestData.get(0);
    }
}
