# Hibernate Lettuce Spring Boot Auto-Configuration 이관 설계

## 1. 개요

### 목적
`bluetape4k-experimental`의 Hibernate 2nd Level Cache Lettuce NearCache Spring Boot Auto-Configuration 라이브러리와 데모 앱을
`bluetape4k-projects`의 `spring-boot3/` 및 `spring-boot4/` 하위로 이관한다.

### 범위
- Auto-Configuration 라이브러리 2개 (Spring Boot 3, Spring Boot 4)
- 데모 앱 2개 (Spring Boot 3, Spring Boot 4)
- 총 4개 신규 모듈 생성

### 핵심 기능
- `application.yml` 기반 Hibernate 2nd Level Cache 자동 설정 (`LettuceNearCacheRegionFactory`)
- Micrometer Metrics 자동 등록 (active regions, local cache size)
- Spring Boot Actuator `/actuator/nearcache` endpoint

---

## 2. 소스 → 대상 매핑

| # | 소스 (bluetape4k-experimental) | 대상 (bluetape4k-projects) | 모듈명 |
|---|-------------------------------|---------------------------|--------|
| 1 | `spring-boot/hibernate-lettuce/` | `spring-boot3/hibernate-lettuce/` | `bluetape4k-spring-boot3-hibernate-lettuce` |
| 2 | `spring-boot/hibernate-lettuce/` | `spring-boot4/hibernate-lettuce/` | `bluetape4k-spring-boot4-hibernate-lettuce` |
| 3 | `examples/spring-boot-hibernate-lettuce-demo/` | `spring-boot3/hibernate-lettuce-demo/` | `bluetape4k-spring-boot3-hibernate-lettuce-demo` |
| 4 | `examples/spring-boot-hibernate-lettuce-demo/` | `spring-boot4/hibernate-lettuce-demo/` | `bluetape4k-spring-boot4-hibernate-lettuce-demo` |

> `settings.gradle.kts`의 `includeModules("spring-boot3", withBaseDir = true)` 패턴에 의해 디렉토리명이 자동으로 모듈명에 매핑된다.
> 별도의 settings 수정은 불필요하다 (디렉토리만 생성하면 자동 인식).

---

## 3. 아키텍처

### Auto-Configuration 라이브러리 구조

```
spring-boot3/hibernate-lettuce/  (spring-boot4도 동일 구조)
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/
    │   │   ├── LettuceNearCacheSpringProperties.kt          # @ConfigurationProperties 바인딩
    │   │   ├── LettuceNearCacheHibernateAutoConfiguration.kt # HibernatePropertiesCustomizer 등록
    │   │   ├── LettuceNearCacheMetricsAutoConfiguration.kt   # Micrometer MetricsBinder 등록
    │   │   ├── LettuceNearCacheMetricsBinder.kt              # Gauge 메트릭 (SmartInitializingSingleton)
    │   │   ├── LettuceNearCacheActuatorAutoConfiguration.kt  # Actuator Endpoint 등록
    │   │   └── LettuceNearCacheActuatorEndpoint.kt           # GET /actuator/nearcache
    │   └── resources/META-INF/spring/
    │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── test/
        └── kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/
            ├── LettuceNearCacheAutoConfigurationTest.kt      # ApplicationContextRunner 단위 테스트
            └── LettuceNearCacheIntegrationTest.kt            # Testcontainers Redis + H2 통합 테스트
```

### 데모 앱 구조

```
spring-boot3/hibernate-lettuce-demo/  (spring-boot4도 동일 구조)
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── kotlin/io/bluetape4k/examples/cache/lettuce/
    │   │   ├── DemoApplication.kt
    │   │   ├── domain/Product.kt                # @Entity @Cacheable @Cache
    │   │   ├── repository/ProductRepository.kt  # JpaRepository
    │   │   ├── controller/ProductController.kt  # CRUD REST API
    │   │   └── controller/CacheController.kt    # 캐시 통계/무효화 API
    │   └── resources/application.yml
    └── test/
        └── kotlin/io/bluetape4k/examples/cache/lettuce/
            └── DemoApplicationTest.kt           # Testcontainers + RestClient 통합 테스트
```

