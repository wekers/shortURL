package com.wekers.shortURL.dto;

import jakarta.validation.constraints.NotBlank;

public record ShortenRequest(@NotBlank String url) {
}
