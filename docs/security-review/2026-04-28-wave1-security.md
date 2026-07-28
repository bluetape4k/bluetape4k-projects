# Wave 1 Security 검토 — 2026-04-28

전체 7개 그룹 병렬 실행 결과. Tier 1 (autoresearch:security) 기준.

## 전체 요약

| 그룹             | CRITICAL | HIGH   | MEDIUM |
|------------------|----------|--------|--------|
| core + testing   | 0        | 2      | 4      |
| io + texts       | 1        | 2      | 3      |
| utils            | 1        | 3      | 4      |
| data             | 0        | 2      | 4      |
| aws              | 0        | 0      | 1      |
| infra            | 0        | 1      | 4      |
| spring-boot + vt | 0        | 3      | 5      |
| **합계**         | **2**    | **13** | **25** |

---

## CRITICAL

### C1 — ProtobufSerializer + JDK fallback → RCE

- **그룹:** io + texts
- **파일:**
    - `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializer.kt:27,47-56`
    - `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufCodecs.kt:17`
    - `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodec.kt`
    - `io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializers.kt:67`
    - `io/io/src/main/kotlin/io/bluetape4k/io/serializer/JdkBinarySerializer.kt:77-86`
-

**이슈:** `ProtoAny.parseFrom(bytes)` 성공 시 attacker-controlled `typeUrl` → `Class.forName(className)` 실행 (static initializer 포함). 파싱 실패 시 `fallback.deserialize(bytes)` = `JdkBinarySerializer` → `ObjectInputStream.readObject()` with no `ObjectInputFilter`. 두 경로 모두 RCE.
- **수정:**
    1. `allowedMessageTypes: Set<Class<out ProtoMessage>>` allow-list 필수 파라미터로 추가
    2. `Class.forName` 제거 → allow-list 직접 조회
    3. JDK fallback 제거 또는 Kryo (isRegistrationRequired=true)로 교체
    4. `BinarySerializers.Default = Jdk` → `Kryo`로 변경

### C2 — KeyChainDto JDK 역직렬화 (RSA 개인키 Redis 저장)

- **그룹:** utils
- **파일:**
    - `utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/KeyChainDto.kt:63-73`
    - `utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/redis/RedisKeyChainRepository.kt:55,64`
    - `io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializers.kt:154` (`LZ4Jdk`)
-

**이슈:** `serializer = BinarySerializers.LZ4Jdk`로 RSA 개인키 포함 `KeyChainDto`를 Redis에 JDK 직렬화. Redis 접근 시 gadget chain → RCE + 개인키 탈취로 JWT 전체 위조.
- **수정:**
    1. JDK 직렬화 → `PKCS8EncodedKeySpec` / `X509EncodedKeySpec` 교체
    2. 개인키 at-rest 암호화 (envelope encryption)
    3. 불가피 시 `ObjectInputFilter` 엄격 설정

---

## HIGH

### H1 — Path Traversal (WebContentLoader)

- **그룹:** core + testing
- **파일:**
    - `testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/web/WebContentLoader.kt:27`
    - `testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/web/WebContentController.kt:38`
    - `testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/web/WebContentLoader.kt:33`
    - `testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/web/WebContentController.kt:58-60`
-

**이슈:** `@PathVariable name` → `ClassPathResource("web/html/$name.html")` allow-list 없이 직접 보간. classpath 내 임의 리소스 노출 가능.
- **수정:** `require(name in ALLOWED_NAMES)` 검증 추가 (컨트롤러의 `HTML_NAMES`를 `load()` 내부로 이동)

### H2 — 인증 없는 /admin/reset

- **그룹:** core + testing
- **파일:**
    - `testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/admin/AdminController.kt:32-38`
    - `testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/admin/AdminController.kt`
- **이슈:** `POST /admin/reset` 인증·CSRF·IP 제한 없음. Docker 이미지로 배포 시 외부에서 상태 초기화 가능.
- **수정:** `X-Admin-Token` 헤더 (env var `MOCKSERVER_ADMIN_TOKEN`) 검증 추가

### H3 — JdkBinarySerializer ObjectInputFilter 없음

- **그룹:** io + texts
- **파일:**
    - `io/io/src/main/kotlin/io/bluetape4k/io/serializer/JdkBinarySerializer.kt:30-32`
    - `io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializers.kt:50,67`
- **이슈:** `BinarySerializers.Default = Jdk`, `ObjectInputFilter = null`. 역직렬화 gadget chain → RCE.
-

**수정:** `DEFAULT_FILTER = createFilter("io.bluetape4k.**;java.base/*;kotlin.**;!*")` 기본값 설정. `BinarySerializers.Default` → `Kryo`로 변경. `Jdk` → `@Deprecated`

### H4 — AbstractGrpcClient 기본값 usePlaintext ()

