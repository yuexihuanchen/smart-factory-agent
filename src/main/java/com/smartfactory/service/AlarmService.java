package com.smartfactory.service;

import com.smartfactory.common.response.PageResult;
import com.smartfactory.dto.AlarmQueryRequest;
import com.smartfactory.entity.Alarm;
import com.smartfactory.service.command.AlarmEventCommand;
import com.smartfactory.vo.AlarmProcessResponse;

public interface AlarmService {

    AlarmProcessResponse processEvent(AlarmEventCommand command);

    PageResult<Alarm> findPage(AlarmQueryRequest request);

    Alarm findById(Long id);

    Alarm acknowledge(Long id, String username);

    Alarm resolve(Long id);
}
