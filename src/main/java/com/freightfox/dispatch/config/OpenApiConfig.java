package com.freightfox.dispatch.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "FreightFox Dispatch Load Balancer API", version = "1.0.0",
        description = "Priority-aware vehicle allocation with capacity checks and Haversine travel distances."))
public class OpenApiConfig {}
