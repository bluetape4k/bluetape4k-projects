# Module bluetape4k-spring-retrofit2

Spring Framework 기반에서 Retrofit2를 손쉽게 사용하기 위한 라이브러리입니다.

## 주요 기능

- **@Retrofit2Client 어노테이션**: 인터페이스 기반 Retrofit 클라이언트 정의
- **@EnableRetrofitClients**: 자동 컴포넌트 스캔 및 빈 등록
- **코루틴 지원**: suspend 함수로 비동기 API 호출
- **Call 지원**: Retrofit Call 타입 반환
- **다양한 HTTP 클라이언트**: OkHttp, AsyncHttpClient, Vert.x 지원
- **Micrometer 연동**: 메트릭 수집 지원

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-spring-retrofit2:${version}")
}
```

## 주요 기능 상세

### 1. Retrofit 클라이언트 정의

#### 코루틴 기반 API (suspend 함수)

```kotlin
import io.bluetape4k.spring.retrofit2.Retrofit2Client
import retrofit2.http.*

@Retrofit2Client(
    name = "jsonPlaceHolderApi",
    baseUrl = "\${api.jsonplaceholder.base-url}"
)
interface JsonPlaceHolderCoroutineApi {

    @GET("/posts")
    suspend fun getPosts(): List<Post>

    @GET("/posts/{id}")
    suspend fun getPost(@Path("id") postId: Int): Post

    @GET("/posts")
    suspend fun getUserPosts(@Query("userId") userId: Int): List<Post>

    @POST("/posts")
    suspend fun createPost(@Body post: Post): Post

    @PUT("/posts/{id}")
    suspend fun updatePost(@Path("id") postId: Int, @Body post: Post): Post

    @DELETE("/posts/{id}")
    suspend fun deletePost(@Path("id") postId: Int): Post
}
```

#### Call 기반 API (동기/비동기)

```kotlin
import retrofit2.Call

@Retrofit2Client(
    name = "externalApi",
    baseUrl = "\${api.external.base-url}"
)
interface ExternalApi {

    @GET("/users")
    fun getUsers(): Call<List<User>>

    @GET("/users/{id}")
    fun getUser(@Path("id") userId: Int): Call<User>

    @POST("/users")
    fun createUser(@Body user: User): Call<User>
}
```

---

### 2. Spring 설정

#### 애플리케이션 설정

```kotlin
import io.bluetape4k.spring.retrofit2.EnableRetrofitClients
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
@EnableRetrofitClients  // Retrofit 클라이언트 자동 스캔
class Application
```

#### application.yml

```yaml
api:
  jsonplaceholder:
    base-url: https://jsonplaceholder.typicode.com
  external:
    base-url: https://api.example.com

# 또는 bluetape4k prefix 사용
bluetape4k:
  retrofit2:
    services:
      jsonPlaceHolder: https://jsonplaceholder.typicode.com
      httpbin: https://httpbin.org
```

#### 패키지 스캔 범위 지정

```kotlin
@SpringBootApplication
@EnableRetrofitClients(
    basePackages = ["com.example.api"],  // 스캔할 패키지
    basePackageClasses = [MyApi::class], // 또는 특정 클래스 기준
    clients = [ExternalApi::class]       // 또는 명시적 지정
)
class Application
```

---

### 3. 서비스에서 사용

```kotlin
import org.springframework.stereotype.Service

