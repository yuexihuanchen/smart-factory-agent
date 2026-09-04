package com.smartfactory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.smartfactory.mapper")
public class SmartFactoryAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartFactoryAgentApplication.class, args);
	}

}
