# Cinephile

Cinephile is a movie ticket reservation system that enables users to book cinema tickts online and a movie catalog. This project is under active development.

## Project Status
- **Initial Version Finished**
- Contributions and feedback are welcome!

## Features
- User authentication and registration
- Movie browsing and search
- Movie management (add/edit/delete)
- Showtimes browsing and search
- Showtimes management (add/edit/delete)
- Cinemas management (add/edit/delete)
- Booking cinema tickets for movies showtimes
- send booking confirmation email with QR code
- Booking management (view/cancel)
- Integration tests
- Swagger API documentation

## Tech Stack
- **Backend:** Java, Spring Boot
- **Database:** MySQL
- **Build Tool:** Maven
- **Testing:** JUnit, Testcontainers

## Project Structure
```
src/
  main/
    java/com/example/cinephile/...
    resources/
  test/
    java/com/example/cinephile/...
    resources/
```

## Getting Started
1. **Clone the repository:**
   ```bash
   git clone https://github.com/Abobakr7/Cinephile.git
   ```
2. **Build the project:**
   ```bash
   ./mvnw clean install
   ```
3. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```
4. **Run tests:**
   ```bash
   ./mvnw test
   ```

## Configuration
- Main configuration: `src/main/resources/application.properties`
- Test configuration: `src/test/resources/application-test.properties`

## Database
- Initial migration script: `src/main/resources/db/migration/V1__init.sql`
- Uses Flyway for database migrations.

## API Documentation
- Swagger UI available at: `http://localhost:8080/swagger-ui.html`
- OpenAPI specification available at: `http://localhost:8080/api-docs`

---

*This README will be updated as the project progresses.*
