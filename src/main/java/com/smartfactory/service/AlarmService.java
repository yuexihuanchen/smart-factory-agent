package com.smartfactory.service;

import com.smartfactory.common.response.PageResult;
import com.smartfactory.dto.AlarmCreateRequest;
import com.smartfactory.dto.AlarmQueryRequest;
import com.smartfactory.entity.Alarm;

public interface AlarmService {

    Alarm create(AlarmCreateRequest request);

    PageResult<Alarm> findPage(AlarmQueryRequest request);

    Alarm findById(Long id);

    Alarm acknowledge(Long id, String username);

    Alarm resolve(Long id);
}
