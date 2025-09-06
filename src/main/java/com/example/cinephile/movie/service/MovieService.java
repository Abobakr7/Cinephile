package com.example.cinephile.movie.service;

import com.example.cinephile.common.exception.CinephileException;
import com.example.cinephile.movie.dto.MovieCard;
import com.example.cinephile.movie.dto.MoviePage;
import com.example.cinephile.movie.dto.MovieRequest;
import com.example.cinephile.movie.entity.Movie;
import com.example.cinephile.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MovieService {
    private final MovieRepository movieRepository;

    public Page<MovieCard> getAllMovies(String search, Pageable pageable) {
        Page<Movie> page = (search == null || search.isBlank())
                ? movieRepository.findAll(pageable)
                : movieRepository.findByTitleContainingIgnoreCase(search, pageable);

        return page.map(movie -> new MovieCard(
                movie.getId(), movie.getTitle(), movie.getPosterUrl(),
                movie.getYear(), movie.getRated(), movie.getRating()
        ));
    }

    public MoviePage getMovieById(UUID id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new CinephileException("Movie not found", HttpStatus.NOT_FOUND));
        return new MoviePage(movie.getId(), movie.getTitle(), movie.getPlot(),
                movie.getImdbId(), movie.getPosterUrl(), movie.getRuntime(),
                movie.getYear(), movie.getGenre(), movie.getRated(),
                movie.getRating(), movie.getDirector(), movie.getWriter(),
                movie.getActors(), movie.getLanguage(), movie.getCountry()
        );
    }

    public MovieCard addMovie(MovieRequest request) {
        Movie movie = new Movie();
        movie.setTitle(request.title());
        movie.setPlot(request.plot());
        movie.setImdbId(request.imdbId());
        movie.setPosterUrl(request.posterUrl());
        movie.setRuntime(request.runtime());
        movie.setYear(request.year());
        movie.setGenre(request.genre());
        movie.setRated(request.rated());
        movie.setRating(request.rating());
        movie.setDirector(request.director());
        movie.setWriter(request.writer());
        movie.setActors(request.actors());
        movie.setLanguage(request.language());
        movie.setCountry(request.country());
        movieRepository.save(movie);
        return new MovieCard(movie.getId(), movie.getTitle(), movie.getPosterUrl(),
                movie.getYear(), movie.getRated(), movie.getRating());
    }

    public MovieCard updateMovie(UUID id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new CinephileException("Movie not found", HttpStatus.NOT_FOUND));
        Optional.ofNullable(request.title()).filter(s -> !s.isBlank()).ifPresent(movie::setTitle);
        Optional.ofNullable(request.plot()).filter(s -> !s.isBlank()).ifPresent(movie::setPlot);
        Optional.ofNullable(request.imdbId()).filter(s -> !s.isBlank()).ifPresent(movie::setImdbId);
        Optional.ofNullable(request.posterUrl()).filter(s -> !s.isBlank()).ifPresent(movie::setPosterUrl);
        if (request.runtime() != null && request.runtime() > 0) movie.setRuntime(request.runtime());
        if (request.year() != null && request.year() > 1900) movie.setYear(request.year());
        Optional.ofNullable(request.genre()).filter(s -> !s.isBlank()).ifPresent(movie::setGenre);
        Optional.ofNullable(request.rated()).filter(s -> !s.isBlank()).ifPresent(movie::setRated);
        if (request.rating() != null && request.rating() > 0.0) movie.setRating(request.rating());
        Optional.ofNullable(request.director()).filter(s -> !s.isBlank()).ifPresent(movie::setDirector);
        Optional.ofNullable(request.writer()).filter(s -> !s.isBlank()).ifPresent(movie::setWriter);
        Optional.ofNullable(request.actors()).filter(s -> !s.isBlank()).ifPresent(movie::setActors);
        Optional.ofNullable(request.language()).filter(s -> !s.isBlank()).ifPresent(movie::setLanguage);
        Optional.ofNullable(request.country()).filter(s -> !s.isBlank()).ifPresent(movie::setCountry);
        movieRepository.save(movie);
        return new MovieCard(movie.getId(), movie.getTitle(), movie.getPosterUrl(),
                movie.getYear(), movie.getRated(), movie.getRating());
    }

    public void deleteMovie(UUID id) {
        if (!movieRepository.existsById(id)) {
            throw new CinephileException("Movie not found", HttpStatus.NOT_FOUND);
        }
        movieRepository.deleteById(id);
    }
}
