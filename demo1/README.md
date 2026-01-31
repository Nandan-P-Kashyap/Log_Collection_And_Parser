# LogParser (Spring Boot migration)

This project was migrated to a Spring Boot application.

## Build and run

- Build: `mvn clean package`
- Run during development: `mvn spring-boot:run`
- Run packaged jar: `java -jar target/log-parser-0.0.1-SNAPSHOT.jar`

## Notes

- `com.logproc.Application` is the Spring Boot entry point.
- The `src/main/resources/application.properties` file contains application configuration.

## REST API Endpoints

- POST `/api/logs/parse` — body: `{ "line": "<raw log line>" }` returns parsed `LogEntry` JSON.
- POST `/api/logs/process` — body: `{ "path": "<absolute-or-relative-file-path>" }` returns a summary with `count` and parsed `entries`.

Note: Endpoints are under `/api/logs` and return JSON.