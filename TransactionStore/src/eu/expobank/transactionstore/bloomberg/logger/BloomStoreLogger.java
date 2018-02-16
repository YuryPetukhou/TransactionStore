package eu.expobank.transactionstore.bloomberg.logger;

import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class BloomStoreLogger {
	private Logger logger;

	public BloomStoreLogger() {
		createLogger();

	}

	private void createLogger() {
		String nameFile = "log4j.properties";
		PropertyConfigurator.configure(nameFile);

		logger = Logger.getRootLogger();
		Enumeration<Logger> append = logger.getAllAppenders();
		while (append.hasMoreElements()) {
			logger.info("Available appender " + append.nextElement());
		}
		logger.info("Hi Logger info!");

	}

	
	public void error(String message) {

		System.out.println(message);
		logger.error(message);
	}

	public void error(String message, Exception e) {

		System.out.println(message);
		logger.error(message, e);
	}

	public void warn(String message) {

		System.out.println(message);
		logger.warn(message);
	}

	public void info(String message) {

		logger.info(message);
	}

	public void debug(String message) {

		logger.debug(message);
	}

	public void notifyException(Exception e) {
		logger.error(e.getMessage() + "\n" + e.getStackTrace());
	}

	
}
