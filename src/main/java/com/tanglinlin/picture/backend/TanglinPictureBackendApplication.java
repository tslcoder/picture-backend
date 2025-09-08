package com.tanglinlin.picture.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication(scanBasePackages = {"com.tanglinlin.picture.backend"})
@MapperScan("com.tanglinlin.picture.backend.generator.mapper")
public class TanglinPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TanglinPictureBackendApplication.class, args);
    }

}
