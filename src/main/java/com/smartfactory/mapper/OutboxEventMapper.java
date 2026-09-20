package com.smartfactory.mapper;

import com.smartfactory.entity.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OutboxEventMapper {

    int insert(OutboxEvent event);

    List<OutboxEvent> findPending(
            @Param("limit") int limit
    );

    OutboxEvent findBySourceAndEventId(
            @Param("source") String source,
            @Param("eventId") String eventId
    );

    int markSent(
            @Param("id") Long id,
            @Param("publishedAt") LocalDateTime publishedAt
    );

    int markPublishFailure(
            @Param("id") Long id,
            @Param("lastError") String lastError
    );
}
