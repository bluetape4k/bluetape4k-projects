# Hibernate Lettuce Spring Boot Auto-Configuration 이관 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `bluetape4k-experimental`의 Hibernate 2nd Level Cache Lettuce NearCache Spring Boot Auto-Configuration 라이브러리와 데모 앱을 `bluetape4k-projects`의 `spring-boot3/` 및 `spring-boot4/` 하위로 이관한다.

**Architecture:** experimental 소스(Spring Boot 4 기준)를 4개 신규 모듈로 복사한다. Spring Boot 3 모듈에서는 `HibernatePropertiesCustomizer` import 경로를 `org.springframework.boot.autoconfigure.orm.jpa`로 변경한다. Spring Boot 4 모듈에서는 `platform(Libs.spring_boot4_dependencies)` BOM과 `compileOnly(Libs.springBoot("hibernate"))` 의존성을 추가한다. 데모 앱은 `bootJar` 없는 라이브러리 스타일로 작성한다.

**Tech Stack:** Kotlin 2.3, Spring Boot 3.5.x / 4.0.x, Hibernate 6, Lettuce 6.8.2, Micrometer, JUnit 5, Testcontainers

---

## 파일 구조 (File Structure)

### 신규 모듈 1: `spring-boot3/hibernate-lettuce/` (라이브러리)

```
spring-boot3/hibernate-lettuce/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/
    │   │   ├── LettuceNearCacheSpringProperties.kt
    │   │   ├── LettuceNearCacheHibernateAutoConfiguration.kt  ← import 변경 필요
    │   │   ├── LettuceNearCacheMetricsAutoConfiguration.kt
    │   │   ├── LettuceNearCacheMetricsBinder.kt
    │   │   ├── LettuceNearCacheActuatorAutoConfiguration.kt
    │   │   └── LettuceNearCacheActuatorEndpoint.kt
    │   └── resources/META-INF/spring/
    │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── test/
        ├── kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/
        │   ├── LettuceNearCacheAutoConfigurationTest.kt       ← import 변경 필요
        │   └── LettuceNearCacheIntegrationTest.kt
        └── resources/
            ├── junit-platform.properties
            └── logback-test.xml
```

### 신규 모듈 2: `spring-boot4/hibernate-lettuce/` (라이브러리)

```
spring-boot4/hibernate-lettuce/
├── build.gradle.kts                                           ← BOM + springBoot("hibernate") 추가
└── src/                                                       ← experimental 소스 그대로 복사 (import 변경 없음)
    ├── main/
    │   ├── kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/
    │   │   ├── LettuceNearCacheSpringProperties.kt
    │   │   ├── LettuceNearCacheHibernateAutoConfiguration.kt
    │   │   ├── LettuceNearCacheMetricsAutoConfiguration.kt
    │   │   ├── LettuceNearCacheMetricsBinder.kt
    │   │   ├── LettuceNearCacheActuatorAutoConfiguration.kt
    │   │   └── LettuceNearCacheActuatorEndpoint.kt
    │   └── resources/META-INF/spring/
    │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── test/
        ├── kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/
        │   ├── LettuceNearCacheAutoConfigurationTest.kt
        │   └── LettuceNearCacheIntegrationTest.kt
        └── resources/
            ├── junit-platform.properties
            └── logback-test.xml
```

### 신규 모듈 3: `spring-boot3/hibernate-lettuce-demo/` (데모)

```
spring-boot3/hibernate-lettuce-demo/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── kotlin/io/bluetape4k/examples/cache/lettuce/
    │   │   ├── DemoApplication.kt
    │   │   ├── domain/Product.kt
    │   │   ├── repository/ProductRepository.kt
    │   │   ├── controller/ProductController.kt
    │   │   └── controller/CacheController.kt
    │   └── resources/application.yml
    └── test/
        └── kotlin/io/bluetape4k/examples/cache/lettuce/
            └── DemoApplicationTest.kt
```

### 신규 모듈 4: `spring-boot4/hibernate-lettuce-demo/` (데모)

```
spring-boot4/hibernate-lettuce-demo/
├── build.gradle.kts                                           ← BOM 추가
└── src/                                                       ← Boot 3 데모와 완전 동일 소스
    ├── main/
    │   ├── kotlin/io/bluetape4k/examples/cache/lettuce/
    │   │   ├── DemoApplication.kt
    │   │   ├── domain/Product.kt
    │   │   ├── repository/ProductRepository.kt
    │   │   ├── controller/ProductController.kt
    │   │   └── controller/CacheController.kt
    │   └── resources/application.yml
    └── test/
        └── kotlin/io/bluetape4k/examples/cache/lettuce/
            └── DemoApplicationTest.kt
```

---

## 소스 참조 경로

