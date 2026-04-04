# Module Examples - Vert.x WebClient

English | [한국어](./README.ko.md)

> **⚠️ Obsolete**: This module has been merged into `bluetape4k-vertx` and fully excluded from the build.

[Vert.x WebClient](https://vertx.io/docs/vertx-web-client/java/) is an async/non-blocking HTTP client. It offers similar functionality to Spring's WebClient, but uses Coroutines instead of Reactor for a simpler programming model.

## Examples

### Basic Usage (SimpleExamples.kt)

Learn the fundamentals of using WebClient.

```kotlin
val client = WebClient.create(vertx)

val response = client.get(8080, "localhost", "/api/users")
    .send()
    .await()
```

### Requests (RequestExamples.kt)

Learn various ways to send HTTP requests.

```kotlin
// GET request
client.get("/api/users/1").send().await()

// POST request with body
client.post("/api/users")
    .putHeader("Content-Type", "application/json")
    .sendBuffer(Buffer.buffer(jsonString))
    .await()

// Query parameters
client.get("/api/users")
    .addQueryParam("page", "1")
    .addQueryParam("size", "10")
    .send()
    .await()
```

### Responses (ResponseExamples.kt)

Learn how to handle HTTP responses.

```kotlin
// Check status code
if (response.statusCode() == 200) {
    // Read body
    val body = response.bodyAsString()
    
    // Parse JSON
    val user = json.decodeFromString<User>(body)
}

// Read headers
val contentType = response.getHeader("Content-Type")
```

### Coroutines (CoroutineExamples.kt)

Learn how to use WebClient with Kotlin Coroutines.

```kotlin
// Parallel requests
val users = async { client.get("/api/users").send().await() }
val orders = async { client.get("/api/orders").send().await() }

val userList = users.await().bodyAsJsonArray()
val orderList = orders.await().bodyAsJsonArray()
```

## Key Learning Points

### 1. Creating a WebClient

```kotlin
// Default
val client = WebClient.create(vertx)

// With options
val options = WebClientOptions()
    .setDefaultHost("api.example.com")
    .setDefaultPort(443)
    .setSsl(true)

val client = WebClient.create(vertx, options)
```

### 2. Sending Requests

```kotlin
// Suspend (Coroutines)
val response = client.get("/path").send().await()

// Callback style
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

### 4. Error Handling

```kotlin
try {
    val response = client.get("/api/data").send().await()
    if (response.statusCode() >= 400) {
        throw ApiException(response.statusMessage())
    }
} catch (e: Exception) {
    // Handle network errors, etc.
}
```

## Running the Examples

```bash
# Run all examples
./gradlew :examples:vertx-webclient:test

# Run a specific example
./gradlew :examples:vertx-webclient:test --tests "*SimpleExamples*"
```

## References

- [Vert.x WebClient Documentation](https://vertx.io/docs/vertx-web-client/java/)
- [Vert.x Kotlin Coroutines](https://vertx.io/docs/vertx-lang-kotlin-coroutines/kotlin/)
- bluetape4k-vertx-core
