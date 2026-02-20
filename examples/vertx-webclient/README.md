# Examples - Vert.x WebClient

[Vert.x WebClient](https://vertx.io/docs/vertx-web-client/java/)는 Async/Non-Blocking 방식의 HTTP 클라이언트입니다. Spring의 WebClient와 비슷한 기능을 제공하지만, Reactor 대신 Coroutines를 사용하여 더 쉽게 구현할 수 있습니다.

## 예제 목록

### 기본 (SimpleExamples.kt)

WebClient 기본 사용법을 학습합니다.

```kotlin
val client = WebClient.create(vertx)

val response = client.get(8080, "localhost", "/api/users")
    .send()
    .await()
```

### 요청 (RequestExamples.kt)

다양한 HTTP 요청 방법을 학습합니다.

```kotlin
// GET 요청
client.get("/api/users/1").send().await()

// POST 요청 with Body
client.post("/api/users")
    .putHeader("Content-Type", "application/json")
    .sendBuffer(Buffer.buffer(jsonString))
    .await()

// Query Parameter
client.get("/api/users")
    .addQueryParam("page", "1")
    .addQueryParam("size", "10")
    .send()
    .await()
```

### 응답 (ResponseExamples.kt)

응답 처리 방법을 학습합니다.

```kotlin
// 상태 코드 확인
if (response.statusCode() == 200) {
    // Body 읽기
    val body = response.bodyAsString()
    
    // JSON 파싱
    val user = json.decodeFromString<User>(body)
}

// Header 읽기
val contentType = response.getHeader("Content-Type")
```

### Coroutines (CoroutineExamples.kt)

Kotlin Coroutines와 함께 사용하는 방법을 학습합니다.

```kotlin
// 병렬 요청
val users = async { client.get("/api/users").send().await() }
val orders = async { client.get("/api/orders").send().await() }

val userList = users.await().bodyAsJsonArray()
val orderList = orders.await().bodyAsJsonArray()
```

## 주요 학습 포인트

### 1. WebClient 생성

```kotlin
// 기본
val client = WebClient.create(vertx)

// 옵션 설정
val options = WebClientOptions()
    .setDefaultHost("api.example.com")
    .setDefaultPort(443)
    .setSsl(true)

val client = WebClient.create(vertx, options)
```

### 2. 요청 전송

```kotlin
// 동기 대기 (Coroutines)
val response = client.get("/path").send().await()

// 콜백 방식
client.get("/path").send { ar ->
    if (ar.succeeded()) {
        println(ar.result().bodyAsString())
    }
}
```

### 3. multipart/form-data

```kotlin
val form = MultipartForm.create()
    .attribute("name", "value")
    .binaryFileUpload("file", "test.txt", buffer, "text/plain")

client.post("/upload")
    .sendMultipartForm(form)
    .await()
```

### 4. 에러 처리

```kotlin
try {
    val response = client.get("/api/data").send().await()
    if (response.statusCode() >= 400) {
        throw ApiException(response.statusMessage())
    }
} catch (e: Exception) {
    // 네트워크 오류 등
}
```

## 실행 방법

```bash
# 모든 예제 실행
./gradlew :examples:vertx-webclient:test

# 특정 예제만 실행
./gradlew :examples:vertx-webclient:test --tests "*SimpleExamples*"
```

## 참고

- [Vert.x WebClient 문서](https://vertx.io/docs/vertx-web-client/java/)
- [Vert.x Kotlin Coroutines](https://vertx.io/docs/vertx-lang-kotlin-coroutines/kotlin/)
- [bluetape4k-vertx-core](../../vertx/core/README.md)
