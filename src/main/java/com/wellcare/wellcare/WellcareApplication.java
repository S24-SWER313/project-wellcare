package com.wellcare.wellcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class WellcareApplication {

	public static void main(String[] args) {
		SpringApplication.run(WellcareApplication.class, args);
	}

	@Configuration
    public static class WebConfig implements WebMvcConfigurer {

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/files/**") // Define the URL pattern to serve the resources
                    .addResourceLocations("file:upload-dir/") // Specify the location of the uploaded files
                    .setCachePeriod(0); // Disable caching for development (set to 0)
        }
    }
}