---

## 4. Spring Boot 3 vs 4 차이점

### 4.1 HibernatePropertiesCustomizer 패키지 변경 (핵심 차이)

| 항목 | Spring Boot 3.5.x | Spring Boot 4.0.x |
|------|-------------------|-------------------|
| **패키지** | `org.springframework.boot.autoconfigure.orm.jpa` | `org.springframework.boot.hibernate.autoconfigure` |
| **의존성** | `spring-boot-autoconfigure` (이미 포함) | `spring-boot-hibernate` (별도 추가 필요) |

experimental 소스는 Spring Boot 4 패키지(`org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer`)를 사용한다.

**Spring Boot 3 모듈에서는 import를 변경해야 한다:**

```kotlin
// Spring Boot 3
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer

// Spring Boot 4
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
```

이 차이가 `LettuceNearCacheHibernateAutoConfiguration.kt`와 `LettuceNearCacheAutoConfigurationTest.kt` 두 파일에 영향을 준다.

### 4.2 BOM 의존성

| 항목 | Spring Boot 3 | Spring Boot 4 |
|------|--------------|--------------|
| **BOM** | 불필요 (Gradle 플러그인이 관리) | `implementation(platform(Libs.spring_boot4_dependencies))` 필수 |
| **주의** | - | `dependencyManagement { imports }` 사용 금지 (KGP 2.3 충돌) |

### 4.3 build.gradle.kts 의존성 차이

| 항목 | Spring Boot 3 | Spring Boot 4 |
|------|--------------|--------------|
| `HibernatePropertiesCustomizer` | `compileOnly(Libs.springBoot("autoconfigure"))` | `compileOnly(Libs.springBoot("hibernate"))` |
| `spring-boot-hibernate` | 불필요 (autoconfigure에 포함) | `compileOnly(Libs.springBoot("hibernate"))` |

### 4.4 Auto-Configuration 등록

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일은 Spring Boot 3과 4 모두 동일한 형식을 사용한다. 변경 불필요.

### 4.5 데모 앱 Spring Boot 플러그인

| 항목 | Spring Boot 3 | Spring Boot 4 |
|------|--------------|--------------|
| **플러그인** | `id(Plugins.spring_boot)` | `id(Plugins.spring_boot)` + BOM |
| **버전** | Gradle 플러그인 기본 버전 | Spring Boot 4 BOM으로 오버라이드 |

> 현재 `bluetape4k-projects`의 루트 `build.gradle.kts`에서 Spring Boot 플러그인 버전이 3.5.x로 설정되어 있을 가능성이 높다. Spring Boot 4 데모 앱에서 `bootJar`를 사용하려면 Spring Boot 4 플러그인 적용이 필요하다. 기존 `spring-boot4/exposed-jdbc-demo`가 `bootJar` 없이 라이브러리 스타일로 동작하는지 확인 필요.

---

## 5. 패키지 네임스페이스

### Auto-Configuration 라이브러리

**변경 없음** — `io.bluetape4k.spring.boot.autoconfigure.cache.lettuce` 유지

근거:
- Spring Boot 3과 4는 동일 패키지 네임스페이스를 사용 (기존 `spring-boot3/exposed-jdbc`와 `spring-boot4/exposed-jdbc` 패턴과 동일)
- 별도 JAR로 배포되므로 충돌 없음
- 사용자가 Spring Boot 3 → 4 마이그레이션 시 패키지 변경 최소화

### 데모 앱

**변경 없음** — `io.bluetape4k.examples.cache.lettuce` 유지

---

## 6. 의존성 설계

### 6.1 `bluetape4k-spring-boot3-hibernate-lettuce` (build.gradle.kts)

