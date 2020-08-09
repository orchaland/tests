package com.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import orcha.lang.configuration.DatabaseAdapter;
import orcha.lang.configuration.DatabaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class PersistenceContext {

    @Bean
    DatabaseAdapter databaseAdapter(){
        return new DatabaseAdapter();
    }

    @Autowired
    DatabaseAdapter databaseAdapter;

    @Bean(destroyMethod = "close")
    DataSource dataSource(Environment env) {
        HikariConfig dataSourceConfig = new HikariConfig();
        //dataSourceConfig.setDriverClassName(env.getRequiredProperty("db.driver"));
        dataSourceConfig.setDriverClassName(databaseAdapter.getConnection().getDriver());
        //dataSourceConfig.setJdbcUrl(env.getRequiredProperty("db.url"));
        dataSourceConfig.setJdbcUrl(databaseAdapter.getConnection().getUrl());
        //dataSourceConfig.setUsername(env.getRequiredProperty("db.username"));
        dataSourceConfig.setUsername(databaseAdapter.getConnection().getUsername());
        //dataSourceConfig.setUsername(env.getRequiredProperty("db.username"));
        dataSourceConfig.setUsername(databaseAdapter.getConnection().getPassword());

        return new HikariDataSource(dataSourceConfig);
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
                                                                Environment env) {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setDataSource(dataSource);
        entityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        entityManagerFactoryBean.setPackagesToScan(databaseAdapter.getConnection().getEntityScanPackage());

        Properties jpaProperties = new Properties();

        jpaProperties.put("hibernate.dialect", databaseAdapter.getHibernateConfig().getDialect());
        jpaProperties.put("hibernate.hbm2ddl.auto", databaseAdapter.getHibernateConfig().getHbm2ddlAuto());
        jpaProperties.put("hibernate.ejb.naming_strategy", databaseAdapter.getHibernateConfig().getEjbNamingStrategy());
        jpaProperties.put("hibernate.show_sql", databaseAdapter.getHibernateConfig().getShowSql());
        jpaProperties.put("hibernate.format_sql", databaseAdapter.getHibernateConfig().getFormatSql());

        //Configures the used database dialect. This allows Hibernate to create SQL
        //that is optimized for the used database.
//        jpaProperties.put("hibernate.dialect", env.getRequiredProperty("hibernate.dialect"));

        //Specifies the action that is invoked to the database when the Hibernate
        //SessionFactory is created or closed.
        /*jpaProperties.put("hibernate.hbm2ddl.auto",
                env.getRequiredProperty("hibernate.hbm2ddl.auto")
        );*/

        //Configures the naming strategy that is used when Hibernate creates
        //new database objects and schema elements
        /*jpaProperties.put("hibernate.ejb.naming_strategy",
                env.getRequiredProperty("hibernate.ejb.naming_strategy")
        );*/

        //If the value of this property is true, Hibernate writes all SQL
        //statements to the console.
        /*jpaProperties.put("hibernate.show_sql",
                env.getRequiredProperty("hibernate.show_sql")
        );*/

        //If the value of this property is true, Hibernate will format the SQL
        //that is written to the console.
        /*jpaProperties.put("hibernate.format_sql",
                env.getRequiredProperty("hibernate.format_sql")
        );*/

        entityManagerFactoryBean.setJpaProperties(jpaProperties);

        return entityManagerFactoryBean;
    }

    @Bean
    JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }
}
