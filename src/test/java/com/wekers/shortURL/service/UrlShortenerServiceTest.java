package com.wekers.shortURL.service;

import com.wekers.shortURL.entity.ShortUrl;
import com.wekers.shortURL.repository.ShortUrlRepository;
import com.wekers.shortURL.util.Base62Encoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private ShortUrlRepository repository;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private UrlShortenerService service;

    private static final String PREFIX = "shorturl:";

    @Nested
    @DisplayName("Encurtamento de URL")
    class Shorten {

        @Test
        @DisplayName("Deve criar uma URL nova")
        void shouldCreateNewShortUrl() {
            String originalUrl = "https://example.com";
            long generatedId = 123L;

            when(repository.findByOriginalUrl(originalUrl)).thenReturn(Optional.empty());

            // Simula a geração de ID ao salvar
            when(repository.saveAndFlush(any(ShortUrl.class)))
                    .thenAnswer(invocation -> {
                        ShortUrl entity = invocation.getArgument(0);
                        entity.setId(generatedId); // ID mockado
                        return entity;
                    });

            String code = service.shorten(originalUrl);
            String expectedCode = Base62Encoder.encode(generatedId + 10_000_000L);

            assertThat(code).isEqualTo(expectedCode);
            verify(repository).saveAndFlush(any(ShortUrl.class));

            ArgumentCaptor<ShortUrl> captor =
                    ArgumentCaptor.forClass(ShortUrl.class);

            verify(repository).save(captor.capture());

            ShortUrl saved = captor.getValue();

            assertThat(saved.getShortCode())
                    .isEqualTo(expectedCode);

            assertThat(saved.getOriginalUrl())
                    .isEqualTo(originalUrl);
        }

        @Test
        @DisplayName("Não deve criar URL duplicada")
        void shouldReuseExistingCode() {
            String originalUrl = "https://example.com";
            String existingCode = "abc123";
            ShortUrl existingEntity = new ShortUrl();
            existingEntity.setId(100L);
            existingEntity.setOriginalUrl(originalUrl);
            existingEntity.setShortCode(existingCode);

            when(repository.findByOriginalUrl(originalUrl)).thenReturn(Optional.of(existingEntity));

            String code = service.shorten(originalUrl);
            assertThat(code).isEqualTo(existingCode);
            verify(repository, never()).saveAndFlush(any(ShortUrl.class));

            verify(repository, never()).save(any());
            verify(repository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Resolução de URL original")
    class GetOriginalUrl {

        @Test
        @DisplayName("Deve retornar do Redis (cache hit)")
        void shouldReturnFromRedisWhenCached() {
            String code = "abc123";
            String cachedUrl = "https://cached.com";
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(PREFIX + code)).thenReturn(cachedUrl);

            Optional<String> result = service.getOriginalUrl(code);

            assertThat(result).contains(cachedUrl);
            verify(valueOps).get(PREFIX + code);
            verify(repository, never()).findByShortCode(anyString());
        }

        @Test
        @DisplayName("Deve consultar banco quando Redis falhar (cache miss)")
        void shouldLoadUrlFromDatabaseWhenCacheMiss() {
            String code = "abc123";
            String originalUrl = "https://fromdb.com";
            ShortUrl entity = new ShortUrl();
            entity.setShortCode(code);
            entity.setOriginalUrl(originalUrl);

            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(PREFIX + code)).thenReturn(null);
            when(repository.findByShortCode(code)).thenReturn(Optional.of(entity));

            Optional<String> result = service.getOriginalUrl(code);

            assertThat(result).contains(originalUrl);
            verify(valueOps).set(eq(PREFIX + code), eq(originalUrl), any(Duration.class));
            verify(repository).findByShortCode(code);
        }

        @Test
        @DisplayName("Código inexistente (Redis e banco vazios)")
        void shouldReturnEmptyWhenCodeNotFound() {
            String code = "nonexistent";
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(PREFIX + code)).thenReturn(null);
            when(repository.findByShortCode(code)).thenReturn(Optional.empty());

            Optional<String> result = service.getOriginalUrl(code);
            assertThat(result).isEmpty();
            verify(repository).findByShortCode(code);

        }
    }

    @Nested
    @DisplayName("Validação")
    class Validation {

        @Test
        @DisplayName("Deve lançar exceção para URL nula")
        void shouldThrowForNullUrl() {
            assertThatThrownBy(()           -> service.shorten(null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Deve lançar exceção para URL vazia")
        void shouldThrowForEmptyUrl() {
            assertThatThrownBy(() -> service.shorten(""))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Deve lançar exceção para URL inválida")
        void shouldThrowForInvalidUrl() {
            assertThatThrownBy(() -> service.shorten("invalid-url"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Deve lançar exceção para URL em branco")
        void shouldThrowForBlankUrl() {

            assertThatThrownBy(() -> service.shorten("   "))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
        }
    }




}

