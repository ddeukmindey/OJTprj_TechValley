package com.techvalley.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI authOpenAPI() {

        return new OpenAPI()

                .info(

                        new Info()

                                .title("Tech Valley Auth API")

                                .description("Authentication Service")

                                .version("1.0.0")
                );
    }

}