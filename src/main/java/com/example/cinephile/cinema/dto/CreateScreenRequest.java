package com.example.cinephile.cinema.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateScreenRequest(@NotBlank(message = "Name is required") String name,
                                  @NotNull(message = "Capacity is required") @Min(0) Integer capacity,
                                  @NotNull(message = "Number of rows is required") @Min(0) @Max(26) Integer numRows,
                                  @NotNull(message = "Number of columns is required") @Min(0) @Max(40) Integer numCols) {
}
