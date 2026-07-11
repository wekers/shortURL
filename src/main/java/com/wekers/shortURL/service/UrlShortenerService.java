package com.wekers.shortURL.service;

import com.wekers.shortURL.entity.ShortUrl;
import com.wekers.shortURL.repository.ShortUrlRepository;
import com.wekers.shortURL.util.Base62Encoder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

@Service
public class UrlShortenerService {

    private static final String PREFIX = "shorturl:";

    private static final Duration CACHE_TTL =
            Duration.ofMinutes(3); //to test 3min

    private static final long OFFSET =
            10_000_000L;

    private final ShortUrlRepository repository;

    private final RedisTemplate<String, String> redisTemplate;



    public UrlShortenerService(
            ShortUrlRepository repository,
            RedisTemplate<String, String> redisTemplate) {

        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }


    @Transactional
    public String shorten(String originalUrl) {

        validate(originalUrl);

        return repository.findByOriginalUrl(originalUrl)
                .map(existing -> {
                    return existing.getShortCode();

                })
                .orElseGet(() -> {

                    ShortUrl entity = new ShortUrl();

                    entity.setOriginalUrl(originalUrl);

                    entity = repository.saveAndFlush(entity);


                    String code = Base62Encoder.encode(
                            entity.getId() + OFFSET
                    );

                    entity.setShortCode(code);

                    repository.save(entity);

                    return code;

                });
    }


    public Optional<String> getOriginalUrl(String code) {



        String cacheKey = PREFIX + code;

        String cachedUrl =
                redisTemplate.opsForValue().get(cacheKey);


        if (cachedUrl != null) {

            return Optional.of(cachedUrl);
        }

        return repository.findByShortCode(code)
                .map(entity -> {

                    redisTemplate.opsForValue()
                            .set(
                                    cacheKey,
                                    entity.getOriginalUrl(),
                                    CACHE_TTL
                            );


                    return entity.getOriginalUrl();

                })
                .or(() -> {

                    return Optional.empty();

                });
    }


    private void validate(String url) {

        try {

            URI uri = URI.create(url);


            if (uri.getScheme() == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "URL inválida"
                );
            }


            if (!uri.getScheme().equalsIgnoreCase("http")
                    &&
                    !uri.getScheme().equalsIgnoreCase("https")) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Somente HTTP/HTTPS são permitidos"
                );
            }


        } catch (IllegalArgumentException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "URL inválida"
            );
        }
    }

}