| 약칭 | 절대 경로 |
|------|----------|
| `EXP_LIB` | `/Users/debop/work/bluetape4k/bluetape4k-experimental/spring-boot/hibernate-lettuce/` |
| `EXP_DEMO` | `/Users/debop/work/bluetape4k/bluetape4k-experimental/examples/spring-boot-hibernate-lettuce-demo/` |
| `PROJ` | `/Users/debop/work/bluetape4k/bluetape4k-projects/` |
| `REF_BOOT3` | `/Users/debop/work/bluetape4k/bluetape4k-projects/spring-boot3/exposed-jdbc/build.gradle.kts` |
| `REF_BOOT4` | `/Users/debop/work/bluetape4k/bluetape4k-projects/spring-boot4/exposed-jdbc/build.gradle.kts` |
| `REF_DEMO3` | `/Users/debop/work/bluetape4k/bluetape4k-projects/spring-boot3/exposed-jdbc-demo/build.gradle.kts` |
| `REF_DEMO4` | `/Users/debop/work/bluetape4k/bluetape4k-projects/spring-boot4/exposed-jdbc-demo/build.gradle.kts` |

---

### Task 1: Spring Boot 3 라이브러리 -- 디렉토리 생성 및 build.gradle.kts 작성

**complexity: high** (핵심 의존성 설계, Spring Boot 3 전용 compileOnly scope 설정)

**Files:**
- Create: `spring-boot3/hibernate-lettuce/build.gradle.kts`

**선행 태스크:** 없음

- [ ] **Step 1: 디렉토리 구조 생성**

```bash
cd /Users/debop/work/bluetape4k/bluetape4k-projects
mkdir -p spring-boot3/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce
mkdir -p spring-boot3/hibernate-lettuce/src/main/resources/META-INF/spring
mkdir -p spring-boot3/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce
mkdir -p spring-boot3/hibernate-lettuce/src/test/resources
```

- [ ] **Step 2: build.gradle.kts 작성**

Create `spring-boot3/hibernate-lettuce/build.gradle.kts`:

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

주의:
- `api(project(":hibernate-cache-lettuce"))` (experimental) 대신 `api(project(":bluetape4k-hibernate-cache-lettuce"))` 사용
- `api(Libs.bluetape4k_cache_lettuce)`, `api(Libs.bluetape4k_io)`, `api(Libs.bluetape4k_redis)` 제거 -- `bluetape4k-hibernate-cache-lettuce`가 transitively 포함
- `configurations { testImplementation.get().extendsFrom(...) }` 패턴 필수 (REF_BOOT3 참고)
- Spring Boot 3에서는 `springBoot("hibernate")` 불필요 -- `HibernatePropertiesCustomizer`가 `spring-boot-autoconfigure`에 포함됨

- [ ] **Step 3: 커밋**

```bash
git add spring-boot3/hibernate-lettuce/build.gradle.kts
git commit -m "feat(spring-boot3): hibernate-lettuce 모듈 build.gradle.kts 추가"
```

---

### Task 2: Spring Boot 3 라이브러리 -- main 소스 코드 복사 및 import 변경

**complexity: high** (HibernatePropertiesCustomizer import 경로 변경이 핵심)

**Files:**
- Create: `spring-boot3/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheSpringProperties.kt`
- Create: `spring-boot3/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt`
- Create: `spring-boot3/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsAutoConfiguration.kt`
- Create: `spring-boot3/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsBinder.kt`
- Create: `spring-boot3/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorAutoConfiguration.kt`
- Create: `spring-boot3/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorEndpoint.kt`
- Create: `spring-boot3/hibernate-lettuce/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**선행 태스크:** Task 1

- [ ] **Step 1: 변경 없이 복사하는 4개 파일 복사**

```bash
EXP_LIB="/Users/debop/work/bluetape4k/bluetape4k-experimental/spring-boot/hibernate-lettuce"
BOOT3_LIB="/Users/debop/work/bluetape4k/bluetape4k-projects/spring-boot3/hibernate-lettuce"
SRC_DIR="src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce"

cp "$EXP_LIB/$SRC_DIR/LettuceNearCacheSpringProperties.kt" "$BOOT3_LIB/$SRC_DIR/"
cp "$EXP_LIB/$SRC_DIR/LettuceNearCacheMetricsAutoConfiguration.kt" "$BOOT3_LIB/$SRC_DIR/"
cp "$EXP_LIB/$SRC_DIR/LettuceNearCacheMetricsBinder.kt" "$BOOT3_LIB/$SRC_DIR/"
cp "$EXP_LIB/$SRC_DIR/LettuceNearCacheActuatorAutoConfiguration.kt" "$BOOT3_LIB/$SRC_DIR/"
cp "$EXP_LIB/$SRC_DIR/LettuceNearCacheActuatorEndpoint.kt" "$BOOT3_LIB/$SRC_DIR/"
```

- [ ] **Step 2: LettuceNearCacheHibernateAutoConfiguration.kt 복사 후 import 변경**

```bash
cp "$EXP_LIB/$SRC_DIR/LettuceNearCacheHibernateAutoConfiguration.kt" "$BOOT3_LIB/$SRC_DIR/"
```

`spring-boot3/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt` 에서 import를 변경한다:

```
AS-IS: import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
TO-BE: import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
```

변경 후 파일의 import 블록은 다음과 같아야 한다:

```kotlin
import io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
```

- [ ] **Step 3: AutoConfiguration.imports 파일 복사**

```bash
cp "$EXP_LIB/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports" \
   "$BOOT3_LIB/src/main/resources/META-INF/spring/"
