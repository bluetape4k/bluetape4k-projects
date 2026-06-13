# bluetape4k-mock-webflux-server

한국어 | [English](./README.md)

통합 테스트용 독립 실행형 Spring Boot 4 + WebFlux Mock 서버입니다.
[httpbin.org](https://httpbin.org), [jsonplaceholder.typicode.com](https://jsonplaceholder.typicode.com)와 호환되는 HTTP 엔드포인트를 Kotlin Coroutines(
`suspend fun`, `Flow`)로 구현합니다. Docker 컨테이너 내부에서 **80**(HTTP) / **443**(HTTPS) 포트로 실행됩니다.

## Architecture

`bluetape4k-mock-webflux-server`는 독립 실행형 Spring Boot 애플리케이션으로 패키징됩니다(모듈명: `bluetape4k-mock-webflux-server`, Docker 이미지:
`bluetape4k/mock-webflux-server`). 외부 네트워크 없이 실제 HTTP 서버가 필요한 통합 테스트 스위트에서 `BluetapeWebfluxServer`(Testcontainers)를 통해
`GenericContainer`로 실행하도록 설계되었습니다.

### `bluetape4k-mock-web-server`와 비교

| 항목       | `mock-web-server` (MVC) | `mock-webflux-server` (WebFlux) |
|----------|-------------------------|---------------------------------|
| 스택       | Spring MVC (Servlet)    | Spring WebFlux (Reactive)       |
| 핸들러      | 일반 `fun`                | `suspend fun` / `Flow`          |
| 스트리밍     | 없음                      | `Flow` 기반 SSE / 청크              |
| 코루틴      | 선택적                     | 핵심                              |
| I/O 모델   | 요청당 스레드                 | 이벤트 루프 (Netty)                  |
| HTTP 포트  | 80                      | 80                              |
| HTTPS 포트 | 443                     | 443                             |

## UML

### 요청 라우팅 개요

![mock webflux server Architecture diagram](../../docs/images/readme-diagrams/testing-mock-webflux-server-diagram-01.png)

### 클래스 다이어그램

![Mock WebFlux Server Class Structure diagram](../../docs/images/readme-diagrams/testing-mock-webflux-server-diagram-02.png)

### 시퀀스 다이어그램 — httpbin GET 요청

![— httpbin GET diagram](../../docs/images/readme-diagrams/testing-mock-webflux-server-sequence-01.png)

## Features

- **Spring Boot 4 + Kotlin Coroutines**: 모든 핸들러가 `suspend fun` 또는 `Flow` 반환 — 완전 논블로킹
- **포트 80(HTTP) / 443(HTTPS)**: 결정론적 테스트 구성을 위한 표준 컨테이너 포트
- **httpbin 시뮬레이션**: `/httpbin/**`에서 HTTP 요청 검사 전체 API 제공
- **jsonplaceholder 시뮬레이션**: `/jsonplaceholder/**`에서 6개 전체 CRUD 리소스 (posts/comments/albums/photos/todos/users)
- **웹 컨텐츠 Fixture**: Caffeine 캐시를 통한 `/web/{name}` HTML 컨텐츠
- **관리 초기화**: `POST /admin/reset`으로 클래스패스 JSON 파일에서 전체 인메모리 Fixture 데이터 재적재
- **Docker 이미지**: `bluetape4k/mock-webflux-server` — Jib으로 빌드
- **Testcontainers 통합**: `BluetapeWebfluxServer`가 HTTP/HTTPS URL 헬퍼 제공

### 설정

`src/main/resources/application.yml` 기본값:

| 키                          | 값                                              | 설명             |
|----------------------------|------------------------------------------------|----------------|
| `server.port`              | `80`                                           | HTTP 컨테이너 포트   |
| `bluetape4k.https.port`    | `443`                                          | HTTPS 컨테이너 포트  |
| `spring.cache.type`        | `caffeine`                                     | 인프로세스 캐시       |
| `spring.cache.cache-names` | `web-content`, `fixture-data`, `httpbin-image` | Caffeine 캐시 이름 |

## Examples

### Docker로 실행

```bash
docker run --rm -p 80:80 -p 443:443 bluetape4k/mock-webflux-server:latest
```

### Jib으로 Docker 이미지 빌드

```bash
./gradlew :bluetape4k-mock-webflux-server:jibDockerBuild --no-configuration-cache
```

### Testcontainers로 사용 (`BluetapeWebfluxServer`)

```kotlin
val server = BluetapeWebfluxServer().apply { start() }

// HTTP URL 헬퍼
println(server.url)                // http://localhost:<동적포트>
println(server.httpbinUrl)         // http://localhost:<port>/httpbin
println(server.jsonplaceholderUrl) // http://localhost:<port>/jsonplaceholder
println(server.webUrl)             // http://localhost:<port>/web

// HTTPS URL 헬퍼 (BluetapeSslContext 필요)
val httpsClient = BluetapeSslContext.configureOkHttp(OkHttpClient.Builder()).build()
println(server.httpsUrl)           // https://localhost:<https-port>
```

