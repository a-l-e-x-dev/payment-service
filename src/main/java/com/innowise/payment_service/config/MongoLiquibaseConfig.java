package com.innowise.payment_service.config;

import jakarta.annotation.PostConstruct;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoLiquibaseConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @PostConstruct
    public void runLiquibase() throws Exception {
        System.setProperty("liquibase.secureParsing", "false");
        Database database = DatabaseFactory.getInstance()
                .openDatabase(mongoUri, null, null, null, new ClassLoaderResourceAccessor());

        try (Liquibase liquibase = new Liquibase("db/changelog/db.changelog-master.json",
                new ClassLoaderResourceAccessor(),
                database)) {

            liquibase.update(new Contexts(), new LabelExpression());
        } finally {
            if (database != null) {
                database.close();
            }
        }
    }
}