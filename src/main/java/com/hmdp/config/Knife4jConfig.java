package com.hmdp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI hmdpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("黑马点评 API 文档")
                        .description("hm-dianping 项目接口文档（基于 Knife4j + OpenAPI3）")
                        .version("v1.0.0")
                        .contact(new Contact().name("GW")));
    }
}
