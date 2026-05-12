package org.gitbounty.gitbountybackend;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class FlywayMigrationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void flywaySchemaHistoryTableExists() {
        assertThat(tableExists("flyway_schema_history")).isTrue();
    }

    @Test
    void usersTableExists() {
        assertThat(tableExists("users")).isTrue();
    }

    @Test
    void flywayMigrationsApplied() {
        Integer migrationCount = getMigrationCount();
        assertThat(migrationCount).isGreaterThan(0);
    }

    private boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet tables = metaData.getTables(null, null, tableName, new String[] {"TABLE"})) {
                return tables.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to inspect database metadata", ex);
        }
    }

    private Integer getMigrationCount() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM flyway_schema_history")) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to query migration history", ex);
        }
        return 0;
    }
}