@Service
class PostService(
    private val postApi: JsonPlaceHolderCoroutineApi
) {
    
    suspend fun getAllPosts(): List<Post> {
        return postApi.getPosts()
    }
    
    suspend fun getPostById(id: Int): Post {
        return postApi.getPost(id)
    }
    
    suspend fun createPost(title: String, body: String, userId: Int): Post {
        val post = Post(
            title = title,
            body = body,
            userId = userId
        )
        return postApi.createPost(post)
    }
}
```

#### 컨트롤러에서 사용

```kotlin
@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService
) {
    
    @GetMapping
    suspend fun list(): List<Post> {
        return postService.getAllPosts()
    }
    
    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: Int): Post {
        return postService.getPostById(id)
    }
    
    @PostMapping
    suspend fun create(@RequestBody request: CreatePostRequest): Post {
        return postService.createPost(
            title = request.title,
            body = request.body,
            userId = request.userId
        )
    }
}
```

---

### 4. 커스텀 설정

#### 기본 설정 클래스

```kotlin
import io.bluetape4k.spring.retrofit2.Retrofit2Client
import io.bluetape4k.spring.retrofit2.RetrofitClientSpecification
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import retrofit2.Call
import retrofit2.Converter
import retrofit2.converter.jackson.JacksonConverterFactory

@Configuration
class RetrofitConfig {
    
    @Bean
    fun jsonPlaceHolderSpec(): RetrofitClientSpecification {
        return RetrofitClientSpecification(
            name = "jsonPlaceHolderApi",
            configuration = CustomClientConfig::class
        )
    }
}

@Configuration
class CustomClientConfig {
    
    @Bean
    fun customConverterFactory(): Converter.Factory {
        return JacksonConverterFactory.create(
            JsonMapper.builder()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .build()
        )
    }
}
```

#### @Retrofit2Client로 설정 지정

```kotlin
@Retrofit2Client(
    name = "customApi",
    baseUrl = "\${api.custom.base-url}",
    configuration = [CustomClientConfig::class],  // 커스텀 설정
    qualifier = "customApiV2"  // Qualifier 지정
)
interface CustomApi {
    @GET("/data")
    suspend fun getData(): Data
}
```

---

### 5. HTTP 클라이언트 선택

기본적으로 다음 순서로 HTTP 클라이언트를 자동 선택합니다:

1. 커스텀 `OkHttpClient` 빈이 있으면 사용
2. 커스텀 `Call.Factory` 빈이 있으면 사용
3. Vert.x HttpClient가 classpath에 있으면 사용
4. AsyncHttpClient 사용 (기본)

#### Vert.x HttpClient 사용

```kotlin
@Configuration
class VertxConfig {
    
    @Bean
    fun vertxHttpClient(): io.vertx.core.http.HttpClient {
        return io.vertx.core.Vertx.vertx()
            .createHttpClient(
                HttpClientOptions()
                    .setConnectTimeout(5000)
                    .setMaxPoolSize(100)
            )
    }
}
```

#### AsyncHttpClient 사용

```kotlin
@Configuration
class AsyncHttpClientConfig {
    
    @Bean
    fun asyncHttpClient(): AsyncHttpClient {
        return Dsl.asyncHttpClient(
            Dsl.config()
                .setConnectTimeout(5000)
                .setMaxConnections(100)
        )
    }
}
```

---

### 6. Micrometer 메트릭 연동

```kotlin
import io.bluetape4k.micrometer.instrument.retrofit2.MicrometerRetrofitMetricsFactory
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfig {
    
    @Bean
    fun micrometerRetrofitMetricsFactory(
        meterRegistry: MeterRegistry
    ): MicrometerRetrofitMetricsFactory {
        return MicrometerRetrofitMetricsFactory(meterRegistry)
    }
}
```

메트릭은 다음과 같이 수집됩니다:

- `retrofit2.requests`: 요청 수
- `retrofit2.latency`: 응답 시간
- `retrofit2.errors`: 에러 수

---

### 7. Resilience4j 연동

Circuit Breaker와 Retry를 적용할 수 있습니다.

```kotlin
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.kotlin.circuitbreaker.decorateSuspendFunction
import io.github.resilience4j.kotlin.retry.decorateSuspendFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.ratpack.decorateSuspendFunction

