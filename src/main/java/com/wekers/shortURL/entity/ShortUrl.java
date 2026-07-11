package com.wekers.shortURL.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;


@Entity
@Table(
        indexes = {
                @Index(name = "idx_short_code", columnList = "shortCode"),
                @Index(name = "idx_original_url", columnList = "originalUrl")
        }
)
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(unique = true, length = 10)
    private String shortCode;

    @Column(nullable = false, length = 2048, unique = true)
    private String originalUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();


    // Getters/Setters

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {

        this.shortCode = shortCode;
    }

    public String getOriginalUrl() {

        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {

        this.originalUrl = originalUrl;
    }

    public LocalDateTime getCreatedAt() {

        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {

        this.createdAt = createdAt;
    }
}




