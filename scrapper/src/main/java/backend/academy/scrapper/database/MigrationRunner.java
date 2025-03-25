package backend.academy.scrapper.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MigrationRunner {

    public void runMigrations(
            String url, String username, String password, String migrationsDir, String changelogFile) {
        try {
            // Устанавливаем соединение с базой данных
            Connection connection = DriverManager.getConnection(url, username, password);
            Database database =
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));

            // Указываем путь к миграциям (каталог migrations/ в корне проекта)
            File migrationsPath = new File(migrationsDir);
            if (!migrationsPath.exists()) {
                throw new IllegalArgumentException("Migrations directory does not exist: " + migrationsDir);
            }

            // Создаём ResourceAccessor для чтения файлов из файловой системы
            FileSystemResourceAccessor resourceAccessor = new FileSystemResourceAccessor(migrationsPath);

            // Создаём объект Liquibase
            Liquibase liquibase = new Liquibase(changelogFile, resourceAccessor, database);

            // Выполняем миграции
            log.info("Starting database migrations...");
            liquibase.update(new Contexts(), new LabelExpression());
            log.info("Database migrations completed successfully.");

            // Закрываем соединение
            connection.close();
        } catch (Exception e) {
            log.error("Failed to run database migrations", e);
            throw new RuntimeException("Migration failed", e);
        }
    }
}
