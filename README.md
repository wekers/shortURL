## Subindo a infraestrutura

```bash
docker run -d --name redis-stack -p 6379:6379 -p 8001:8001 redis/redis-stack:latest
```

Verificar container:

```bash
docker ps
```

Executar a aplicação:

```bash
./mvnw spring-boot:run
```

Endpoints:

Criar URL encurtada:
POST /shorten
```bash
curl -X POST http://localhost:8080/shorten \
-H "Content-Type: application/json" \
-d '{"url": "http://www.example.com/muito/longa/url/de/teste"}'
```

Acessar URL encurtada:
GET /{shortUrl}
```bash
curl -v http://localhost:8080/{shortUrl}
```