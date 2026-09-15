package com.smartfactory.mapper;

import com.smartfactory.entity.Alarm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AlarmMapper {

    List<Alarm> findPage(
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("deviceId") Long deviceId,
            @Param("alarmCode") String alarmCode,
            @Param("alarmLevel") String alarmLevel,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    long count(
            @Param("deviceId") Long deviceId,
            @Param("alarmCode") String alarmCode,
            @Param("alarmLevel") String alarmLevel,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    Alarm findById(Long id);

    Alarm findOpenByDeviceIdAndAlarmCode(
            @Param("deviceId") Long deviceId,
            @Param("alarmCode") String alarmCode
    );

    int upsert(Alarm alarm);

    int acknowledge(
            @Param("id") Long id,
            @Param("acknowledgedAt") LocalDateTime acknowledgedAt,
            @Param("acknowledgedBy") Long acknowledgedBy
    );

    int resolve(
            @Param("id") Long id,
            @Param("resolvedAt") LocalDateTime resolvedAt
    );
}
