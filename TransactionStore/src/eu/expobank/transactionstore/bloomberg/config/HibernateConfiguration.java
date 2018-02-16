package eu.expobank.transactionstore.bloomberg.config;

import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@ComponentScan(basePackages = { "eu.expobank.transactionstore.bloomberg.dao" })
@EnableTransactionManagement
@PropertySource("classpath:database.properties")
public class HibernateConfiguration {
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public static DataSource dataSource(@Value("${server.url}") String url, @Value("${server.driver}") String driver,
			@Value("${server.login}") String login, @Value("${server.password}") String pass) {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setDriverClassName(driver);
		hikariConfig.setJdbcUrl(url);
		hikariConfig.setUsername(login);
		hikariConfig.setPassword(pass);
		final HikariDataSource ds = new HikariDataSource(hikariConfig);
		return ds;
	}

	@Bean
	public Properties hibernateProperties(@Value("${hibernate.dialect}") String dialect,
			@Value("${hibernate.show_sql}") boolean showSql, @Value("${hibernate.format_sql}") boolean formatSql,
			@Value("${hibernate.hbm2ddl.auto}") String hbm2ddl) {
		Properties properties = new Properties();
		properties.put("hibernate.dialect", dialect);
		properties.put("hibernate.show_sql", showSql);
		properties.put("hibernate.format_sql", formatSql);
		properties.put("hibernate.hbm2ddl.auto", hbm2ddl);
		return properties;
	}

	@Bean
	public SessionFactory sessionFactory(DataSource dataSource,
			@Value("${hibernate.packagesToScan}") String packagesToScan,
			@Qualifier("hibernateProperties") Properties properties) throws Exception {
		LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
		sessionFactoryBean.setDataSource(dataSource);
		sessionFactoryBean.setPackagesToScan(packagesToScan);
		sessionFactoryBean.setHibernateProperties(properties);
		sessionFactoryBean.afterPropertiesSet();
		return sessionFactoryBean.getObject();
	}
	
	@Bean
	public PlatformTransactionManager transactionManager(SessionFactory sessionFactory) {
		return new HibernateTransactionManager(sessionFactory);
	}
}