```

파일 내용 (변경 없음):

```
io.bluetape4k.spring.boot.autoconfigure.cache.lettuce.LettuceNearCacheHibernateAutoConfiguration
io.bluetape4k.spring.boot.autoconfigure.cache.lettuce.LettuceNearCacheMetricsAutoConfiguration
io.bluetape4k.spring.boot.autoconfigure.cache.lettuce.LettuceNearCacheActuatorAutoConfiguration
```

- [ ] **Step 4: 커밋**

```bash
git add spring-boot3/hibernate-lettuce/src/main/
git commit -m "feat(spring-boot3): hibernate-lettuce main 소스 복사 (HibernatePropertiesCustomizer import Boot 3 패키지로 변경)"
```

---

### Task 3: Spring Boot 3 라이브러리 -- 테스트 코드 복사 및 import 변경

**complexity: medium** (테스트 import 변경 + 테스트 리소스 복사)

**Files:**
- Create: `spring-boot3/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt`
- Create: `spring-boot3/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheIntegrationTest.kt`
- Create: `spring-boot3/hibernate-lettuce/src/test/resources/junit-platform.properties`
- Create: `spring-boot3/hibernate-lettuce/src/test/resources/logback-test.xml`

**선행 태스크:** Task 2

- [ ] **Step 1: LettuceNearCacheIntegrationTest.kt 복사 (변경 없음)**

```bash
EXP_LIB="/Users/debop/work/bluetape4k/bluetape4k-experimental/spring-boot/hibernate-lettuce"
BOOT3_LIB="/Users/debop/work/bluetape4k/bluetape4k-projects/spring-boot3/hibernate-lettuce"
TEST_DIR="src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce"

cp "$EXP_LIB/$TEST_DIR/LettuceNearCacheIntegrationTest.kt" "$BOOT3_LIB/$TEST_DIR/"
```

- [ ] **Step 2: LettuceNearCacheAutoConfigurationTest.kt 복사 후 import 변경**

```bash
cp "$EXP_LIB/$TEST_DIR/LettuceNearCacheAutoConfigurationTest.kt" "$BOOT3_LIB/$TEST_DIR/"
```

`spring-boot3/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt` 에서 import를 변경한다:

```
AS-IS: import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
TO-BE: import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
```

- [ ] **Step 3: 테스트 리소스 파일 복사**

```bash
cp "$EXP_LIB/src/test/resources/junit-platform.properties" "$BOOT3_LIB/src/test/resources/"
cp "$EXP_LIB/src/test/resources/logback-test.xml" "$BOOT3_LIB/src/test/resources/"
```

`junit-platform.properties` 내용:

```properties
junit.jupiter.extensions.autodetection.enabled=true
junit.jupiter.testinstance.lifecycle.default=per_class

junit.jupiter.execution.parallel.enabled=false
junit.jupiter.execution.parallel.mode.default=same_thread
junit.jupiter.execution.parallel.mode.classes.default=concurrent
```

`logback-test.xml` 내용:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- @formatter:off -->
    <property name="CONSOLE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5p) %magenta(${PID:- }) --- [%25.25t] %cyan(%-40.40logger{39}) : %m%n"/>

    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>DEBUG</level>
        </filter>
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>utf8</charset>
        </encoder>
    </appender>
    <!-- formatter:on -->

    <logger name="io.bluetape4k.cache" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="console"/>
    </root>
</configuration>
```

- [ ] **Step 4: 커밋**

```bash
git add spring-boot3/hibernate-lettuce/src/test/
git commit -m "feat(spring-boot3): hibernate-lettuce 테스트 코드 복사 (Boot 3 import 변경 포함)"
```

---

### Task 4: Spring Boot 3 라이브러리 -- 빌드 및 테스트 검증

**complexity: medium** (컴파일 및 테스트 실행 검증)

**Files:**
- 변경 없음 (검증만)

**선행 태스크:** Task 3

- [ ] **Step 1: 컴파일 검증**