- **그룹:** io + texts
- **파일:**
    - `io/grpc/src/main/kotlin/io/bluetape4k/grpc/AbstractGrpcClient.kt:35-39`
    - `io/grpc/src/main/kotlin/io/bluetape4k/grpc/inprocess/AbstractGrpcInprocessClient.kt:41,50`
- **이슈:** 기본 생성자가 `usePlaintext()` → TLS 없는 gRPC 채널. 운영 환경 실수 배포 시 평문 통신.
- **수정:** TLS 기본값, `plaintext=true` 명시 opt-in + localhost 제한 가드

### H5 — ULID 비암호화 Random

- **그룹:** utils
- **파일:** `utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/internal/ULIDFactory.kt:6-7,16,27`
- **이슈:** `kotlin.random.Random.Default` (선형 PRNG) 사용. ULID를 세션/토큰 ID로 쓰면 예측 가능.
- **수정:** `SecureRandom().asKotlinRandom()` 기본값으로 변경

### H6 — Hashids 빈 default salt

- **그룹:** utils
- **파일:** `utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/hashids/Hashids.kt:33`
- **이슈:** `DEFAULT_SALT = ""` → 공개 Hashids 참조 구성과 동일, trivially reversible.
- **수정:** salt 파라미터 필수화 + KDoc "보안 토큰에 사용 금지" 경고

### H7 — JwtReaderDto.toJwtReader () 서명 검증 우회

- **그룹:** utils
- **파일:** `utils/jwt/src/main/kotlin/io/bluetape4k/jwt/reader/JwtReaderSupport.kt:49-60`
- **이슈:** 캐시에서 복원 시 서명 재검증 없이 claims를 신뢰. 캐시 오염 → 위조 JWT 수용.
- **수정:** 원본 token string 저장 후 `provider.parse(tokenString)` 재파싱으로 교체

### H8 — r2dbc DSL SQL Injection (table/field/where 미검증)

- **그룹:** data
- **파일:**
    - `data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/Delete.kt:110-114`
    - `data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/Update.kt:289-299`
    - `data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/Insert.kt:223-225`
- **이슈:** `"DELETE FROM $table"`, `"$sql WHERE $where"` 등 식별자 미검증 SQL 조합.
- **수정:** `^[A-Za-z_][A-Za-z0-9_]*$` allowlist 정규식 검증 추가

### H9 — VectorDistanceOp.operator SQL Injection

- **그룹:** data
-

**파일:** `data/exposed-postgresql/src/main/kotlin/io/bluetape4k/exposed/postgresql/pgvector/VectorExtensions.kt:129-141`
- **이슈:** public `operator: String` 파라미터가 SQL에 직접 보간.
- **수정:** `VectorDistanceOperator` enum으로 교체

### H10 — Kafka 기본 codec 헤더 기반 Class.forName → RCE

- **그룹:** infra
- **파일:**
    - `infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt:70,114-132`
    - `infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/JacksonKafkaCodec.kt:30-34`
-

**이슈:** `defaultCodec() = JacksonKafkaCodec()`. 헤더 `bluetape4k.kafka.codec.value.type` → `Class.forName(name, true, cl)` → static initializer 실행 + Jackson 임의 클래스 인스턴스화 → RCE.
- **수정:** `targetType: Class<T>` 필수 파라미터화 + allowlist 검증. `Class.forName(name, false, cl)` 변경

### H11 — AbstractCoroutine*Controller 싱글톤 CoroutineScope (보안 컨텍스트 손실)

- **그룹:** spring-boot + vt
- **파일:**
    - `spring-boot3/core/.../controller/AbstractCoroutineIOController.kt:21`
    - `spring-boot3/core/.../controller/AbstractCoroutineVTController.kt:22`
    - `spring-boot3/core/.../controller/AbstractCoroutineDefaultController.kt:21`
    - `spring-boot4/core/.../controller/AbstractCoroutineIOController.kt:20`
    - `spring-boot4/core/.../controller/AbstractCoroutineVTController.kt:21`
    - `spring-boot4/core/.../controller/AbstractCoroutineDefaultController.kt:21`
-

**이슈:** 클래스 레벨 `CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob())` → Spring Security `ReactiveSecurityContextHolder` 비어있음, MDC/tenant 손실, 요청 간 principal 누출 가능.
- **수정:** 클래스 레벨 CoroutineScope 위임 제거 → handler를 `suspend fun`으로 변경

### H12 — 데모 앱 인증/검증 없음

- **그룹:** spring-boot + vt
- **파일:** `spring-boot3,4/exposed-jdbc-demo`, `exposed-r2dbc-demo`, `hibernate-lettuce-demo` 컨트롤러
- **이슈:** Spring Security 없음, `@Valid` 없음, `CacheController.evictAll()` 무인증.
- **수정:** `@Valid` 추가, SecurityFilterChain 추가, README SECURITY.md 경고

### H13 — @RequestBody mass assignment (id 필드 포함)

