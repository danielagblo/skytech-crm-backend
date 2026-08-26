package com.skytech.crm.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username:}")
    private String usernameProperty;

    @Value("${spring.datasource.password:}")
    private String passwordProperty;

    @Value("${spring.datasource.hikari.maximum-pool-size:5}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:1}")
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Bean
    @Primary
    public DataSource dataSource() throws URISyntaxException {
        HikariConfig config = new HikariConfig();

        if (databaseUrl != null && (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            // Re-map schema name postgresql to postgres so Java's URI class can parse it correctly
            String uriString = databaseUrl.replace("postgresql://", "postgres://");
            URI dbUri = new URI(uriString);

            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String portPart = dbUri.getPort() == -1 ? "" : ":" + dbUri.getPort();
            String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + portPart + dbUri.getPath();
            
            if (dbUri.getQuery() != null) {
                dbUrl += "?" + dbUri.getQuery();
            }

            config.setJdbcUrl(dbUrl);
            config.setUsername(username);
            config.setPassword(password);
        } else if (databaseUrl != null && databaseUrl.startsWith("jdbc:postgresql://") && databaseUrl.contains("@")) {
            // If the user kept the jdbc: prefix but also kept the username/password in URL:
            // e.g. jdbc:postgresql://username:password@host:port/database
            String cleanUrl = databaseUrl.substring("jdbc:".length());
            URI dbUri = new URI(cleanUrl);

            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String portPart = dbUri.getPort() == -1 ? "" : ":" + dbUri.getPort();
            String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + portPart + dbUri.getPath();

            if (dbUri.getQuery() != null) {
                dbUrl += "?" + dbUri.getQuery();
            }

            config.setJdbcUrl(dbUrl);
            config.setUsername(username);
            config.setPassword(password);
        } else {
            // Standard fallback when credentials are provided separately via properties/env
            config.setJdbcUrl(databaseUrl);
            config.setUsername(usernameProperty);
            config.setPassword(passwordProperty);
        }

        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);

        if (config.getJdbcUrl() != null && config.getJdbcUrl().startsWith("jdbc:postgresql://")) {
            config.setDriverClassName("org.postgresql.Driver");
        }

        return new HikariDataSource(config);
    }
}