```bash
cd /Users/debop/work/bluetape4k/bluetape4k-projects
./gradlew :bluetape4k-spring-boot3-hibernate-lettuce:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 테스트 실행**

```bash
./gradlew :bluetape4k-spring-boot3-hibernate-lettuce:test
```

Expected: BUILD SUCCESSFUL, 모든 테스트 PASS

- [ ] **Step 3: 실패 시 디버그**

실패 원인별 대응:
- `HibernatePropertiesCustomizer` import 오류 → Step 2의 import 변경이 누락됨, Task 2 재확인
- `Unresolved reference: bluetape4k-hibernate-cache-lettuce` → 프로젝트 참조 확인 (`project(":bluetape4k-hibernate-cache-lettuce")`)
- Testcontainers Redis 연결 실패 → Docker 데몬 실행 여부 확인

---

### Task 5: Spring Boot 4 라이브러리 -- 디렉토리 생성 및 build.gradle.kts 작성

**complexity: high** (Spring Boot 4 BOM + `springBoot("hibernate")` 의존성 설계)

**Files:**
- Create: `spring-boot4/hibernate-lettuce/build.gradle.kts`

**선행 태스크:** 없음 (Task 1과 병렬 가능)

- [ ] **Step 1: 디렉토리 구조 생성**

```bash
cd /Users/debop/work/bluetape4k/bluetape4k-projects
mkdir -p spring-boot4/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce
mkdir -p spring-boot4/hibernate-lettuce/src/main/resources/META-INF/spring
mkdir -p spring-boot4/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce
mkdir -p spring-boot4/hibernate-lettuce/src/test/resources
```

- [ ] **Step 2: build.gradle.kts 작성**

Create `spring-boot4/hibernate-lettuce/build.gradle.kts`:

```kotlin
plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Spring Boot 4 BOM: platform()을 사용하면 compileClasspath/runtimeClasspath에만 적용되고
    // kotlinBuildToolsApiClasspath 같은 내부 Gradle 설정에는 영향을 주지 않음
    // (dependencyManagement 플러그인은 ALL configurations에 적용되어 kotlin-stdlib 버전 충돌 유발)
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

주의:
- `implementation(platform(Libs.spring_boot4_dependencies))` 필수 (REF_BOOT4 참고)
- `compileOnly(Libs.springBoot("hibernate"))` 추가 -- Boot 4에서 `HibernatePropertiesCustomizer`가 이 모듈로 이동됨
- `dependencyManagement { imports }` 사용 금지 -- KGP 2.3.x 충돌 발생

- [ ] **Step 3: 커밋**

```bash
git add spring-boot4/hibernate-lettuce/build.gradle.kts
git commit -m "feat(spring-boot4): hibernate-lettuce 모듈 build.gradle.kts 추가 (Boot 4 BOM)"
```

---

### Task 6: Spring Boot 4 라이브러리 -- 소스 코드 복사 (import 변경 없음)

**complexity: low** (experimental 소스를 그대로 복사, Spring Boot 4 패키지 import가 이미 맞음)

