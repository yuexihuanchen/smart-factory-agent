package com.smartfactory.controller;

import com.smartfactory.common.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Result<Map<String, Object>> home() {

        Map<String, Object> data = Map.of(
                "project", "SmartFactory-Agent",
                "version", "0.0.1-SNAPSHOT",
                "status", "UP",
                "message", "SmartFactory-Agent API is running"
        );

        return Result.success(data);
    }
}