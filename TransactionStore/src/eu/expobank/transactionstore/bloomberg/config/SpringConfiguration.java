package eu.expobank.transactionstore.bloomberg.config;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import com.bloomberglp.blpapi.SessionOptions;

import eu.expobank.transactionstore.bloomberg.logger.BloomStoreLogger;

@Configuration
@PropertySource("classpath:application.properties")
@Import({ HibernateConfiguration.class })
@ComponentScan(basePackages= {"eu.expobank.transactionstore"})
public class SpringConfiguration {

	@Bean
	public BloomStoreLogger logger() {
		return new BloomStoreLogger();
	}

	@Bean
	public SessionOptions sessionOptions(@Value("${bloomberg.hostname}") String hostname,
			@Value("${bloomberg.port}") int port) {
		System.out.println("Terminal hostname:" +hostname);
		System.out.println("Terminal port:" +port);
		SessionOptions sessionOptions = new SessionOptions();
		sessionOptions.setServerHost(hostname);
		sessionOptions.setServerPort(port);
		return sessionOptions;
	}
}
