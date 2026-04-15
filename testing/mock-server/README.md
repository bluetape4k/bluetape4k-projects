# Module bluetape4k-mock-server

English | [한국어](./README.ko.md)

A self-contained Spring Boot HTTP mock server that replaces external HTTP dependencies in integration tests.
It simulates **httpbin.org**, **jsonplaceholder.typicode.com**, and a simple web-content endpoint, all in one Docker image (`bluetape4k/mock-server`).

## Overview

| Replaces | Prefix |
|----------|--------|
| [httpbin.org](https://httpbin.org) — HTTP inspection API | `/httpbin/**` |
| [jsonplaceholder.typicode.com](https://jsonplaceholder.typicode.com) — REST fixture API | `/jsonplaceholder/**` |
| Generic HTML / web content fixtures | `/web/**` |
| Health check | `/ping` |
| Admin / data reset | `/admin/reset` |

## Endpoints

### Core

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/ping` | Health check — returns `pong` |
| `POST` | `/admin/reset` | Reloads all in-memory fixture data from classpath JSON files |

### `/httpbin/**`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/httpbin/get` | Echoes GET request info |
| `POST` | `/httpbin/post` | Echoes POST request + body |
| `PUT` | `/httpbin/put` | Echoes PUT request + body |
| `PATCH` | `/httpbin/patch` | Echoes PATCH request + body |
| `DELETE` | `/httpbin/delete` | Echoes DELETE request info |
| `GET` | `/httpbin/headers` | Returns all request headers |
| `GET` | `/httpbin/ip` | Returns client IP |
| `GET` | `/httpbin/user-agent` | Returns User-Agent header |
| `GET` | `/httpbin/uuid` | Returns a random UUID |
| `ANY` | `/httpbin/anything/**` | Echoes any request |
| `ANY` | `/httpbin/status/{code}` | Returns the given HTTP status code |
| `GET` | `/httpbin/bytes/{n}` | Returns `n` random bytes |
| `GET` | `/httpbin/delay/{seconds}` | Responds after a delay |
| `GET` | `/httpbin/stream/{n}` | Streams `n` JSON lines |
| `GET` | `/httpbin/image/{format}` | Returns a sample image (png/jpeg/svg/webp) |

### `/jsonplaceholder/**`

Mirrors [jsonplaceholder.typicode.com](https://jsonplaceholder.typicode.com). All resources support full CRUD.

| Resource | Base Path |
|----------|-----------|
| Posts | `/jsonplaceholder/posts` |
| Comments | `/jsonplaceholder/comments` |
| Albums | `/jsonplaceholder/albums` |
| Photos | `/jsonplaceholder/photos` |
| Todos | `/jsonplaceholder/todos` |
| Users | `/jsonplaceholder/users` |

### `/web/**`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/web/{name}` | Returns cached HTML content by name |

## Architecture

### Class Diagram

```mermaid
classDiagram
    class MockServerApplication {
        +main(args)
    }

    class HttpbinController {
        +get(request) HttpbinResponse
        +post(request, body) HttpbinResponse
        +headers(request) Map
        +ip(request) Map
        +status(code) ResponseEntity
        +bytes(n) ResponseEntity
    }

    class HttpbinAdvancedController {
        +delay(seconds) HttpbinResponse
        +image(format) ResponseEntity
    }

    class HttpbinStreamController {
        +stream(n) StreamingResponseBody
    }

    class PostsController {
        +list() List~PostRecord~
        +get(id) PostRecord
        +create(post) PostRecord
        +update(id, post) PostRecord
        +delete(id) ResponseEntity
    }

    class JsonplaceholderService {
        +reloadFromFixtures()
    }

    class InMemoryRepository~T~ {
        <<generic>>
        +findAll() List~T~
        +findById(id) T?
        +save(entity) T
        +deleteById(id)
    }

    class WebContentController {
        +getContent(name) ResponseEntity
    }

    class WebContentLoader {
        +load(name) String
    }

    class AdminController {
        +reset() ResponseEntity
    }

    MockServerApplication --> HttpbinController
    MockServerApplication --> HttpbinAdvancedController
    MockServerApplication --> HttpbinStreamController
    MockServerApplication --> PostsController
    MockServerApplication --> WebContentController
    MockServerApplication --> AdminController

    PostsController --> JsonplaceholderService
    AdminController --> JsonplaceholderService
    JsonplaceholderService --> InMemoryRepository

    WebContentController --> WebContentLoader

    style MockServerApplication fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style HttpbinController fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style HttpbinAdvancedController fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style HttpbinStreamController fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style PostsController fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style JsonplaceholderService fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    style InMemoryRepository fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    style WebContentController fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style WebContentLoader fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style AdminController fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
```

### Request Routing Flowchart

```mermaid
flowchart TD
    REQ["Incoming HTTP Request"]

    REQ --> ROUTE{Path prefix?}

    ROUTE -->|/httpbin/**| HB["HttpbinController\nHttpbinAdvancedController\nHttpbinStreamController"]
    ROUTE -->|/jsonplaceholder/**| JP["Posts / Comments / Albums\nPhotos / Todos / Users\nControllers"]
    ROUTE -->|/web/**| WEB["WebContentController\n(Caffeine cached HTML)"]
    ROUTE -->|/ping| PING["200 pong"]
    ROUTE -->|/admin/reset| ADMIN["AdminController\nreload fixtures"]

    HB --> RESP["HTTP Response"]
    JP --> RESP
    WEB --> RESP
    PING --> RESP
    ADMIN --> RESP

    classDef routeStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef handlerStyle fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    classDef respStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32

    class REQ,ROUTE routeStyle
    class HB,JP,WEB,PING,ADMIN handlerStyle
    class RESP respStyle
```

### Sequence Diagram — httpbin GET

```mermaid
sequenceDiagram
    participant CLIENT as Test / Client
    participant SERVER as BluetapeHttpServer (Docker)
    participant CTRL as HttpbinController

    CLIENT->>SERVER: GET http://host:8888/httpbin/get
    SERVER->>CTRL: dispatch /httpbin/get
    CTRL->>CTRL: build HttpbinResponse\n(url, method, headers, origin)
    CTRL-->>SERVER: HttpbinResponse JSON
    SERVER-->>CLIENT: 200 OK + JSON body
```

## Configuration

`src/main/resources/application.yml` defaults:

| Key | Value | Notes |
|-----|-------|-------|
| `server.port` | `8888` | Fixed container port |
| `spring.threads.virtual.enabled` | `true` | Virtual Threads (JDK 21+) |
| `spring.cache.type` | `caffeine` | In-process caching |
| `spring.cache.cache-names` | `html-content`, `fixture-data`, `httpbin-image` | Caffeine cache names |
| `server.http2.enabled` | `true` | HTTP/2 support |

## Build & Run

### Build Docker image with Jib

```bash
./gradlew :bluetape4k-mock-server:jibBuildTar
```

This produces `build/jib-image.tar`. Load it into Docker:

```bash
docker load < testing/mock-server/build/jib-image.tar
```

### Run directly

```bash
docker run --rm -p 8888:8888 bluetape4k/mock-server:latest
```

### Use via Testcontainers (`BluetapeHttpServer`)

```kotlin
val server = BluetapeHttpServer.Launcher.bluetapeHttpServer

// Pre-built URL helpers
println(server.url)                // http://localhost:<dynamic-port>
println(server.httpbinUrl)         // http://localhost:<port>/httpbin
println(server.jsonplaceholderUrl) // http://localhost:<port>/jsonplaceholder
println(server.webUrl)             // http://localhost:<port>/web
```

## Adding the Dependency

The mock-server module is a Docker image, not a library dependency.
To run it in tests, add the testcontainers module:

```kotlin
dependencies {
    testImplementation("io.github.bluetape4k:bluetape4k-testcontainers:${version}")
}
```

## References

- [httpbin.org](https://httpbin.org)
- [jsonplaceholder.typicode.com](https://jsonplaceholder.typicode.com)
- [Testcontainers](https://www.testcontainers.org/)
- [Jib — Containerize Java apps](https://github.com/GoogleContainerTools/jib)