### Testcontainers 의존성 추가

```kotlin
dependencies {
    testImplementation("io.github.bluetape4k:bluetape4k-testcontainers:${version}")
}
```

### 애플리케이션 직접 실행 (Docker 없이)

```bash
./gradlew :bluetape4k-mock-webflux-server:bootRun
# 서버가 http://localhost:80 에서 시작됩니다
```

### 엔드포인트 참조

#### 기본

| Method | Path           | 설명                                 |
|--------|----------------|------------------------------------|
| `GET`  | `/ping`        | 헬스 체크 — `pong` 반환                  |
| `POST` | `/admin/reset` | 인메모리 Fixture 데이터를 클래스패스 JSON에서 재적재 |
| `GET`  | `/admin/info`  | 서버 정보 반환                           |

#### `/httpbin/**`

| Method   | Path                        | 설명                                   |
|----------|-----------------------------|--------------------------------------|
| `GET`    | `/httpbin/get`              | GET 요청 정보 반환                         |
| `POST`   | `/httpbin/post`             | POST 요청 + body 반환                    |
| `PUT`    | `/httpbin/put`              | PUT 요청 + body 반환                     |
| `PATCH`  | `/httpbin/patch`            | PATCH 요청 + body 반환                   |
| `DELETE` | `/httpbin/delete`           | DELETE 요청 정보 반환                      |
| `GET`    | `/httpbin/headers`          | 모든 요청 헤더 반환                          |
| `GET`    | `/httpbin/ip`               | 클라이언트 IP 반환                          |
| `GET`    | `/httpbin/user-agent`       | User-Agent 헤더 반환                     |
| `GET`    | `/httpbin/uuid`             | 랜덤 UUID 반환                           |
| `ANY`    | `/httpbin/anything/**`      | 임의 요청 에코                             |
| `ANY`    | `/httpbin/status/{code}`    | 지정된 HTTP 상태 코드 반환                    |
| `GET`    | `/httpbin/bytes/{n}`        | `n` 바이트의 랜덤 바이너리 반환                  |
| `GET`    | `/httpbin/delay/{seconds}`  | 지연 후 응답 (`0.5` = 500ms, 범위 0.0–10.0) |
| `GET`    | `/httpbin/stream/{n}`       | `Flow`로 JSON 라인 `n`개 스트리밍            |
| `GET`    | `/httpbin/stream-bytes/{n}` | 랜덤 바이트 `n`개 스트리밍                     |
| `GET`    | `/httpbin/drip`             | 지연 드립 스트리밍                           |
| `GET`    | `/httpbin/sse`              | Server-Sent Events 스트림               |
| `GET`    | `/httpbin/image/{format}`   | 샘플 이미지 반환 (png/jpeg/svg/webp)        |
| `GET`    | `/httpbin/gzip`             | gzip 인코딩 응답                          |
| `GET`    | `/httpbin/deflate`          | deflate 인코딩 응답                       |
| `GET`    | `/httpbin/brotli`           | brotli 인코딩 응답                        |
| `GET`    | `/httpbin/html`             | 샘플 HTML 반환                           |
| `GET`    | `/httpbin/xml`              | 샘플 XML 반환                            |
| `GET`    | `/httpbin/json`             | 샘플 JSON 반환                           |
| `GET`    | `/httpbin/robots.txt`       | robots.txt 반환                        |
| `GET`    | `/httpbin/deny`             | 403 Forbidden 반환                     |

#### `/jsonplaceholder/**`

[jsonplaceholder.typicode.com](https://jsonplaceholder.typicode.com)을 그대로 모방합니다. 모든 리소스는 전체 CRUD를 지원합니다.

| 리소스      | 기본 경로                       |
|----------|-----------------------------|
| Posts    | `/jsonplaceholder/posts`    |
| Comments | `/jsonplaceholder/comments` |
| Albums   | `/jsonplaceholder/albums`   |
| Photos   | `/jsonplaceholder/photos`   |
| Todos    | `/jsonplaceholder/todos`    |
| Users    | `/jsonplaceholder/users`    |

#### `/web/**`

| Method | Path          | 설명                   |
|--------|---------------|----------------------|
| `GET`  | `/web/{name}` | 이름으로 캐시된 HTML 컨텐츠 반환 |
| `GET`  | `/web/random` | 랜덤 HTML 컨텐츠 반환       |
| `GET`  | `/web/naver`  | Naver 형식 HTML 픽스처 반환 |

## 참고

- [httpbin.org](https://httpbin.org)
- [jsonplaceholder.typicode.com](https://jsonplaceholder.typicode.com)
- [Testcontainers](https://www.testcontainers.org/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Jib — Java 앱 컨테이너화](https://github.com/GoogleContainerTools/jib)