```kotlin
plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // 핵심: Hibernate 2nd Level Cache Lettuce 구현체
    api(project(":bluetape4k-hibernate-cache-lettuce"))

    // Spring Boot autoconfigure (HibernatePropertiesCustomizer 포함) — compileOnly (transitive 오염 방지)
    compileOnly(Libs.springBoot("autoconfigure"))

    // Optional 의존성 (사용자 프로젝트에서 선택적 활성화)
    compileOnly(Libs.springBootStarter("data-jpa"))
    compileOnly(Libs.hibernate_core)
    compileOnly(Libs.micrometer_core)
    compileOnly(Libs.springBootStarter("actuator"))

    // 직렬화/압축 런타임
    implementation(Libs.fory_kotlin)
    implementation(Libs.zstd_jni)

    // Test
    testImplementation(Libs.springBootStarter("test"))
    testImplementation(Libs.springBootStarter("data-jpa"))
    testImplementation(Libs.springBootStarter("actuator"))
    testImplementation(Libs.micrometer_core)
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)
}
```

**experimental과의 차이점:**
- `api(project(":hibernate-cache-lettuce"))` → `api(project(":bluetape4k-hibernate-cache-lettuce"))` (프로젝트 참조 변경)
- `api(Libs.bluetape4k_cache_lettuce)`, `api(Libs.bluetape4k_io)`, `api(Libs.bluetape4k_redis)` 제거 (이미 `bluetape4k-hibernate-cache-lettuce`가 transitively 포함)
- `configurations { testImplementation.get().extendsFrom(...) }` 패턴 추가

### 6.2 `bluetape4k-spring-boot4-hibernate-lettuce` (build.gradle.kts)

```kotlin
plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Spring Boot 4 BOM
    implementation(platform(Libs.spring_boot4_dependencies))

    // 핵심: Hibernate 2nd Level Cache Lettuce 구현체
    api(project(":bluetape4k-hibernate-cache-lettuce"))

    // Spring Boot 4: HibernatePropertiesCustomizer가 spring-boot-hibernate 모듈로 이동 — compileOnly
    compileOnly(Libs.springBoot("autoconfigure"))
    compileOnly(Libs.springBoot("hibernate"))

    // Optional 의존성
    compileOnly(Libs.springBootStarter("data-jpa"))
    compileOnly(Libs.hibernate_core)
    compileOnly(Libs.micrometer_core)
    compileOnly(Libs.springBootStarter("actuator"))

    // 직렬화/압축 런타임
    implementation(Libs.fory_kotlin)
    implementation(Libs.zstd_jni)

    // Test
    testImplementation(Libs.springBootStarter("test"))
    testImplementation(Libs.springBootStarter("data-jpa"))
    testImplementation(Libs.springBootStarter("actuator"))
    testImplementation(Libs.micrometer_core)
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)
}
```

### 6.3 `bluetape4k-spring-boot3-hibernate-lettuce-demo` (build.gradle.kts)

```kotlin
plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("plugin.allopen")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(project(":bluetape4k-spring-boot3-hibernate-lettuce"))
    implementation(Libs.springBootStarter("web"))
    implementation(Libs.springBootStarter("data-jpa"))
    implementation(Libs.springBootStarter("actuator"))
    implementation(Libs.micrometer_core)
    runtimeOnly(Libs.h2_v2)

    testImplementation(Libs.springBootStarter("test"))
    testImplementation(project(":bluetape4k-testcontainers"))
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
```

### 6.4 `bluetape4k-spring-boot4-hibernate-lettuce-demo` (build.gradle.kts)

```kotlin
plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("plugin.allopen")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Spring Boot 4 BOM
    implementation(platform(Libs.spring_boot4_dependencies))

    implementation(project(":bluetape4k-spring-boot4-hibernate-lettuce"))
    implementation(Libs.springBootStarter("web"))
    implementation(Libs.springBootStarter("data-jpa"))
    implementation(Libs.springBootStarter("actuator"))
    implementation(Libs.micrometer_core)
    runtimeOnly(Libs.h2_v2)

    testImplementation(Libs.springBootStarter("test"))
    testImplementation(project(":bluetape4k-testcontainers"))
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
```

---

## 7. settings.gradle.kts 변경

**변경 불필요.**

루트 `settings.gradle.kts`의 `includeModules("spring-boot3", withBaseDir = true)` 및 `includeModules("spring-boot4", withBaseDir = true)` 호출이 하위 디렉토리를 자동 스캔하므로, 디렉토리만 생성하면 자동 등록된다.

