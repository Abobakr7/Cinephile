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
   
## Booking Flow
1. User must be logged in.
2. Browse available showtimes: ``GET /api/showtimes/now?title=&genre=&rated=&page=&size&``
3. Select a showtime: ``GET /api/showtimes/{showtimeId}``
4. Choose cinema from cinemas hosting the showtime: ``GET /api/showtimes/movie/{movieId}/cinemas``
5. Choose date from available dates: ``GET /api/showtimes/movie/{movieId}/cinema/{cinemaId}/dates``
6. Choose time from available times: ``GET /api/showtimes/movie/{movieId}/cinema/{cinemaId}/date/{date}/times``
7. Choose screen from available screens: ``GET /api/showtimes/movie/{movieId}/cinema/{cinemaId}/dates/{date}/times/{time}/screens``
8. Layout of the screen with available seats: ``GET /api/showtimes/movie/{movieId}/cinema/{cinemaId}/dates/{date}/times/{time}/screens/{screenId}/layout``
9. Create booking **(start point)**: ``POST /api/bookings/{showtimesId}``
10. Now you can choose your seats:
    - Choose one seat at a time: ``POST /api/bookings/{bookingId}/lock-seat``
    - To deselect a seat: ``POST /api/bookings/{bookingId}/release-seat``
      ```
      Request Body for both endpoints:
      {
         "seatId": UUID,
         "showtimeId": UUID
      }
      ```
11. When you finish choosing your seats, confirm your booking: ``POST /api/bookings/{bookingId}/confirm``
12. You will receive a booking confirmation email with QR code.
13. To cancel your booking: ``POST /api/bookings/{bookingId}/cancel``

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
