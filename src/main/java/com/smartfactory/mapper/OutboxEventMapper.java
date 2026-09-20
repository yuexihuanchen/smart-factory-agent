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

    List<OutboxEvent> findClaimable(
            @Param("limit") int limit,
            @Param("now") LocalDateTime now
    );

    OutboxEvent findBySourceAndEventId(
            @Param("source") String source,
            @Param("eventId") String eventId
    );

    int markSent(
            @Param("id") Long id,
            @Param("leaseOwner") String leaseOwner,
            @Param("publishedAt") LocalDateTime publishedAt
    );

    int markPublishFailure(
            @Param("id") Long id,
            @Param("leaseOwner") String leaseOwner,
            @Param("lastError") String lastError,
            @Param("maxRetries") int maxRetries
    );

    int claim(
            @Param("id") Long id,
            @Param("leaseOwner") String leaseOwner,
            @Param("leaseUntil") LocalDateTime leaseUntil,
            @Param("now") LocalDateTime now
    );
}
