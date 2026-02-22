# Module bluetape4k-spring-tests

Spring Boot Application을 테스트할 때 필요한 유용한 기능을 제공합니다.

## 주요 기능

- **RestClient 확장**: 동기 HTTP 클라이언트 확장 함수
- **WebClient 확장**: 비동기 HTTP 클라이언트 확장 함수
- **WebTestClient 확장**: WebFlux 테스트용 확장 함수

## 의존성 추가

```kotlin
dependencies {
    testImplementation("io.bluetape4k:bluetape4k-spring-tests:${version}")
}
```

## 주요 기능 상세

### 1. RestClient 확장

Spring 6.1+의 `RestClient`를 위한 간결한 HTTP 메소드 확장 함수입니다.

#### GET 요청

```kotlin
import io.bluetape4k.spring.tests.*

@SpringBootTest
class ApiTest {

    @Autowired
    lateinit var restClient: RestClient

    @Test
    fun `GET 요청`() {
        val response = restClient
            .httpGet("/api/users/1")
            .toEntity<String>()

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldNotBe null
    }

    @Test
    fun `GET 요청 with Accept 헤더`() {
        val body = restClient
            .httpGet("/api/users", MediaType.APPLICATION_JSON)
            .body(String::class.java)

        println(body)
    }
}
```

#### POST 요청

```kotlin
@Test
fun `POST 요청`() {
    val request = CreateUserRequest(
        name = "John",
        email = "john@example.com"
    )

    val response = restClient
        .httpPost("/api/users", request, MediaType.APPLICATION_JSON)
        .toEntity<UserResponse>()

    response.statusCode shouldBe HttpStatus.CREATED
}

@Test
fun `POST 요청 with Flow`() = runSuspendTest {
    val userFlow = flowOf(
        CreateUserRequest("User1", "user1@test.com"),
        CreateUserRequest("User2", "user2@test.com")
    )

    val response = restClient
        .httpPost("/api/users/batch", userFlow)
        .toEntity<String>()

    println(response.body)
}
```

#### PUT, PATCH, DELETE 요청

```kotlin
@Test
fun `PUT 요청`() {
    val request = UpdateUserRequest(name = "Updated Name")

    restClient
        .httpPut("/api/users/1", request)
        .toBodilessEntity()
}

@Test
fun `PATCH 요청`() {
    val patch = mapOf("status" to "ACTIVE")

    restClient
        .httpPatch("/api/users/1/status", patch)
        .body(Void::class.java)
}

@Test
fun `DELETE 요청`() {
    restClient
        .httpDelete("/api/users/1")
        .toBodilessEntity()
}
```

#### HEAD, OPTIONS 요청

```kotlin
@Test
fun `HEAD 요청`() {
    val response = restClient
        .httpHead("/api/users/1")
        .toBodilessEntity()

    response.headers.containsKey("Content-Length") shouldBe true
}

@Test
fun `OPTIONS 요청`() {
    val response = restClient
        .httpOptions("/api/users")
        .toEntity<String>()

    println("Allowed methods: ${response.body}")
}
```

---

### 2. WebClient 확장

WebFlux의 `WebClient`를 위한 코루틴 친화적 확장 함수입니다.

#### GET 요청

```kotlin
import io.bluetape4k.spring.tests.*

@SpringBootTest
class WebClientTest {

    @Autowired
    lateinit var webClient: WebClient

    @Test
    fun `GET 요청`() = runSuspendTest {
        val user = webClient
            .httpGet("/api/users/1")
            .awaitBody<User>()

        user.name shouldBe "John"
    }

    @Test
    fun `GET 요청 Flow`() = runSuspendTest {
        webClient
            .httpGet("/api/users")
            .bodyToFlow<User>()
            .collect { user ->
                println(user)
            }
    }
}
```

#### POST 요청

```kotlin
@Test
fun `POST 요청`() = runSuspendTest {
        val request = CreateUserRequest(
            name = "John",
            email = "john@example.com"
        )

        val created = webClient
            .httpPost("/api/users", request, MediaType.APPLICATION_JSON)
            .awaitBody<User>()

        created.id shouldNotBe null
    }

@Test
fun `POST 요청 with Publisher`() = runSuspendTest {
    val users = Mono.just(CreateUserRequest("John", "john@test.com"))

    val response = webClient
        .httpPost("/api/users", users)
        .awaitBody<User>()

    println(response)
}
```

#### PUT, PATCH, DELETE 요청

```kotlin
@Test
fun `PUT 요청`() = runSuspendTest {
        val request = UpdateUserRequest(name = "Updated")

        webClient
            .httpPut("/api/users/1", request)
            .awaitBodilessEntity()
    }

@Test
fun `PATCH 요청`() = runSuspendTest {
    val patch = mapOf("status" to "INACTIVE")

    val updated = webClient
        .httpPatch("/api/users/1/status", patch)
        .awaitBody<User>()

    updated.status shouldBe "INACTIVE"
}

@Test
fun `DELETE 요청`() = runSuspendTest {
    webClient
        .httpDelete("/api/users/1")
        .awaitBodilessEntity()
}
```

---

### 3. WebTestClient 확장

WebFlux 컨트롤러 테스트를 위한 확장 함수입니다. HTTP 상태 코드를 파라미터로 직접 지정할 수 있습니다.

#### GET 요청

