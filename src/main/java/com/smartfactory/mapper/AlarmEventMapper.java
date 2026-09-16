package com.smartfactory.mapper;

import com.smartfactory.entity.AlarmEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlarmEventMapper {

    int insert(AlarmEvent event);

    AlarmEvent findBySourceAndEventId(
            @Param("source") String source,
            @Param("eventId") String eventId
    );

    int updateAlarmId(
            @Param("id") Long id,
            @Param("alarmId") Long alarmId
    );
}