자동 생성되는 모듈명:
- `spring-boot3/hibernate-lettuce/` → `:bluetape4k-spring-boot3-hibernate-lettuce`
- `spring-boot3/hibernate-lettuce-demo/` → `:bluetape4k-spring-boot3-hibernate-lettuce-demo`
- `spring-boot4/hibernate-lettuce/` → `:bluetape4k-spring-boot4-hibernate-lettuce`
- `spring-boot4/hibernate-lettuce-demo/` → `:bluetape4k-spring-boot4-hibernate-lettuce-demo`

---

## 8. 테스트 전략

### 8.1 Auto-Configuration 라이브러리 테스트

| 테스트 파일 | 유형 | 설명 |
|------------|------|------|
| `LettuceNearCacheAutoConfigurationTest.kt` | 단위 | `ApplicationContextRunner` 기반, Redis 불필요 |
| `LettuceNearCacheIntegrationTest.kt` | 통합 | Testcontainers Redis + H2 인메모리 DB |

**Spring Boot 3 vs 4 테스트 차이:**
- `LettuceNearCacheAutoConfigurationTest.kt`: `HibernatePropertiesCustomizer` import 경로 변경
- `LettuceNearCacheIntegrationTest.kt`: 변경 없음 (Spring Boot 공통 API만 사용)

### 8.2 데모 앱 테스트

| 테스트 파일 | 유형 | 설명 |
|------------|------|------|
| `DemoApplicationTest.kt` | 통합 | `@SpringBootTest` + Testcontainers Redis + RestClient HTTP 호출 |

### 8.3 빌드 검증 명령

```bash
# Spring Boot 3 라이브러리
./gradlew :bluetape4k-spring-boot3-hibernate-lettuce:test

# Spring Boot 3 데모
./gradlew :bluetape4k-spring-boot3-hibernate-lettuce-demo:test

# Spring Boot 4 라이브러리
./gradlew :bluetape4k-spring-boot4-hibernate-lettuce:test

# Spring Boot 4 데모
./gradlew :bluetape4k-spring-boot4-hibernate-lettuce-demo:test
```

---

## 9. CLAUDE.md 업데이트

### Spring Boot 3 테이블에 추가

```markdown
| `hibernate-lettuce` (`bluetape4k-spring-boot3-hibernate-lettuce`) | Hibernate 2nd Level Cache Lettuce NearCache Auto-Configuration — Properties 바인딩, Micrometer Metrics, Actuator Endpoint |
| `hibernate-lettuce-demo` (`bluetape4k-spring-boot3-hibernate-lettuce-demo`) | Hibernate Lettuce NearCache + Spring MVC 통합 데모 |
```

### Spring Boot 4 테이블에 추가

```markdown
| `hibernate-lettuce` (`bluetape4k-spring-boot4-hibernate-lettuce`) | Hibernate 2nd Level Cache Lettuce NearCache Auto-Configuration — Properties 바인딩, Micrometer Metrics, Actuator Endpoint |
| `hibernate-lettuce-demo` (`bluetape4k-spring-boot4-hibernate-lettuce-demo`) | Hibernate Lettuce NearCache + Spring MVC 통합 데모 (Spring Boot 4 BOM) |
```

---

## 10. 단계별 구현 계획

### Phase 1: Spring Boot 3 라이브러리 (`hibernate-lettuce`)

1. `spring-boot3/hibernate-lettuce/` 디렉토리 생성
2. `build.gradle.kts` 작성 (섹션 6.1)
3. 소스 코드 복사 (6개 .kt 파일)
4. **`LettuceNearCacheHibernateAutoConfiguration.kt`의 import 변경:**
   ```kotlin
   // AS-IS (Spring Boot 4 패키지)
   import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
   // TO-BE (Spring Boot 3 패키지)
   import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
   ```