```kotlin
import io.bluetape4k.spring.tests.*

@SpringBootTest
@AutoConfigureWebTestClient
class ControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `GET 요청`() {
        webTestClient
            .httpGet("/api/users/1", HttpStatus.OK)
            .expectBody<User>()
            .value { user ->
                user.name shouldBe "John"
            }
    }

    @Test
    fun `GET 요청 404`() {
        webTestClient
            .httpGet("/api/users/999", HttpStatus.NOT_FOUND)
            .expectBody()
            .jsonPath("$.message").exists()
    }
}
```

#### POST 요청

```kotlin
@Test
fun `POST 요청`() {
    val request = CreateUserRequest(
        name = "John",
        email = "john@example.com"
    )

    webTestClient
        .httpPost("/api/users", request, HttpStatus.CREATED)
        .expectBody<User>()
        .jsonPath("$.id").exists()
        .jsonPath("$.name").isEqualTo("John")
}

@Test
fun `POST 요청 검증 실패`() {
    val invalidRequest = CreateUserRequest(
        name = "",  // Invalid
        email = "invalid-email"  // Invalid
    )

    webTestClient
        .httpPost("/api/users", invalidRequest, HttpStatus.BAD_REQUEST)
        .expectBody()
        .jsonPath("$.errors").isArray
}
```

#### PUT 요청

```kotlin
@Test
fun `PUT 요청`() {
    val request = UpdateUserRequest(name = "Updated Name")

    webTestClient
        .httpPut("/api/users/1", request, HttpStatus.OK)
        .expectBody<User>()
        .jsonPath("$.name").isEqualTo("Updated Name")
}

@Test
fun `PUT 요청 with Flow`() {
    val updates = flowOf(
        UpdateUserRequest("User1"),
        UpdateUserRequest("User2")
    )

    webTestClient
        .httpPut("/api/users/batch", updates, HttpStatus.OK)
        .expectBody()
        .jsonPath("$.updated").isEqualTo(2)
}
```

#### PATCH, DELETE 요청

```kotlin
@Test
fun `PATCH 요청`() {
    val patch = mapOf("status" to "ARCHIVED")

    webTestClient
        .httpPatch("/api/users/1/status", patch, HttpStatus.OK)
        .expectBody<User>()
}

@Test
fun `DELETE 요청`() {
    webTestClient
        .httpDelete("/api/users/1", HttpStatus.NO_CONTENT)
        .expectBody().isEmpty
}
```

#### HEAD, OPTIONS 요청

```kotlin
@Test
fun `HEAD 요청`() {
    webTestClient
        .httpHead("/api/users/1", HttpStatus.OK)
        .expectHeader().exists("Content-Length")
}

@Test
fun `OPTIONS 요청`() {
    webTestClient
        .httpOptions("/api/users", HttpStatus.OK)
        .expectHeader().value("Allow") {
            it shouldContain "GET"
            it shouldContain "POST"
        }
}
```

---

### 4. 전체 테스트 예시

```kotlin
@SpringBootTest
@AutoConfigureWebTestClient
class UserControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockkBean
    lateinit var userService: UserService

    @Test
    fun `사용자 조회`() {
        every { userService.findById(1L) } returns
                User(id = 1L, name = "John", email = "john@test.com")

        webTestClient
            .httpGet("/api/users/1", HttpStatus.OK)
            .expectBody<User>()
            .value { user ->
                user.id shouldBe 1L
                user.name shouldBe "John"
            }
    }

    @Test
    fun `사용자 생성`() {
        val request = CreateUserRequest("John", "john@test.com")
        val expected = User(id = 1L, name = "John", email = "john@test.com")

        every { userService.create(any()) } returns expected

        webTestClient
            .httpPost(
                uri = "/api/users",
                value = request,
                httpStatus = HttpStatus.CREATED,
                contentType = MediaType.APPLICATION_JSON
            )
            .expectBody<User>()
            .value { user ->
                user.id shouldBe 1L
            }
    }

    @Test
    fun `사용자 삭제`() {
        every { userService.delete(1L) } returns Unit

        webTestClient
            .httpDelete("/api/users/1", HttpStatus.NO_CONTENT)
            .expectBody().isEmpty
    }
}
```

---

## 지원 HTTP 메소드

| 메소드     | RestClient             | WebClient              | WebTestClient                  |
|---------|------------------------|------------------------|--------------------------------|
| GET     | `httpGet(uri)`         | `httpGet(uri)`         | `httpGet(uri, status)`         |
| HEAD    | `httpHead(uri)`        | `httpHead(uri)`        | `httpHead(uri, status)`        |
| POST    | `httpPost(uri, body)`  | `httpPost(uri, body)`  | `httpPost(uri, body, status)`  |
| PUT     | `httpPut(uri, body)`   | `httpPut(uri, body)`   | `httpPut(uri, body, status)`   |
| PATCH   | `httpPatch(uri, body)` | `httpPatch(uri, body)` | `httpPatch(uri, body, status)` |
| DELETE  | `httpDelete(uri)`      | `httpDelete(uri)`      | `httpDelete(uri, status)`      |
| OPTIONS | `httpOptions(uri)`     | `httpOptions(uri)`     | `httpOptions(uri, status)`     |

---

## 테스트

```bash
./gradlew :spring:tests:test
```

## 참고

- [Spring Framework Testing](https://docs.spring.io/spring-framework/reference/testing.html)
- [WebTestClient Reference](https://docs.spring.io/spring-framework/reference/testing/webtestclient.html)
