package com.enterprise.coreapi;

import org.springframework.boot.SpringApplication;

public class TestCoreApiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(CoreApiServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
