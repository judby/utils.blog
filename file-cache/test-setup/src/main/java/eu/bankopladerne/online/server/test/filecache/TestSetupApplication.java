package eu.bankopladerne.online.server.test.filecache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationPropertiesScan
@EnableConfigurationProperties
@SpringBootApplication
public class TestSetupApplication {
	public static void main(String[] args) {
		SpringApplication.run(TestSetupApplication.class, args);
	}
}
