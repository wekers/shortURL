package com.wekers.shortURL.controller;

import com.wekers.shortURL.dto.ShortenRequest;
import com.wekers.shortURL.dto.ShortenResponse;
import com.wekers.shortURL.service.UrlShortenerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
public class UrlController {

    private final UrlShortenerService service;

    @Value("${app.url.base}")
    private String baseUrl;

    public UrlController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {

        String code = service.shorten(request.url());
        return ResponseEntity.ok(new ShortenResponse(baseUrl + code));

    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {

        Optional<String> url = service.getOriginalUrl(code);

        return url.<ResponseEntity<Void>>map(s -> ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(s))
                .build()).orElseGet(() -> ResponseEntity.notFound().build());

    }
}
