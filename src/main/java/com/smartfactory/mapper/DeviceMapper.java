package com.smartfactory.mapper;

import com.smartfactory.entity.Device;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DeviceMapper {

    List<Device> findAll();

    Device findById(Long id);

    int insert(Device device);

    int update(Device device);

    int deleteById(Long id);
}