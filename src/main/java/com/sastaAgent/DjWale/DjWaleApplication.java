package com.sastaAgent.DjWale;

import com.sastaAgent.DjWale.config.LiveDjProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LiveDjProperties.class)
public class DjWaleApplication {

	public static void main(String[] args) {
		SpringApplication.run(DjWaleApplication.class, args);
	}

}