@Service
class ResilientPostService(
    private val postApi: JsonPlaceHolderCoroutineApi
) {
    
    private val circuitBreaker = CircuitBreaker.ofDefaults("postApi")
    private val retry = Retry.ofDefaults("postApi")
    
    suspend fun getPostWithResilience(id: Int): Post {
        val decorated = decorateSuspendFunction(circuitBreaker) {
            decorateSuspendFunction(retry) {
                postApi.getPost(id)
            }()
        }
        
        return decorated()
    }
}
```

---

### 8. 테스트 작성

```kotlin
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.junit5.random.RandomValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@RandomizedTest
class JsonPlaceHolderApiTest {

    @Autowired
    private lateinit var api: JsonPlaceHolderCoroutineApi

    @Test
    fun `context loading`() {
        api.shouldNotBeNull()
    }

    @Test
    fun `get all posts`() = runSuspendIO {
        val posts = api.getPosts()
        
        posts.shouldNotBeEmpty()
        posts.forEach { post ->
            post.id shouldBeGreaterThan 0
            post.title.shouldNotBeEmpty()
        }
    }

    @Test
    fun `get post by id`(@RandomValue(type = Int::class) randomId: Int) = runSuspendIO {
        val postId = randomId.absoluteValue % 100 + 1
        
        val post = api.getPost(postId)
        
        post.shouldNotBeNull()
        post.id shouldBeEqualTo postId
    }

    @Test
    fun `create post`() = runSuspendIO {
        val newPost = Post(
            title = "Test Post",
            body = "Test Body",
            userId = 1
        )
        
        val created = api.createPost(newPost)
        
        created.id.shouldNotBeNull()
        created.title shouldBeEqualTo newPost.title
    }
    
    @Test
    fun `parallel requests`() = runSuspendIO {
        val deferreds = (1..10).map { id ->
            async(Dispatchers.IO) {
                api.getPost(id)
            }
        }
        
        val posts = deferreds.awaitAll()
        posts.size shouldBeEqualTo 10
    }
}
```

---

### 9. 전체 설정 예시

```kotlin
// application.yml
/*
api:
  jsonplaceholder:
    base-url: https://jsonplaceholder.typicode.com
  httpbin:
    base-url: https://httpbin.org
*/

// API 인터페이스 정의
@Retrofit2Client(name = "jsonPlaceHolderApi", baseUrl = "\${api.jsonplaceholder.base-url}")
interface JsonPlaceHolderApi {
    @GET("/posts")
    suspend fun getPosts(): List<Post>
}

// 서비스 구현
@Service
class PostService(private val api: JsonPlaceHolderApi) {
    suspend fun getPosts(): List<Post> = api.getPosts()
}

// 컨트롤러
@RestController
@RequestMapping("/api/posts")
class PostController(private val postService: PostService) {
    @GetMapping
    suspend fun list() = postService.getPosts()
}

// 애플리케이션
@SpringBootApplication
@EnableRetrofitClients
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
```

---

## @Retrofit2Client 속성

| 속성              | 설명               | 기본값 |
|-----------------|------------------|-----|
| `name`          | 클라이언트 식별자        | ""  |
| `value`         | name과 동일         | ""  |
| `baseUrl`       | API 기본 URL       | ""  |
| `qualifier`     | Spring Qualifier | ""  |
| `configuration` | 커스텀 설정 클래스       | []  |

## @EnableRetrofitClients 속성

| 속성                     | 설명               | 기본값 |
|------------------------|------------------|-----|
| `value`                | basePackages와 동일 | []  |
| `basePackages`         | 스캔할 패키지          | []  |
| `basePackageClasses`   | 스캔 기준 클래스        | []  |
| `defaultConfiguration` | 기본 설정 클래스        | []  |
| `clients`              | 명시적 클라이언트 목록     | []  |

---

## 테스트

```bash
./gradlew :spring:retrofit2:test
```

## 참고

- [Retrofit2 공식 문서](https://square.github.io/retrofit/)
- [Spring Boot Auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)
