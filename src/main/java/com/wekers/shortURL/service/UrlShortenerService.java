package com.wekers.shortURL.service;

import com.wekers.shortURL.entity.ShortUrl;
import com.wekers.shortURL.repository.ShortUrlRepository;
import com.wekers.shortURL.util.Base62Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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

    private static final Logger log =
            LoggerFactory.getLogger(UrlShortenerService.class);


    public UrlShortenerService(
            ShortUrlRepository repository,
            RedisTemplate<String, String> redisTemplate) {

        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }


    @Transactional
    public String shorten(String originalUrl) {

        validate(originalUrl);

        log.info("Received URL shortening request: {}", originalUrl);

        return repository.findByOriginalUrl(originalUrl)
                .map(existing -> {

                    log.info(
                            "URL already exists. Reusing short code '{}'",
                            existing.getShortCode()
                    );

                    return existing.getShortCode();

                })
                .orElseGet(() -> {

                    log.info(
                            "Original URL '{}' not found. Generating a new short code.",
                            originalUrl
                    );

                    ShortUrl entity = new ShortUrl();

                    entity.setOriginalUrl(originalUrl);

                    entity = repository.saveAndFlush(entity);


                    String code = Base62Encoder.encode(
                            entity.getId() + OFFSET
                    );

                    entity.setShortCode(code);

                    repository.save(entity);


                    log.info(
                            "Generated short code '{}' for URL '{}'",
                            code,
                            originalUrl
                    );

                    return code;

                });
    }


    public Optional<String> getOriginalUrl(String code) {

        log.info(
                "[{}] Retrieving original URL for short code.",
                code
        );


        String cacheKey = PREFIX + code;

        String cachedUrl =
                redisTemplate.opsForValue().get(cacheKey);


        if (cachedUrl != null) {

            log.info(
                    "[{}] Cache HIT. Found in Redis. Redirecting to '{}'",
                    code,
                    cachedUrl
            );


            return Optional.of(cachedUrl);
        }


        log.info(
                "[{}] Cache MISS. Loading from database.",
                code
        );


        return repository.findByShortCode(code)
                .map(entity -> {

                    redisTemplate.opsForValue()
                            .set(
                                    cacheKey,
                                    entity.getOriginalUrl(),
                                    CACHE_TTL
                            );


                    log.info(
                            "[{}] Loaded from database and cached in Redis.",
                            code
                    );


                    return entity.getOriginalUrl();

                })
                .or(() -> {

                    log.warn(
                            "[{}] Short code not found.",
                            code
                    );

                    return Optional.empty();

                });
    }


    private void validate(String url) {

        if (!StringUtils.hasText(url)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "URL is required"
            );
        }

        try {

            URI uri = URI.create(url);

            if (uri.getScheme() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "URL must start with http:// or https://"
                );
            }

            if (!uri.getScheme().equalsIgnoreCase("http")
                    && !uri.getScheme().equalsIgnoreCase("https")) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Only HTTP and HTTPS URLs are supported"
                );
            }

        } catch (IllegalArgumentException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid URL"
            );
        }
    }

}