- **그룹:** spring-boot + vt
- **파일:** `spring-boot3,4/exposed-r2dbc-demo/.../controller/ProductController.kt:38-39`
- **이슈:** 전체 DTO를 `@RequestBody`로 수신 시 `id`, `createdBy`, `createdAt` 등 서버 제어 필드 덮어씌기 가능.
- **수정:** Create/Update 전용 input DTO 분리 (`id` 필드 제외)

---

## MEDIUM (25건)

### core + testing (M1~M4)

- M1: 하드코딩 keystore 패스워드 `"changeit"` + Docker 이미지 번들 (`testing/mock-web-server/HttpsConfiguration.kt:37`)
- M2: `ClassSupport.kt:92-103` — `newInstanceOrNull(qualifiedName)` arbitrary class instantiation 가능 (KDoc 경고 없음)
- M3: `AwaitilityCoroutines.kt:197`, `FieldArgumentsProvider.kt:54` — 리플렉션 `setAccessible(true)` (JDK 업그레이드 취약)
- M4: testcontainers `PASSWORD = "test"` 등이 `public const val`로 배포 JAR에 포함

### io + texts (M5~M7)

- M5: `Jackson.kt:118-178` — `createDefaultJsonMapper(needTypeInfo=true)` 시 `allowIfBaseType(Any::class.java)` → Jackson 폴리모픽 RCE 클래스
- M6: `KryoProvider.kt:161,190`, `ForyBinarySerializer.kt:38,60` — `isRegistrationRequired=false` 기본값
- M7: `ZipFileSupport.kt:281-334` — 압축해제 크기/엔트리 수 제한 없음 (zip bomb DoS)

### utils (M8~M11)

- M8: `JwtComposer.kt:293,298` — TRACE 레벨에서 전체 claim value 로깅
- M9: `JwtProvider.kt:134` — 예외 메시지에 전체 JWT 토큰 문자열 포함
- M10: `JwtProvider.kt:148-151` — `tryParse()` 서명 실패/만료를 동일한 `null`로 처리 (보안 이벤트 구분 불가)
- M11: `JwtConsts.kt:38` — `DEFAULT_KEY_ROTATION_TTL_MILLIS = 365일` (NIST 권장 90일 초과)

### data (M12~M15)

- M12: `R2dbcConnectionConfig.kt:90` — `ssl = false` 기본값
- M13: `Insert.kt:223-225` — 컬럼명 식별자 미검증 (H8와 동일 root cause)
- M14: `GeoExtensions.kt:245,485` — `Double` 거리값 SQL 보간 (`NaN`/`Infinity` → SQL 파싱 오류)
- M15: `LettuceNearCacheProperties.kt:143-156` — Kryo/Fory codec으로 Redis 역직렬화 (신뢰 모델 미문서화)

### aws (M16)

- M16: `aws/auth/AuthSupport.kt:19,33,49`, `aws-kotlin/auth/AuthSupport.kt:10,15,21` — `LocalCredentialsProvider` / `AWS_LOCAL_ACCESS_KEY`가 `src/main`에 위치 → `src/testFixtures`로 이동 필요

### infra (M17~M20)

- M17: `kafka/BinaryKafkaCodecs.kt:41`, `redisson/codec/RedissonCodecs.kt:83` — JDK 직렬화 codec이 1st-class 옵션으로 제공 (`ObjectInputFilter` 없음)
- M18: `redisson/codec/Jackson3Codec.kt:50`, `Fastjson2Codec.kt:52` — `allowedPackagePrefixes=null` 기본값 (allow-all)
- M19: `kafka/codec/KafkaCodec.kt:103` — 역직렬화 실패 로그에 `headers` 전체 출력 (PII 포함 가능)
- M20: `cache-lettuce/LettuceCachingProvider.kt:37` — `DEFAULT_REDIS_URI = "redis://localhost:6379"` (TLS 없음)

### spring-boot + vt (M21~M25)

- M21: `hibernate-lettuce-demo/application.yml:38-41` — `health.show-details: always` 무인증 Actuator
- M22: `r2dbc/.../blog/controller/PostController.kt:25` — 싱글톤 CoroutineScope (H11과 동일 패턴, test 코드)
- M23: `exposed-r2dbc-demo/application.yml:5-12` — `username: sa`, `password:` (빈 값) in `src/main/resources`
- M24: `hibernate-lettuce-demo/.../CacheController.kt:53` — `@PathVariable region` CRLF 미검증 응답 반영
- M25: `AbstractExposedJdbcRepositoryTest.kt:49,52` — `synchronized(this)` VT carrier thread pinning

---

## 수정 우선순위

| 순위 | 심각도   | 건수 | 기한              |
|------|----------|------|-------------------|
| 1    | CRITICAL | 2    | 즉시              |
| 2    | HIGH     | 13   | Wave 전체 완료 후 |
| 3    | MEDIUM   | 25   | MEDIUM 일괄 처리  |

---

*Wave 2~5 결과는 동일 디렉토리에 추가 파일로 저장 예정*
