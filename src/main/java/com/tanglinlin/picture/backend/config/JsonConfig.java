package com.tanglinlin.picture.backend.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * @program: tanglin-picture-backend
 * @ClassName JsonConfig
 * @description:
 * @author: TSL
 * @create: 2025-08-31 23:04
 * @Version 1.0
 **/
@Configuration
public class JsonConfig {

    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        com.fasterxml.jackson.databind.ObjectMapper build = builder.createXmlMapper(false).build();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        build.registerModule(simpleModule);
        return build;
    }

}
