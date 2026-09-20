package com.smartfactory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.smartfactory.mapper")
@EnableScheduling
public class SmartFactoryAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartFactoryAgentApplication.class, args);
	}

}
