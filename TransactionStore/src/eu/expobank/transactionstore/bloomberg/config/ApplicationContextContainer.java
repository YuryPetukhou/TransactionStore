package eu.expobank.transactionstore.bloomberg.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ApplicationContextContainer {
	private static volatile ApplicationContext context;

	private static synchronized ApplicationContext getContext() {
		if (context == null) {
			context = new AnnotationConfigApplicationContext(SpringConfiguration.class);				
		}
		return context;
	}

	public static synchronized Object getBean(Class<?> type) {
		ApplicationContext contextCurrent = getContext();
		return contextCurrent.getBean(type);
	}
	public static synchronized Object getBean(String typeName) {
		ApplicationContext contextCurrent = getContext();
		return contextCurrent.getBean(typeName);
	}
}
