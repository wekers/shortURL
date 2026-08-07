# 🔗 Short URL

[![Build](https://github.com/wekers/shortURL/actions/workflows/build.yml/badge.svg)](https://github.com/wekers/shortURL/actions/workflows/build.yml)
[![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-Stack-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A lightweight URL shortening service built with Spring Boot 4.1.

This project demonstrates the implementation of a RESTful API using:

- Base62 encoding
- Cache Aside pattern with Redis
- PostgreSQL persistence
- Docker Compose
- Unit, Controller and Integration Tests with Testcontainers




```text
                ┌─────────────┐
                │   Client    │
                └──────┬──────┘
                       │
                POST /shorten
                GET /{code}
                       │
              ┌────────▼────────┐
              │ Spring Boot API │
              └────────┬────────┘
                       │
             ┌─────────┴─────────┐
             ▼                   ▼
        PostgreSQL             Redis
     Persistent Store          Cache
```
---
## Example

The following example shows the complete request flow:

1. Create a shortened URL
2. Access the generated short URL
3. Receive an HTTP 302 redirect
4. Observe the Cache-Aside flow (Redis → PostgreSQL → Redis)

![endpoints](assets/AppLogEndPoint.png)

---
## Architecture

### Component Diagram - URL Shortening Flow
```mermaid
flowchart LR
    Client[Client]
    subgraph App[Spring Boot Application]
        Controller[UrlController]
        Service[UrlShortenerService]
        Repository[ShortUrlRepository]
        Encoder[Base62Encoder]
    end
    DB[(PostgreSQL)]
    Redis[(Redis)]

    Client -->|POST /shorten| Controller
    Controller -->|call| Service
    Service -->|read / write| Repository
    Repository -->|SQL| DB
    Service -->|Generate Base62 code| Encoder
    Service -.->|not used| Redis
```
### URL Shortening Sequence:
```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant PostgreSQL

    Client->>Controller: POST /shorten
    Controller->>Service: shorten(url)

    Service->>PostgreSQL: URL already exists?

    alt Yes
        PostgreSQL-->>Service: existing code
    else No
        Service->>Service: generate Base62 code
        Service->>PostgreSQL: persist URL
    end

    Service-->>Controller: shortUrl
    Controller-->>Client: 200 OK
```

### URL Redirect Flow (Cache Aside)
```mermaid
sequenceDiagram
    participant Client
    participant Service
    participant Redis
    participant PostgreSQL

    Client->>Service: GET /{code}
    Service->>Redis: query

    alt Cache HIT
        Redis-->>Service: URL
    else Cache MISS
        Service->>PostgreSQL: query
        PostgreSQL-->>Service: URL
        Service->>Redis: update cache
    end

    Service-->>Client: 302 Redirect
```

---

## Technologies
- Java 21
- Spring Boot 4.1
- Spring MVC
- Spring Data JPA
- Spring Data Redis
- PostgreSQL
- Redis
- Docker Compose
- JUnit 5
- Mockito
- MockMvc
- WebTestClient
- Testcontainers
---
## Project Structure:

```text
src
 main
 ├── config
 ├── controller
 ├── dto
 ├── entity
 ├── repository
 ├── service
 └── util
 test
 ├── controller
 ├── integration
 └── service
```
--- 
## Getting Started:

```bash
# Clone the Repository
git clone https://github.com/wekers/shortURL.git
cd shortURL/
```

Copy the example environment file:

```bash
cp .env.example .env
```

Edit the `.env` file if you wish to change the PostgreSQL credentials.
## Starting the infrastructure

```bash
docker compose --env-file .env up -d
```

Check containers:

```bash
docker ps
```

## Running the application

![run](assets/AppRun.png)

Load the environment variables:

* For Bash/Zsh:

```bash
export $(grep -v '^#' .env | xargs)
```

* For Fish shell (use a helper file):
```bash
source load-env.fish
```

Run the application:

```bash
./mvnw spring-boot:run
```

## Redis Insight

**Redis Stack Web UI:**

http://localhost:8001

## Endpoints
![endpoints](assets/AppLogEndPoint.png)

### Shorten URL

```bash
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com/products/electronics/notebooks/gaming/high-performance-model-2026"}'
```

Response:

```json
{
  "shortUrl": "http://localhost:8080/fxSM"
}
```

### Redirect to Original URL

```bash
curl -v http://localhost:8080/fxSM
```

Response:

```
HTTP/1.1 302 Found
Location: https://example.com/products/electronics/notebooks/gaming/high-performance-model-2026
```


---
### Testing

```bash
./mvn test
```


| Type        | Frameworks                                          |
| ----------  | --------------------------------------------------- |
| Unit        | JUnit + Mockito                                     |
| Controller  | MockMvc                                             |
| Integration | WebTestClient + Testcontainers (PostgreSQL + Redis) |

![tests](assets/All-tests-shortURL.png)
---

### Roadmap


- [x] URL Shortening
- [x] PostgreSQL
- [x] Redis
- [x] Docker Compose
- [x] Unit Tests
- [x] Controller Tests
- [x] Integration Tests
- [x] Testcontainers
- [x] GitHub Actions
- [x] Build Badge

## Future Improvements

- [ ] API documentation (OpenAPI / Swagger)
- [ ] Metrics with Micrometer + Prometheus
- [ ] Docker image publishing
- [ ] Deployment to a cloud provider

## License

MIT
