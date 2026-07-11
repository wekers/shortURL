## Configuração

Copie o arquivo `.env.example` para `.env`:

```bash
cp .env.example .env
```

Edite o arquivo `.env` caso deseje alterar as credenciais do PostgreSQL.
## Subindo a infraestrutura

```bash
docker compose --env-file .env up -d
```

Verificar containers:

```bash
docker ps
```

## Executando a aplicação

Carregue as variáveis de ambiente:

```bash
source .env
```

Execute a aplicação:

```bash
./mvnw spring-boot:run
```

## Redis Insight

A interface web do Redis Stack estará disponível em:

http://localhost:8001

## Endpoints

### Criar URL encurtada

```bash
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"http://www.example.com/muito/longa/url/de/teste"}'
```

Resposta:

```json
{
  "shortUrl": "http://localhost:8080/fxSL"
}
```

### Redirecionar

```bash
curl -v http://localhost:8080/fxSL
```

Resposta:

```
HTTP/1.1 302 Found
Location: http://www.example.com/muito/longa/url/de/teste
```
