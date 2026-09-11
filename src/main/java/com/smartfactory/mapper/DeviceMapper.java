package com.smartfactory.mapper;

import com.smartfactory.entity.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceMapper {

    List<Device> findPage(
        @Param("offset") int offset,
        @Param("size") int size,
        @Param("keyword") String keyword,
        @Param("deviceType") String deviceType,
        @Param("status") String status,
        @Param("protocol") String protocol
);

long count(
        @Param("keyword") String keyword,
        @Param("deviceType") String deviceType,
        @Param("status") String status,
        @Param("protocol") String protocol
);

    Device findById(Long id);

    int insert(Device device);

    int update(Device device);

    int deleteById(Long id);
}