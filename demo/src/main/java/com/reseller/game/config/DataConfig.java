package com.reseller.game.config;

import net.sf.log4jdbc.sql.jdbcapi.DataSourceSpy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataConfig {

    @Autowired
    private DataSourceProperties dataSourceProperties;

    @Bean(name = "realDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource realDataSource() {
        return dataSourceProperties
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        // Wrap the real data source with log4jdbc's DataSourceSpy
        return new DataSourceSpy(realDataSource());
    }
}
