CREATE TABLE `users` (
    `id` VARCHAR(255) PRIMARY KEY,  -- VARCHAR for UUID compatibility
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `name` VARCHAR(150) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `enabled` BOOLEAN DEFAULT False,
    `role` ENUM('USER', 'MANAGER', 'ADMIN') NOT NULL,
    `verification_token` VARCHAR(255),
    `verification_token_expiry` DATETIME,
    `reset_password_token` VARCHAR(255),
    `reset_password_token_expiry` DATETIME,
    `created_at` DATETIME,
    `updated_at` DATETIME
);

CREATE TABLE `cinemas` (
    `id` VARCHAR(255) PRIMARY KEY,
    `name` VARCHAR(150) NOT NULL,
    `address` VARCHAR(255) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `user_id` VARCHAR(255) NOT NULL,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` DATETIME,
    `updated_at` DATETIME,

    CONSTRAINT `cinema_manager_FK` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
);

CREATE TABLE `screens` (
    `id` VARCHAR(255) PRIMARY KEY,
    `name` VARCHAR(150) NOT NULL,
    `cinema_id` VARCHAR(255) NOT NULL,
    `capacity` INT NOT NULL DEFAULT 0,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` DATETIME,
    `updated_at` DATETIME,

    CONSTRAINT `screen_cinema_FK` FOREIGN KEY (`cinema_id`) REFERENCES `cinemas`(`id`),
    UNIQUE KEY `unique_screen_per_cinema` (`cinema_id`, `name`)
);

CREATE TABLE `movies` (
    `id` VARCHAR(255) PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `plot` TEXT NOT NULL,
    `imdb_id` VARCHAR(20) NOT NULL UNIQUE,
    `poster_url` VARCHAR(255) NOT NULL,
    `runtime` INT NOT NULL, -- minutes
    `year` YEAR NOT NULL,
    `genre` VARCHAR(255) NOT NULL,
    `rated` VARCHAR(20),
    `rating` DECIMAL(3, 1), -- allow 10.0 rating
    `director` VARCHAR(255),
    `writer` VARCHAR(255),
    `actors` VARCHAR(255),
    `language` VARCHAR(255),
    `country` VARCHAR(255),
    `created_at` DATETIME,
    `updated_at` DATETIME
);

CREATE TABLE `showtimes` (
    `id` VARCHAR(255) PRIMARY KEY,
    `cinema_id` VARCHAR(255) NOT NULL,
    `screen_id` VARCHAR(255) NOT NULL,
    `movie_id` VARCHAR(255) NOT NULL,
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` DATETIME,
    `updated_at` DATETIME,

    CONSTRAINT `st_cinema_FK` FOREIGN KEY (`cinema_id`) REFERENCES `cinemas`(`id`),
    CONSTRAINT `st_screen_FK` FOREIGN KEY (`screen_id`) REFERENCES `screens`(`id`),
    CONSTRAINT `st_movie_FK` FOREIGN KEY (`movie_id`) REFERENCES `movies`(`id`),
    UNIQUE KEY `unique_showtime` (`screen_id`, `movie_id`, `start_time`)
);

-- Static seats for layout purposes
CREATE TABLE `seats` (
    `id` VARCHAR(255) PRIMARY KEY,
    `screen_id` VARCHAR(255) NOT NULL,
    `seat_number` VARCHAR(10) NOT NULL, -- e.g., "A1", "B12"
    `row_num` VARCHAR(5) NOT NULL, -- e.g., "A", "B"
    `col_num` INT NOT NULL, -- e.g., 1, 2, 3
    `type` ENUM('STANDARD', 'BALCONY', 'PREMIUM', 'WHEELCHAIR') DEFAULT 'STANDARD',
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` DATETIME,
    `updated_at` DATETIME,

    CONSTRAINT `seat_screen_FK` FOREIGN KEY (`screen_id`) REFERENCES `screens`(`id`),
    UNIQUE KEY `unique_seat_per_screen` (`screen_id`, `seat_number`)
);

CREATE TABLE `bookings` (
    `id` VARCHAR(255) PRIMARY KEY,
    `user_id` VARCHAR(255) NOT NULL,
    `showtime_id` VARCHAR(255) NOT NULL,
    `num_seats` INT NULL,
    `total_price` DECIMAL(6, 2) NULL,
    `status` ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED') DEFAULT 'PENDING',
    `expires_at` DATETIME NULL, -- for pending bookings
    `confirmed_at` DATETIME NULL,
    `created_at` DATETIME,
    `updated_at` DATETIME,

    CONSTRAINT `booking_user_FK` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `booking_showtime_FK` FOREIGN KEY (`showtime_id`) REFERENCES `showtimes`(`id`)
);

CREATE TABLE `booking_seats` (
    `id` VARCHAR(255) PRIMARY KEY,
    `seat_id` VARCHAR(255) NOT NULL,
    `showtime_id` VARCHAR(255) NOT NULL,
    `user_id` VARCHAR(255) NULL,
    `booking_id` VARCHAR(255) NULL,
    `status` ENUM('AVAILABLE', 'HELD', 'BOOKED') DEFAULT 'AVAILABLE',
    `price` DECIMAL(5, 2) NOT NULL,
    `held_until` DATETIME NULL,
    `created_at` DATETIME,
    `updated_at` DATETIME,

    CONSTRAINT `bs_seat_FK` FOREIGN KEY (`seat_id`) REFERENCES `seats`(`id`),
    CONSTRAINT `bs_showtime_FK` FOREIGN KEY (`showtime_id`) REFERENCES `showtimes`(`id`),
    CONSTRAINT `bs_user_FK` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `bs_booking_FK` FOREIGN KEY (`booking_id`) REFERENCES `bookings`(`id`),
    UNIQUE KEY `unique_seat_showtime` (`id`, `showtime_id`)
);

CREATE TABLE `movie_lists` (
    `id` VARCHAR(255) PRIMARY KEY,
    `user_id` VARCHAR(255) NOT NULL,
    `movie_id` VARCHAR(255) NOT NULL,
    CONSTRAINT `ml_user_FK` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `ml_movie_FK` FOREIGN KEY (`movie_id`) REFERENCES `movies`(`id`),
    UNIQUE KEY `unique_user_movie` (`id`, `movie_id`)
);

CREATE TABLE `refresh_tokens` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `user_email` VARCHAR(100) NOT NULL,
    `token` VARCHAR(255) NOT NULL,
    `created_at` DATETIME,

    UNIQUE KEY `unique_token` (`token`)
);