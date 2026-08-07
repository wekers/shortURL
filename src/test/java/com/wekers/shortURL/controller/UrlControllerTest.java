package com.wekers.shortURL.controller;

import com.wekers.shortURL.dto.ShortenRequest;
import com.wekers.shortURL.service.UrlShortenerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UrlController.class, properties = "app.url.base=http://localhost:8080/")
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UrlShortenerService service;

    private static final String BASE_URL =
            "http://localhost:8080/";

    @Test
    @DisplayName("POST /shorten should return a shortened URL")
    void shortenShouldReturnShortUrl() throws Exception {
        String originalUrl = "https://example.com";
        String shortCode = "abc123";
        when(service.shorten(originalUrl)).thenReturn(shortCode);

        ShortenRequest request =
                new ShortenRequest(originalUrl);

        mockMvc.perform(
                post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value(BASE_URL + shortCode));
    }

    @Test
    @DisplayName("GET /{code} should redirect when URL exists")
    void redirectShouldReturnFound() throws Exception {
        String code = "abc123";
        String targetUrl = "https://target.com";
        when(service.getOriginalUrl(code)).thenReturn(Optional.of(targetUrl));

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, targetUrl));
    }

    @Test
    @DisplayName("GET /{code} should return 404 for an unknown short code")
    void redirectShouldReturnNotFound() throws Exception {
        String code = "unknown";
        when(service.getOriginalUrl(code)).thenReturn(Optional.empty());

        mockMvc.perform(get("/" + code))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /shorten should return 400 for an invalid URL")
    void shortenShouldReturnBadRequest() throws Exception {

        when(service.shorten("invalid"))
                .thenThrow(
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid URL"
                        )
                );

        ShortenRequest request =
                new ShortenRequest("invalid");

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        ))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /shorten should reject an empty body")
    void shortenShouldRejectEmptyBody() throws Exception {

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
