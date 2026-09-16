package com.smartfactory.mapper;

import com.smartfactory.entity.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OutboxEventMapper {

    int insert(OutboxEvent event);

    OutboxEvent findBySourceAndEventId(
            @Param("source") String source,
            @Param("eventId") String eventId
    );
}
