package com.wekers.shortURL.integration;

import com.wekers.shortURL.dto.ShortenRequest;
import com.wekers.shortURL.dto.ShortenResponse;
import com.wekers.shortURL.entity.ShortUrl;
import com.wekers.shortURL.repository.ShortUrlRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class UrlShortenerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("shorturl")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis/redis-stack:latest")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port",
                () -> redis.getMappedPort(6379));
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ShortUrlRepository repository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    @DisplayName("Deve executar o fluxo completo do encurtador")
    void shouldExecuteCompleteShortenerFlow() {

        String originalUrl = "https://integration.test";

        //-------------------------------------------------------
        // POST /shorten
        //-------------------------------------------------------

        ShortenResponse response =
                webTestClient.post()
                        .uri("/shorten")
                        .bodyValue(new ShortenRequest(originalUrl))
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(ShortenResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(response).isNotNull();

        String shortUrl = response.shortUrl();

        assertThat(shortUrl)
                .startsWith("http://localhost:");

        String code =
                shortUrl.substring(shortUrl.lastIndexOf('/') + 1);

        //-------------------------------------------------------
        // Banco
        //-------------------------------------------------------

        assertThat(repository.count())
                .isEqualTo(1);

        Optional<ShortUrl> entity =
                repository.findByShortCode(code);

        assertThat(entity).isPresent();

        assertThat(entity.get().getOriginalUrl())
                .isEqualTo(originalUrl);

        //-------------------------------------------------------
        // GET /{code}
        //-------------------------------------------------------

        webTestClient.get()
                .uri("/" + code)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FOUND)
                .expectHeader()
                .valueEquals(HttpHeaders.LOCATION, originalUrl);

        //-------------------------------------------------------
        // Redis
        //-------------------------------------------------------

        String cached =
                redisTemplate.opsForValue()
                        .get("shorturl:" + code);

        assertThat(cached)
                .isEqualTo(originalUrl);

        //-------------------------------------------------------
        // Mesmo POST novamente (idempotência)
        //-------------------------------------------------------

        ShortenResponse duplicated =
                webTestClient.post()
                        .uri("/shorten")
                        .bodyValue(new ShortenRequest(originalUrl))
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(ShortenResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(duplicated).isNotNull();

        assertThat(duplicated.shortUrl())
                .isEqualTo(shortUrl);

        assertThat(repository.count())
                .isEqualTo(1);
    }
}