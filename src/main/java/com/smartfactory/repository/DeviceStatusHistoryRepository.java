package com.smartfactory.repository;

import com.smartfactory.entity.DeviceStatusHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeviceStatusHistoryRepository
        extends MongoRepository<DeviceStatusHistory, String> {
}