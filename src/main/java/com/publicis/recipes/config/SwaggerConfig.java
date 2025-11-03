package com.publicis.recipes.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Recipes API",
                version = "1.0",
                description = "Publicis Sapient ASDE - Recipes Orchestration API"
        )
)
public class SwaggerConfig {
	
}