5. `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 복사
6. 테스트 코드 복사 (2개 .kt 파일)
7. **`LettuceNearCacheAutoConfigurationTest.kt`의 import 변경** (동일한 패키지 변경)
8. `./gradlew :bluetape4k-spring-boot3-hibernate-lettuce:test` 실행

### Phase 2: Spring Boot 4 라이브러리 (`hibernate-lettuce`)

1. `spring-boot4/hibernate-lettuce/` 디렉토리 생성
2. `build.gradle.kts` 작성 (섹션 6.2) — `platform(Libs.spring_boot4_dependencies)` + `springBoot("hibernate")`
3. 소스 코드 복사 (6개 .kt 파일, **import 변경 없이** 그대로 사용)
4. `META-INF/spring/...imports` 복사
5. 테스트 코드 복사 (2개 .kt 파일, import 변경 없음)
6. `./gradlew :bluetape4k-spring-boot4-hibernate-lettuce:test` 실행

### Phase 3: Spring Boot 3 데모 (`hibernate-lettuce-demo`)

1. `spring-boot3/hibernate-lettuce-demo/` 디렉토리 생성
2. `build.gradle.kts` 작성 (섹션 6.3) — `spring_boot` 플러그인 + `bootJar`
3. 소스 코드 복사 (5개 .kt 파일 + `application.yml`)
4. 테스트 코드 복사 (1개 .kt 파일)
5. `./gradlew :bluetape4k-spring-boot3-hibernate-lettuce-demo:test` 실행

### Phase 4: Spring Boot 4 데모 (`hibernate-lettuce-demo`)

1. `spring-boot4/hibernate-lettuce-demo/` 디렉토리 생성
2. `build.gradle.kts` 작성 (섹션 6.4) — BOM 추가, `bootJar` 여부는 기존 패턴 참고
3. 소스 코드 복사 (5개 .kt 파일 + `application.yml`)
4. 테스트 코드 복사 (1개 .kt 파일)
5. `./gradlew :bluetape4k-spring-boot4-hibernate-lettuce-demo:test` 실행

### Phase 5: 마무리

1. 4개 모듈 README.md 작성 (한국어, 코드 예시 포함)
2. CLAUDE.md 업데이트 (섹션 9)
3. 전체 빌드 검증: `./gradlew build -x test` (컴파일 확인)
4. 전체 테스트 검증:
   ```bash
   ./gradlew :bluetape4k-spring-boot3-hibernate-lettuce:test
   ./gradlew :bluetape4k-spring-boot4-hibernate-lettuce:test
   ./gradlew :bluetape4k-spring-boot3-hibernate-lettuce-demo:test
   ./gradlew :bluetape4k-spring-boot4-hibernate-lettuce-demo:test
   ```

---

## 부록: 의존성 전이 관계

```
bluetape4k-spring-boot3-hibernate-lettuce
├── bluetape4k-hibernate-cache-lettuce (api)
│   ├── bluetape4k-cache-lettuce (api)      ← NearCache 추상화
│   ├── bluetape4k-io (api)                 ← BinarySerializers
│   ├── bluetape4k-lettuce (api)            ← LettuceBinaryCodec
│   └── hibernate-core (api)
├── spring-boot-autoconfigure (api)
├── fory-kotlin (implementation)
└── zstd-jni (implementation)

bluetape4k-spring-boot3-hibernate-lettuce-demo
├── bluetape4k-spring-boot3-hibernate-lettuce (implementation)
├── spring-boot-starter-web
├── spring-boot-starter-data-jpa
├── spring-boot-starter-actuator
└── h2 (runtimeOnly)
```

## 부록: 위험 요소 및 대응

| 위험 | 영향 | 대응 |
|------|------|------|
| Spring Boot 3에서 `HibernatePropertiesCustomizer` 패키지가 다름 | 컴파일 실패 | Phase 1에서 import 변경 (확인됨) |
| `bluetape4k-hibernate-cache-lettuce`의 transitive 의존성 누락 | 런타임 ClassNotFoundException | `configurations { testImplementation.extendsFrom(compileOnly) }` 패턴으로 테스트 시 검증 |
| Spring Boot 4 `bootJar` 플러그인 버전 불일치 | 데모 앱 빌드 실패 | 기존 `spring-boot4/exposed-jdbc-demo` 패턴 참고 (bootJar 미사용) |
| `Libs.bluetape4k_*` 상수가 `bluetape4k-projects`에 없음 | 빌드 실패 | `project(":bluetape4k-*")` 로컬 참조로 대체 (experimental에서만 외부 참조 사용) |
