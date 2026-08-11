package com.healthcare.system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${MYSQLHOST:}")
    private String mysqlHost;

    @Value("${MYSQLUSER:root}")
    private String mysqlUser;

    @Value("${MYSQLPASSWORD:nivetha@123}")
    private String mysqlPassword;

    @Value("${MYSQLPORT:3306}")
    private String mysqlPort;

    @Value("${MYSQLDATABASE:healthcare_db}")
    private String mysqlDatabase;

    @Bean
    public DataSource dataSource() {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        
        // Render environment variable is set to "true" on Render hosting
        String isRender = System.getenv("RENDER");
        
        if ("true".equals(isRender) && (mysqlHost == null || mysqlHost.isEmpty())) {
            // Fallback to in-memory H2 database for smooth zero-config deployment on Render
            dataSourceBuilder.driverClassName("org.h2.Driver");
            dataSourceBuilder.url("jdbc:h2:mem:healthcare_db;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL");
            dataSourceBuilder.username("sa");
            dataSourceBuilder.password("");
        } else {
            // Connect to MySQL (for local development or when MYSQLHOST is defined on Render)
            String host = (mysqlHost == null || mysqlHost.isEmpty()) ? "localhost" : mysqlHost;
            dataSourceBuilder.driverClassName("com.mysql.cj.jdbc.Driver");
            dataSourceBuilder.url("jdbc:mysql://" + host + ":" + mysqlPort + "/" + mysqlDatabase + 
                                  "?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
            dataSourceBuilder.username(mysqlUser);
            dataSourceBuilder.password(mysqlPassword);
        }
        
        return dataSourceBuilder.build();
    }
}