**Files:**
- Create: `spring-boot4/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/*.kt` (6개)
- Create: `spring-boot4/hibernate-lettuce/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `spring-boot4/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/*.kt` (2개)
- Create: `spring-boot4/hibernate-lettuce/src/test/resources/junit-platform.properties`
- Create: `spring-boot4/hibernate-lettuce/src/test/resources/logback-test.xml`

**선행 태스크:** Task 5

- [ ] **Step 1: main 소스 6개 파일 복사 (변경 없음)**

```bash
EXP_LIB="/Users/debop/work/bluetape4k/bluetape4k-experimental/spring-boot/hibernate-lettuce"
BOOT4_LIB="/Users/debop/work/bluetape4k/bluetape4k-projects/spring-boot4/hibernate-lettuce"
SRC_DIR="src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce"

cp "$EXP_LIB/$SRC_DIR/LettuceNearCacheSpringProperties.kt" "$BOOT4_LIB/$SRC_DIR/"
cp "$EXP_LIB/$SRC_DIR/LettuceNearCacheHibernateAutoConfiguration.kt" "$BOOT4_LIB/$SRC_DIR/"
cp "$EXP_LIB/$SRC_DIR/LettuceNearCacheMetricsAutoConfiguration.kt" "$BOOT4_LIB/$SRC_DIR/"
cp "$EXP_LIB/$SRC_DIR/LettuceNearCacheMetricsBinder.kt" "$BOOT4_LIB/$SRC_DIR/"
cp "$EXP_LIB/$SRC_DIR/LettuceNearCacheActuatorAutoConfiguration.kt" "$BOOT4_LIB/$SRC_DIR/"
cp "$EXP_LIB/$SRC_DIR/LettuceNearCacheActuatorEndpoint.kt" "$BOOT4_LIB/$SRC_DIR/"
```

import `org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer`는 Spring Boot 4 패키지이므로 변경 불필요.

- [ ] **Step 2: AutoConfiguration.imports 복사**

```bash
cp "$EXP_LIB/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports" \
   "$BOOT4_LIB/src/main/resources/META-INF/spring/"
```

- [ ] **Step 3: 테스트 코드 2개 파일 복사 (변경 없음)**

```bash
TEST_DIR="src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce"

cp "$EXP_LIB/$TEST_DIR/LettuceNearCacheAutoConfigurationTest.kt" "$BOOT4_LIB/$TEST_DIR/"
cp "$EXP_LIB/$TEST_DIR/LettuceNearCacheIntegrationTest.kt" "$BOOT4_LIB/$TEST_DIR/"
```

- [ ] **Step 4: 테스트 리소스 파일 복사**

```bash
cp "$EXP_LIB/src/test/resources/junit-platform.properties" "$BOOT4_LIB/src/test/resources/"
cp "$EXP_LIB/src/test/resources/logback-test.xml" "$BOOT4_LIB/src/test/resources/"
```

- [ ] **Step 5: 커밋**

```bash
git add spring-boot4/hibernate-lettuce/src/
git commit -m "feat(spring-boot4): hibernate-lettuce 소스 복사 (experimental 그대로, import 변경 없음)"
```

---

### Task 7: Spring Boot 4 라이브러리 -- 빌드 및 테스트 검증

**complexity: medium** (컴파일 및 테스트 실행 검증)

**Files:**
- 변경 없음 (검증만)

**선행 태스크:** Task 6

- [ ] **Step 1: 컴파일 검증**

```bash
cd /Users/debop/work/bluetape4k/bluetape4k-projects
./gradlew :bluetape4k-spring-boot4-hibernate-lettuce:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 테스트 실행**

```bash
./gradlew :bluetape4k-spring-boot4-hibernate-lettuce:test
```

Expected: BUILD SUCCESSFUL, 모든 테스트 PASS

- [ ] **Step 3: 실패 시 디버그**

실패 원인별 대응:
- `HibernatePropertiesCustomizer` 클래스 못 찾음 → `compileOnly(Libs.springBoot("hibernate"))` 누락 확인
- BOM 관련 버전 충돌 → `implementation(platform(...))` 사용 여부 확인
- Testcontainers Redis 연결 실패 → Docker 데몬 실행 여부 확인

---

### Task 8: Spring Boot 3 데모 -- 디렉토리 생성, build.gradle.kts 작성, 소스 복사

**complexity: medium** (표준 데모 앱 패턴, bootJar 없는 라이브러리 스타일)

**Files:**
- Create: `spring-boot3/hibernate-lettuce-demo/build.gradle.kts`
- Create: `spring-boot3/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/DemoApplication.kt`
- Create: `spring-boot3/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/domain/Product.kt`
- Create: `spring-boot3/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/repository/ProductRepository.kt`
- Create: `spring-boot3/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/controller/ProductController.kt`
- Create: `spring-boot3/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/controller/CacheController.kt`
- Create: `spring-boot3/hibernate-lettuce-demo/src/main/resources/application.yml`
- Create: `spring-boot3/hibernate-lettuce-demo/src/test/kotlin/io/bluetape4k/examples/cache/lettuce/DemoApplicationTest.kt`

**선행 태스크:** Task 4 (Spring Boot 3 라이브러리 테스트 통과 후)

- [ ] **Step 1: 디렉토리 구조 생성**

```bash
cd /Users/debop/work/bluetape4k/bluetape4k-projects
mkdir -p spring-boot3/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/domain
mkdir -p spring-boot3/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/repository
mkdir -p spring-boot3/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/controller
mkdir -p spring-boot3/hibernate-lettuce-demo/src/main/resources
mkdir -p spring-boot3/hibernate-lettuce-demo/src/test/kotlin/io/bluetape4k/examples/cache/lettuce
```

- [ ] **Step 2: build.gradle.kts 작성**

Create `spring-boot3/hibernate-lettuce-demo/build.gradle.kts`:

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

주의:
- `bootJar` 미사용 -- 라이브러리 스타일 (기존 `exposed-jdbc-demo` 패턴 준수)
- `implementation(project(":spring-boot-hibernate-lettuce"))` (experimental) 대신 `implementation(project(":bluetape4k-spring-boot3-hibernate-lettuce"))` 사용
- `Libs.bluetape4k_testcontainers` 대신 `project(":bluetape4k-testcontainers")` 사용

- [ ] **Step 3: 소스 코드 5개 + application.yml 복사 (변경 없음)**

```bash
EXP_DEMO="/Users/debop/work/bluetape4k/bluetape4k-experimental/examples/spring-boot-hibernate-lettuce-demo"
BOOT3_DEMO="/Users/debop/work/bluetape4k/bluetape4k-projects/spring-boot3/hibernate-lettuce-demo"
MAIN_DIR="src/main/kotlin/io/bluetape4k/examples/cache/lettuce"

cp "$EXP_DEMO/$MAIN_DIR/DemoApplication.kt" "$BOOT3_DEMO/$MAIN_DIR/"
cp "$EXP_DEMO/$MAIN_DIR/domain/Product.kt" "$BOOT3_DEMO/$MAIN_DIR/domain/"
cp "$EXP_DEMO/$MAIN_DIR/repository/ProductRepository.kt" "$BOOT3_DEMO/$MAIN_DIR/repository/"
cp "$EXP_DEMO/$MAIN_DIR/controller/ProductController.kt" "$BOOT3_DEMO/$MAIN_DIR/controller/"
cp "$EXP_DEMO/$MAIN_DIR/controller/CacheController.kt" "$BOOT3_DEMO/$MAIN_DIR/controller/"
cp "$EXP_DEMO/src/main/resources/application.yml" "$BOOT3_DEMO/src/main/resources/"
```

모든 파일은 변경 없이 복사한다.

- [ ] **Step 4: 테스트 코드 복사 (변경 없음)**

```bash
TEST_DIR="src/test/kotlin/io/bluetape4k/examples/cache/lettuce"
cp "$EXP_DEMO/$TEST_DIR/DemoApplicationTest.kt" "$BOOT3_DEMO/$TEST_DIR/"
```

- [ ] **Step 5: 커밋**

```bash
git add spring-boot3/hibernate-lettuce-demo/
git commit -m "feat(spring-boot3): hibernate-lettuce-demo 데모 앱 추가"
```

---

### Task 9: Spring Boot 3 데모 -- 빌드 및 테스트 검증

**complexity: medium** (통합 테스트 -- Testcontainers Redis + H2 + Spring Boot)

**Files:**
- 변경 없음 (검증만)

**선행 태스크:** Task 8

- [ ] **Step 1: 컴파일 검증**

```bash
cd /Users/debop/work/bluetape4k/bluetape4k-projects
./gradlew :bluetape4k-spring-boot3-hibernate-lettuce-demo:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 테스트 실행**

```bash
./gradlew :bluetape4k-spring-boot3-hibernate-lettuce-demo:test
```

Expected: BUILD SUCCESSFUL, 모든 테스트 PASS (7개 테스트)

- [ ] **Step 3: 실패 시 디버그**

실패 원인별 대응:
- `DemoApplication` 컨텍스트 로드 실패 → `@SpringBootApplication`이 auto-configuration을 찾지 못함, `spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 경로 확인
- `RestClient` 테스트 실패 → `webEnvironment = RANDOM_PORT` 설정 확인
- `allOpen` 관련 JPA 프록시 문제 → `kotlin("plugin.allopen")` 및 `allOpen` 블록 확인

---

### Task 10: Spring Boot 4 데모 -- 디렉토리 생성, build.gradle.kts 작성, 소스 복사

**complexity: medium** (Spring Boot 4 BOM 추가, 나머지는 Boot 3 데모와 동일)

**Files:**
- Create: `spring-boot4/hibernate-lettuce-demo/build.gradle.kts`
- Create: `spring-boot4/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/DemoApplication.kt`
- Create: `spring-boot4/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/domain/Product.kt`
- Create: `spring-boot4/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/repository/ProductRepository.kt`
- Create: `spring-boot4/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/controller/ProductController.kt`
- Create: `spring-boot4/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/controller/CacheController.kt`
- Create: `spring-boot4/hibernate-lettuce-demo/src/main/resources/application.yml`
- Create: `spring-boot4/hibernate-lettuce-demo/src/test/kotlin/io/bluetape4k/examples/cache/lettuce/DemoApplicationTest.kt`

**선행 태스크:** Task 7 (Spring Boot 4 라이브러리 테스트 통과 후)

- [ ] **Step 1: 디렉토리 구조 생성**

```bash
cd /Users/debop/work/bluetape4k/bluetape4k-projects
mkdir -p spring-boot4/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/domain
mkdir -p spring-boot4/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/repository
mkdir -p spring-boot4/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce/controller
mkdir -p spring-boot4/hibernate-lettuce-demo/src/main/resources
mkdir -p spring-boot4/hibernate-lettuce-demo/src/test/kotlin/io/bluetape4k/examples/cache/lettuce
```

- [ ] **Step 2: build.gradle.kts 작성**

Create `spring-boot4/hibernate-lettuce-demo/build.gradle.kts`:

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
    // Spring Boot 4 BOM: platform() 방식 필수 (dependencyManagement 사용 금지 - KGP 2.3 충돌)
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

주의:
- `implementation(platform(Libs.spring_boot4_dependencies))` 필수 (REF_DEMO4 패턴)
- `project(":bluetape4k-spring-boot4-hibernate-lettuce")` 참조
- `bootJar` 미사용 (라이브러리 스타일)

- [ ] **Step 3: 소스 코드 복사 (Boot 3 데모와 완전 동일)**

```bash
EXP_DEMO="/Users/debop/work/bluetape4k/bluetape4k-experimental/examples/spring-boot-hibernate-lettuce-demo"
BOOT4_DEMO="/Users/debop/work/bluetape4k/bluetape4k-projects/spring-boot4/hibernate-lettuce-demo"
MAIN_DIR="src/main/kotlin/io/bluetape4k/examples/cache/lettuce"

cp "$EXP_DEMO/$MAIN_DIR/DemoApplication.kt" "$BOOT4_DEMO/$MAIN_DIR/"
cp "$EXP_DEMO/$MAIN_DIR/domain/Product.kt" "$BOOT4_DEMO/$MAIN_DIR/domain/"
cp "$EXP_DEMO/$MAIN_DIR/repository/ProductRepository.kt" "$BOOT4_DEMO/$MAIN_DIR/repository/"
cp "$EXP_DEMO/$MAIN_DIR/controller/ProductController.kt" "$BOOT4_DEMO/$MAIN_DIR/controller/"
cp "$EXP_DEMO/$MAIN_DIR/controller/CacheController.kt" "$BOOT4_DEMO/$MAIN_DIR/controller/"
cp "$EXP_DEMO/src/main/resources/application.yml" "$BOOT4_DEMO/src/main/resources/"
```

- [ ] **Step 4: 테스트 코드 복사 (변경 없음)**

```bash
TEST_DIR="src/test/kotlin/io/bluetape4k/examples/cache/lettuce"
cp "$EXP_DEMO/$TEST_DIR/DemoApplicationTest.kt" "$BOOT4_DEMO/$TEST_DIR/"
```

- [ ] **Step 5: 커밋**

```bash
git add spring-boot4/hibernate-lettuce-demo/
git commit -m "feat(spring-boot4): hibernate-lettuce-demo 데모 앱 추가 (Spring Boot 4 BOM)"
```

---

### Task 11: Spring Boot 4 데모 -- 빌드 및 테스트 검증

**complexity: medium** (통합 테스트 검증)

**Files:**
- 변경 없음 (검증만)

**선행 태스크:** Task 10

- [ ] **Step 1: 컴파일 검증**

```bash
cd /Users/debop/work/bluetape4k/bluetape4k-projects
./gradlew :bluetape4k-spring-boot4-hibernate-lettuce-demo:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 테스트 실행**

```bash
./gradlew :bluetape4k-spring-boot4-hibernate-lettuce-demo:test
```

Expected: BUILD SUCCESSFUL, 모든 테스트 PASS

- [ ] **Step 3: 실패 시 디버그**

실패 원인별 대응:
- Spring Boot 4 auto-configuration 로드 실패 → Boot 4 BOM 버전 확인, `spring-boot-hibernate` transitive 포함 확인
- `RestClient` API 호환성 문제 → Spring Boot 4에서 `RestClient` API 변경 여부 확인 (Spring 7 기반)

---

### Task 12: 4개 모듈 README.md 작성

**complexity: low** (문서 작성, 한국어)

**Files:**
- Create: `spring-boot3/hibernate-lettuce/README.md`
- Create: `spring-boot4/hibernate-lettuce/README.md`
- Create: `spring-boot3/hibernate-lettuce-demo/README.md`
- Create: `spring-boot4/hibernate-lettuce-demo/README.md`

**선행 태스크:** Task 9, Task 11 (모든 테스트 통과 후)

각 README.md는 한국어로 작성하며, 다음 내용을 포함한다:
- 모듈 개요 및 주요 기능
- 의존성 (build.gradle.kts 예시)
- 설정 방법 (application.yml 예시)
- 사용 예시 (코드 스니펫)
- Spring Boot 3 vs 4 차이점 (라이브러리 모듈)
- REST API 엔드포인트 목록 (데모 모듈)

참조: experimental README
- `bluetape4k-experimental/spring-boot/hibernate-lettuce/README.md`
- `bluetape4k-experimental/examples/spring-boot-hibernate-lettuce-demo/README.md`

---

### Task 12-B: bluetape4k-patterns 체크리스트 검토

**complexity: low** (패턴 검토)

**Files:** 4개 모듈의 모든 `.kt` 파일

**선행 태스크:** Task 12

- [ ] **Step 1: 모든 public 클래스/인터페이스에 KDoc 존재 확인 (한국어)**
- [ ] **Step 2: Kotlin 관용 코드 패턴 확인** (Java 스타일 회피)
- [ ] **Step 3: `configurations { testImplementation.extendsFrom(compileOnly) }` 라이브러리 모듈에 존재 확인**
- [ ] **Step 4: `compileOnly` scope 올바른지 확인** (`spring-boot-autoconfigure` 등)

---

### Task 13: CLAUDE.md 업데이트

**complexity: low** (문서 업데이트)

**Files:**
- Modify: `/Users/debop/work/bluetape4k/bluetape4k-projects/CLAUDE.md`

**선행 태스크:** Task 12-B

- [ ] **Step 1: Spring Boot 3 섹션에 모듈 추가**

`CLAUDE.md`의 `### Spring Boot 3 (`spring-boot3/`)` 테이블에 다음 2행을 추가한다:

```markdown
| `hibernate-lettuce` (`bluetape4k-spring-boot3-hibernate-lettuce`) | Hibernate 2nd Level Cache Lettuce NearCache Auto-Configuration -- Properties 바인딩, Micrometer Metrics, Actuator Endpoint |
| `hibernate-lettuce-demo` (`bluetape4k-spring-boot3-hibernate-lettuce-demo`) | Hibernate Lettuce NearCache + Spring MVC 통합 데모 |
```

- [ ] **Step 2: Spring Boot 4 섹션에 모듈 추가**

`CLAUDE.md`의 `### Spring Boot 4 (`spring-boot4/`)` 테이블에 다음 2행을 추가한다:

```markdown
| `hibernate-lettuce` (`bluetape4k-spring-boot4-hibernate-lettuce`) | Hibernate 2nd Level Cache Lettuce NearCache Auto-Configuration -- Properties 바인딩, Micrometer Metrics, Actuator Endpoint |
| `hibernate-lettuce-demo` (`bluetape4k-spring-boot4-hibernate-lettuce-demo`) | Hibernate Lettuce NearCache + Spring MVC 통합 데모 (Spring Boot 4 BOM) |
```

- [ ] **Step 3: 커밋**

```bash
git add CLAUDE.md
git commit -m "docs: CLAUDE.md에 hibernate-lettuce 모듈 4개 추가"
```

---

### Task 14: 전체 빌드 검증 및 최종 커밋

**complexity: low** (최종 검증)

**Files:**
- 변경 없음 (검증만)

**선행 태스크:** Task 13

- [ ] **Step 1: 전체 컴파일 검증**

```bash
cd /Users/debop/work/bluetape4k/bluetape4k-projects
./gradlew build -x test 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 4개 모듈 테스트 일괄 실행**

```bash
./gradlew \
  :bluetape4k-spring-boot3-hibernate-lettuce:test \
  :bluetape4k-spring-boot4-hibernate-lettuce:test \
  :bluetape4k-spring-boot3-hibernate-lettuce-demo:test \
  :bluetape4k-spring-boot4-hibernate-lettuce-demo:test
```

Expected: BUILD SUCCESSFUL, 모든 테스트 PASS

- [ ] **Step 3: 생성된 모듈 확인**

```bash
ls -la spring-boot3/hibernate-lettuce/build.gradle.kts
ls -la spring-boot4/hibernate-lettuce/build.gradle.kts
ls -la spring-boot3/hibernate-lettuce-demo/build.gradle.kts
ls -la spring-boot4/hibernate-lettuce-demo/build.gradle.kts
```

Expected: 4개 파일 모두 존재

---

## 태스크 의존성 그래프

```
Task 1 ──→ Task 2 ──→ Task 3 ──→ Task 4 ──→ Task 8 ──→ Task 9 ──┐
                                                                   ├──→ Task 12 ──→ Task 12-B ──→ Task 13 ──→ Task 14
Task 5 ──→ Task 6 ──→ Task 7 ──→ Task 10 ──→ Task 11 ────────────┘
```

병렬 실행 가능: Task 1~4 (Boot 3 라이브러리) || Task 5~7 (Boot 4 라이브러리)

## 태스크 요약

| # | 설명 | 복잡도 | 선행 |
|---|------|--------|------|
| 1 | Boot 3 라이브러리 -- build.gradle.kts | high | -- |
| 2 | Boot 3 라이브러리 -- main 소스 복사 + import 변경 | high | 1 |
| 3 | Boot 3 라이브러리 -- 테스트 코드 복사 + import 변경 | medium | 2 |
| 4 | Boot 3 라이브러리 -- 빌드/테스트 검증 | medium | 3 |
| 5 | Boot 4 라이브러리 -- build.gradle.kts (BOM) | high | -- |
| 6 | Boot 4 라이브러리 -- 소스 복사 (변경 없음) | low | 5 |
| 7 | Boot 4 라이브러리 -- 빌드/테스트 검증 | medium | 6 |
| 8 | Boot 3 데모 -- build.gradle.kts + 소스 복사 | medium | 4 |
| 9 | Boot 3 데모 -- 빌드/테스트 검증 | medium | 8 |
| 10 | Boot 4 데모 -- build.gradle.kts + 소스 복사 (BOM) | medium | 7 |
| 11 | Boot 4 데모 -- 빌드/테스트 검증 | medium | 10 |
| 12 | README.md 4개 모듈 작성 | low | 9, 11 |
| 12-B | bluetape4k-patterns 체크리스트 검토 | low | 12 |
| 13 | CLAUDE.md 업데이트 | low | 12-B |
| 14 | 전체 빌드 검증 | low | 13 |
