# Module bluetape4k-jwt

## TODO

현재 제작된 버전은 RSA 방식이라 KeyChain 관리 등이 상당히 복잡하다
차라리 Access Token, Refresh Token 을 활용하고, Algorithm을 HMAC 방식으로 사용하는 것이 더 좋을 것 같다.

Spring Security 에서 이미 JWT 를 제공하므로, 굳이 이렇게 따로 제공할 필요가 없을 듯 하다

참고: [Jwt Refresh Token 적용기](https://velog.io/@jkijki12/Jwt-Refresh-Token-%EC%A0%81%EC%9A%A9%EA%B8%B0)

참고 : workshop/webflux-security (단 JWT Library는 jjwt 가 최신이다) -> Spring Security의 JWT 를 사용하는게 낫다.

Json Web Token 을 생성하고, Parsing 하는 라이브러리입니다.

## 사용법

### JWT Token 만들기

JWT Token 문자열을 만드는 방식은 다음과 같이 `JwtProvider` 를 먼저 생성하고, 관련된 환경설을 수행한 후 `compose()` 함수를 이용하여 생성할 수 있습니다.

```kotlin
val jwtProvider = JwtProvider.default()
val jwtString = jwtProvider.composer()
    .header("x-service", "bluetape4k")
    .claim("author", "debop")
    .expirationAfterMinutes(60L)
    .compose()
```

다음은 Kotlin DSL 기능을 이용하여, jwt token 을 생성하는 예제입니다.

```kotlin
val jwt: String = composeJwt(KeyChain()) {
    header("x-author", "debop")
    claim("service", "bluetape4k")
    claim("library", "bluetape4k-utils-jwt")

    expirationAfterMinutes = 60L
}
```

전달된 JWT Token을 파싱하려면 `JwtReader`를 통해 정보를 추출할 수 있습니다.

```kotlin
val now = Date()
val nowSeconds = now.epochSeconds

val jwtProvider = JwtProvider.default()
val jwt = jwtProvider.composer()
    .header("x-author", "debop")
    .claim("claim1", claim1)
    .claim("claim2", claim2)
    .claim("claim3", claim3)
    .issuer("bluetape4k")
    .issuedAt(now)
    .expirationAfterMinutes(60L)
    .compose()

println("jwt=$jwt")

// parsing jwt token string.
val reader = jwtPropvider.parse(jwt)
reader.header<String>("x-author") shouldBeEqualTo "debop"
reader.claim<String>("claim1") shouldBeEqualTo claim1
reader.claim<String>("claim2") shouldBeEqualTo claim2
reader.claim<Long>("claim3") shouldBeEqualTo claim3

reader.issuer shouldBeEqualTo "bluetape4k"
reader.issuedAt.time shouldBeEqualTo nowSeconds
reader.expiration.time shouldBeGreaterThan now.time
```

### JwtProvider 사용법

jwt token 을 만들 때, 주기적으로 `rotate` 를 통해 다른 key를 생성해야 보안에 안정적입니다. 그리고, 발급된 jwt token 이 오래된 것이라면 `JwtReader`로 parsing 을 할 수
없도록 해줘야 합니다. 이를 위해 `JwtKeyManager` 에서는 주기적으로 자동으로 rotate 하는 기능과 오래된 jwt token 을 파싱할 수 있도록, `KeyChain` 의 버퍼를 가지고 있습니다.

```kotlin
fun JwtProviderFactory.default(
    signatureAlgorithm: SignatureAlgorithm = DefaultSignatureAlgorithm,
    keyRotationQueueSize: Int = DefaultJwtKeyManager.DEFAULT_ROTATION_QUEUE_SIZE,
    keyRotationMinutes: Int = DefaultJwtKeyManager.DEFAULT_KEY_ROTATION_MINUTES,
): DefaultJwtProvider =
    DefaultJwtProvider(signatureAlgorithm, keyRotationQueueSize, keyRotationMinutes)
```

다음은 생성된 jwt 를 parsing 하는 작업 중, 오래된 jwt token 에서는 예외가 발생하게 됩니다 (유효하지 않은 jwt 입니다)

```kotlin
val jwtProvider = JwtProviderFactory.default()
val jwtString = jwtProvider.composer()
    .claim("author", "debop")
    .compose()

val reader = jwtProvider.parse(jwtString)
println("kid=${reader.header<String>(HEADER_KEY_ID)}")

jwtProvider.rotate()

val reader2 = jwtProvider.parse(jwtString)
println("kid=${reader2.header<String>(HEADER_KEY_ID)}")

// 오래된 KeyChain 을 버린다 
jwtProvider.rotate()
jwtProvider.rotate()
jwtProvider.rotate()

// Expired 된 jwt 를 읽을 때 예외를 발생시킵니다.
assertFailsWith<SecurityException> {
    jwtProvider.parse(jwtString)
}
```

### 분산환경에서의 작업

멀티 서버에서 JWT 를 발급하고, 인증하기 위해서는 KeyPair 를 공유해야 합니다.
이를 위해 Redis 나 MongoDB를 저장소로 사용하고, 새롭게 생성된 `KeyChain` 정보를 저장하고, 주기적으로 메모리로 로드해야 합니다. 즉 특정 서버가 rotate 를 수행하면 다른
서버들에게도 `KeyChain` 정보가 전파되어야 하는데, 이를 위해 1분단위로 Refresh 하도록 합니다.
또한 현재 서버에서 사용하는 KeyPair 가 아닌 경우에도 `KeyChain.id` (jwt header에 kid로 저장됨) 를 이용하여 저장소에 저장되 `KeyChain` 을 로드하여 사용합니다.

다음 예제는 `RedisKeyChainRepository` 를 지정한 `JwtProvider` 를 사용합니다.

```kotlin
val persister: KeyChainRepository = RedisKeyChainRepository(redissonClient)
val provider = JwtProviderFactory.default(persister)

// 새로운 KeyChain 을 하나 만들고, Redis 에 추가합니다.
// 다른 서버에서는 1분마다 조회하여 새로운 KeyChain이 있다면 current 를 변경합니다.
provider.rotate()
```
