# Module Consolidation Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** bluetape4k 프로젝트의 모듈 수를 ~117개에서 ~82개로 줄여 사용자의 의존성 선언 부담과 유지보수 비용을 낮춘다.

**Architecture:** 각 통합 대상 영역의 서브 모듈 소스를 상위(또는 신규) 모듈로 이동하고, 선택적 의존성은 `compileOnly`로 전환한다. 패키지 계층은 변경하지 않는다.

**Tech Stack:** Kotlin 2.3, Gradle (Kotlin DSL), `includeModules()` 자동 감지 구조

---

## 사전 지식

### `includeModules()` 동작 방식

`settings.gradle.kts`의 `includeModules(baseDir)` 함수는 `baseDir`의 **서브 디렉토리 하나당 모듈 하나**를 등록한다.

- `includeModules("io", withBaseDir = false)` → `io/jackson` = `bluetape4k-jackson`, `io/jackson-binary` = `bluetape4k-jackson-binary` …
- 서브 디렉토리를 삭제하면 **자동으로** 모듈 등록이 사라진다 (settings.gradle.kts 수정 불필요)

단, `aws/`, `aws-kotlin/`, `vertx/`처럼 **디렉토리 자체**를 단일 모듈로 만들 때는 settings.gradle.kts 수정이 필요하다:
```kotlin
// 기존
includeModules("aws", withBaseDir = true)

// 변경 후
include("bluetape4k-aws")
project(":bluetape4k-aws").projectDir = file("aws")
```

### 공통 작업 패턴 (각 청크 반복)

1. 타겟 모듈 `build.gradle.kts`에 소스 모듈의 의존성 추가 (`compileOnly`)
2. `cp -r` 로 소스/테스트 파일 이동 (패키지 경로 그대로 유지)
3. 소스 모듈 디렉토리 삭제
4. 참조 업데이트: 다른 모듈의 `build.gradle.kts`에서 구 모듈명 → 신 모듈명
5. `./gradlew :<new-module>:test` 실행 확인
6. `git commit`

---

## Chunk 1: io/jackson + io/jackson3 통합

**목표:** `jackson-binary`, `jackson-text` → `jackson` 흡수 (패키지 유지)
**위험도:** 낮음 — 외부 참조 없음, 내부 compileOnly 패턴 이미 적용됨

### Task 1-1: jackson-binary → jackson 통합

**Files:**
- Modify: `io/jackson/build.gradle.kts`
- Move: `io/jackson-binary/src/main/kotlin/` → `io/jackson/src/main/kotlin/`
- Move: `io/jackson-binary/src/test/kotlin/` → `io/jackson/src/test/kotlin/`
- Delete: `io/jackson-binary/`

- [ ] **Step 1: build.gradle.kts에 binary 의존성 추가**

`io/jackson/build.gradle.kts`의 `dependencies { }` 블록에 추가:
```kotlin
// Jackson Dataformats Binary
compileOnly(Libs.jackson_dataformat_avro)
compileOnly(Libs.jackson_dataformat_cbor)
compileOnly(Libs.jackson_dataformat_ion)
compileOnly(Libs.jackson_dataformat_protobuf)
compileOnly(Libs.jackson_dataformat_smile)
```

- [ ] **Step 2: 소스 파일 이동**

```bash
cp -r io/jackson-binary/src/main/kotlin/io/bluetape4k/jackson/binary \
      io/jackson/src/main/kotlin/io/bluetape4k/jackson/
cp -r io/jackson-binary/src/test/kotlin/io/bluetape4k/jackson/binary \
      io/jackson/src/test/kotlin/io/bluetape4k/jackson/
```

- [ ] **Step 3: 소스 모듈 삭제**

```bash
rm -rf io/jackson-binary
```

- [ ] **Step 4: 테스트 실행**

```bash
./gradlew :bluetape4k-jackson:test
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "refactor: jackson-binary → bluetape4k-jackson 통합"
```

---

### Task 1-2: jackson-text → jackson 통합

**Files:**
- Modify: `io/jackson/build.gradle.kts`
- Move: `io/jackson-text/src/` → `io/jackson/src/`
- Delete: `io/jackson-text/`

- [ ] **Step 1: build.gradle.kts에 text 의존성 추가**

(yaml/properties는 이미 있으므로 추가 항목만):
```kotlin
// Jackson Dataformats Text
compileOnly(Libs.jackson_dataformat_csv)
compileOnly(Libs.jackson_dataformat_toml)
```

- [ ] **Step 2: 소스 파일 이동**

```bash
cp -r io/jackson-text/src/main/kotlin/io/bluetape4k/jackson/text \
      io/jackson/src/main/kotlin/io/bluetape4k/jackson/
cp -r io/jackson-text/src/test/kotlin/io/bluetape4k/jackson/text \
      io/jackson/src/test/kotlin/io/bluetape4k/jackson/
```

- [ ] **Step 3: 소스 모듈 삭제**

```bash
rm -rf io/jackson-text
```

- [ ] **Step 4: 테스트 실행**

```bash
./gradlew :bluetape4k-jackson:test
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "refactor: jackson-text → bluetape4k-jackson 통합"
```

---

### Task 1-3: jackson3-binary + jackson3-text → jackson3 통합

Task 1-1, 1-2와 동일한 패턴. `jackson` → `jackson3`, `jackson/binary` → `jackson3/binary` 로 경로만 변경.

- [ ] **Step 1: jackson3 build.gradle.kts에 binary + text 의존성 추가**

`io/jackson3/build.gradle.kts`에 추가:
```kotlin
// Jackson3 Dataformats Binary
compileOnly(Libs.jackson3_dataformat_avro)
compileOnly(Libs.jackson3_dataformat_cbor)
compileOnly(Libs.jackson3_dataformat_ion)
compileOnly(Libs.jackson3_dataformat_protobuf)
compileOnly(Libs.jackson3_dataformat_smile)
// Jackson3 Dataformats Text
compileOnly(Libs.jackson3_dataformat_csv)
compileOnly(Libs.jackson3_dataformat_toml)
```

> **주의:** Libs.kt에서 jackson3 관련 상수명 확인 필요 (`jackson_dataformat_*` vs `jackson3_dataformat_*`)

- [ ] **Step 2: 소스 파일 이동**

```bash
cp -r io/jackson3-binary/src/main/kotlin/io/bluetape4k/jackson3/binary \
      io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/
cp -r io/jackson3-binary/src/test/kotlin/io/bluetape4k/jackson3/binary \
      io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/
cp -r io/jackson3-text/src/main/kotlin/io/bluetape4k/jackson3/text \
      io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/
cp -r io/jackson3-text/src/test/kotlin/io/bluetape4k/jackson3/text \
      io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/
```

- [ ] **Step 3: 소스 모듈 삭제**

```bash
rm -rf io/jackson3-binary io/jackson3-text
```

- [ ] **Step 4: 테스트 실행**

```bash
./gradlew :bluetape4k-jackson3:test
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "refactor: jackson3-binary/text → bluetape4k-jackson3 통합"
```

---

## Chunk 2: utils/geo 통합

**목표:** `geocode` + `geohash` + `geoip2` → `geo` (신규 디렉토리)
**위험도:** 낮음 — 외부 참조 없음

### Task 2-1: utils/geo 신규 모듈 생성 및 소스 이동

**Files:**
- Create: `utils/geo/build.gradle.kts`
- Create: `utils/geo/src/main/kotlin/io/bluetape4k/geo/geocode/` (from geocode)
- Create: `utils/geo/src/main/kotlin/io/bluetape4k/geo/geohash/` (from geohash)
- Create: `utils/geo/src/main/kotlin/io/bluetape4k/geo/geoip2/` (from geoip2)
- Delete: `utils/geocode/`, `utils/geohash/`, `utils/geoip2/`

- [ ] **Step 1: utils/geo 디렉토리 및 build.gradle.kts 생성**

```bash
mkdir -p utils/geo/src/main/kotlin/io/bluetape4k/geo
mkdir -p utils/geo/src/test/kotlin/io/bluetape4k/geo
mkdir -p utils/geo/src/test/resources
```

`utils/geo/build.gradle.kts` 내용:
```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    // geocode (Google Maps, Bing)
    compileOnly(project(":bluetape4k-jackson"))
    compileOnly(project(":bluetape4k-resilience4j"))
    compileOnly(project(":bluetape4k-feign"))
    compileOnly(Libs.feign_core)
    compileOnly(Libs.feign_kotlin)
    compileOnly(Libs.feign_slf4j)
    compileOnly(Libs.feign_jackson)
    compileOnly("com.google.maps:google-maps-services:2.2.0")
    compileOnly(Libs.httpclient5)
    compileOnly(Libs.httpclient5_cache)

    // geoip2 (MaxMind)
    compileOnly("com.maxmind.geoip2:geoip2:5.0.2")

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
```

> **주의:** geohash 모듈은 순수 알고리즘이라 외부 라이브러리 없음. geocode는 현재 `api()`였으나 `compileOnly()`로 전환.

- [ ] **Step 2: 소스 파일 이동**

```bash
# geocode 패키지 이동 (패키지명 변경: bluetape4k.geocode → bluetape4k.geo.geocode)
cp -r utils/geocode/src/main/kotlin/io/bluetape4k/geocode \
      utils/geo/src/main/kotlin/io/bluetape4k/geo/
cp -r utils/geocode/src/test/kotlin/io/bluetape4k/geocode \
      utils/geo/src/test/kotlin/io/bluetape4k/geo/

# geohash 이동
cp -r utils/geohash/src/main/kotlin/io/bluetape4k/geohash \
      utils/geo/src/main/kotlin/io/bluetape4k/geo/
cp -r utils/geohash/src/test/kotlin/io/bluetape4k/geohash \
      utils/geo/src/test/kotlin/io/bluetape4k/geo/

# geoip2 이동
cp -r utils/geoip2/src/main/kotlin/io/bluetape4k/geoip2 \
      utils/geo/src/main/kotlin/io/bluetape4k/geo/
cp -r utils/geoip2/src/test/kotlin/io/bluetape4k/geoip2 \
      utils/geo/src/test/kotlin/io/bluetape4k/geo/
```

> **주의:** 패키지 선언 변경 필요:
> - `package io.bluetape4k.geocode` → `package io.bluetape4k.geo.geocode`
> - `package io.bluetape4k.geohash` → `package io.bluetape4k.geo.geohash`
> - `package io.bluetape4k.geoip2` → `package io.bluetape4k.geo.geoip2`
>
> 각 파일의 `package` 선언 및 `import` 구문을 일괄 변경한다:
> ```bash
> find utils/geo -name "*.kt" -exec sed -i '' \
>   's/package io\.bluetape4k\.geocode/package io.bluetape4k.geo.geocode/g;
>    s/package io\.bluetape4k\.geohash/package io.bluetape4k.geo.geohash/g;
>    s/package io\.bluetape4k\.geoip2/package io.bluetape4k.geo.geoip2/g;
>    s/import io\.bluetape4k\.geocode\./import io.bluetape4k.geo.geocode./g;
>    s/import io\.bluetape4k\.geohash\./import io.bluetape4k.geo.geohash./g;
>    s/import io\.bluetape4k\.geoip2\./import io.bluetape4k.geo.geoip2./g' {} \;
> ```

- [ ] **Step 3: test/resources 복사 (필요 시)**

```bash
for dir in geocode geohash geoip2; do
  if [ -d "utils/$dir/src/test/resources" ]; then
    cp -r utils/$dir/src/test/resources/* utils/geo/src/test/resources/
  fi
done
```

- [ ] **Step 4: 소스 모듈 삭제**

```bash
rm -rf utils/geocode utils/geohash utils/geoip2
```

- [ ] **Step 5: 빌드 확인**

```bash
./gradlew :bluetape4k-geo:test
```
Expected: BUILD SUCCESSFUL (외부 API 호출 테스트는 스킵될 수 있음)

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "refactor: geocode/geohash/geoip2 → bluetape4k-geo 통합"
```

---

## Chunk 3: vertx/ 통합

**목표:** `vertx/core` + `vertx/resilience4j` + `vertx/sqlclient` → `vertx/` 단일 모듈
**위험도:** 낮음 — 외부 참조 없음
**settings.gradle.kts 수정 필요**

### Task 3-1: vertx 단일 모듈로 통합

**Files:**
- Create: `vertx/build.gradle.kts`
- Move: `vertx/core/src/` → `vertx/src/`
- Move: `vertx/resilience4j/src/` → `vertx/src/`
- Move: `vertx/sqlclient/src/` → `vertx/src/`
- Delete: `vertx/core/`, `vertx/resilience4j/`, `vertx/sqlclient/`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: vertx/build.gradle.kts 생성**

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-netty"))
    api(project(":bluetape4k-coroutines"))
    api(project(":bluetape4k-jdbc"))
    testImplementation(project(":bluetape4k-junit5"))

    // Vertx core
    api(Libs.vertx_core)
    api(Libs.vertx_lang_kotlin)
    api(Libs.vertx_lang_kotlin_coroutines)
    compileOnly(Libs.vertx_web)
    compileOnly(Libs.vertx_web_client)
    compileOnly(Libs.vertx_junit5)

    // Resilience4j
    api(project(":bluetape4k-resilience4j"))
    compileOnly(Libs.resilience4j_reactor)
    compileOnly(Libs.resilience4j_micrometer)

    // SqlClient
    api(Libs.vertx_sql_client)
    api(Libs.vertx_sql_client_templates)
    implementation(Libs.vertx_mysql_client)
    implementation(Libs.vertx_pg_client)
    compileOnly(Libs.vertx_jdbc_client)
    compileOnly(Libs.agroal_pool)
    compileOnly(project(":bluetape4k-jackson"))
    compileOnly(Libs.jackson_module_kotlin)
    compileOnly(Libs.jackson_module_blackbird)
    implementation(Libs.mybatis_dynamic_sql)

    // Coroutines
    api(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    testRuntimeOnly(Libs.h2_v2)
    testRuntimeOnly(Libs.mysql_connector_j)
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_mysql)
}
```

- [ ] **Step 2: 소스 파일 이동**

```bash
mkdir -p vertx/src/main/kotlin vertx/src/test/kotlin vertx/src/test/resources

cp -r vertx/core/src/main/kotlin/. vertx/src/main/kotlin/
cp -r vertx/core/src/test/kotlin/. vertx/src/test/kotlin/

cp -r vertx/resilience4j/src/main/kotlin/. vertx/src/main/kotlin/
cp -r vertx/resilience4j/src/test/kotlin/. vertx/src/test/kotlin/

cp -r vertx/sqlclient/src/main/kotlin/. vertx/src/main/kotlin/
cp -r vertx/sqlclient/src/test/kotlin/. vertx/src/test/kotlin/

# test resources
for sub in core resilience4j sqlclient; do
  [ -d "vertx/$sub/src/test/resources" ] && cp -r vertx/$sub/src/test/resources/. vertx/src/test/resources/
done
```

- [ ] **Step 3: 서브 모듈 삭제**

```bash
rm -rf vertx/core vertx/resilience4j vertx/sqlclient
```

- [ ] **Step 4: settings.gradle.kts 수정**

```kotlin
// 기존
includeModules("vertx", withBaseDir = true)

// 변경
include("bluetape4k-vertx")
project(":bluetape4k-vertx").projectDir = file("vertx")
```

- [ ] **Step 5: 테스트 실행**

```bash
./gradlew :bluetape4k-vertx:test
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "refactor: vertx/core+resilience4j+sqlclient → bluetape4k-vertx 통합"
```

---

## Chunk 4: spring/ 재편

**목표:** `spring-boot3` 신규 생성 (core + webflux + tests + retrofit2), `spring/jpa` → `data/hibernate`
**위험도:** 중간 — spring 모듈은 여러 테스트 인프라에서 참조됨

### Task 4-1: spring-boot3 신규 모듈 생성

**Files:**
- Create: `spring/boot3/build.gradle.kts`
- Move: `spring/core/src/` → `spring/boot3/src/`
- Move: `spring/webflux/src/` → `spring/boot3/src/`
- Move: `spring/tests/src/` → `spring/boot3/src/`
- Move: `spring/retrofit2/src/` → `spring/boot3/src/`
- Delete: `spring/core/`, `spring/webflux/`, `spring/tests/`, `spring/retrofit2/`

- [ ] **Step 1: spring/boot3 디렉토리 생성**

```bash
mkdir -p spring/boot3/src/main/kotlin spring/boot3/src/test/kotlin spring/boot3/src/test/resources
```

- [ ] **Step 2: 각 spring 서브 모듈 build.gradle.kts 확인 후 의존성 통합**

```bash
cat spring/core/build.gradle.kts spring/webflux/build.gradle.kts \
    spring/tests/build.gradle.kts spring/retrofit2/build.gradle.kts
```

각 모듈의 의존성을 `spring/boot3/build.gradle.kts`로 통합 (중복 제거, compileOnly 패턴 적용)

내부 패키지:
- `io.bluetape4k.spring.core`
- `io.bluetape4k.spring.webflux`
- `io.bluetape4k.spring.tests`
- `io.bluetape4k.spring.retrofit2`

- [ ] **Step 3: 소스 파일 이동**

```bash
for sub in core webflux tests retrofit2; do
  cp -r spring/$sub/src/main/kotlin/. spring/boot3/src/main/kotlin/ 2>/dev/null || true
  cp -r spring/$sub/src/test/kotlin/. spring/boot3/src/test/kotlin/ 2>/dev/null || true
  cp -r spring/$sub/src/test/resources/. spring/boot3/src/test/resources/ 2>/dev/null || true
done
```

- [ ] **Step 4: 서브 모듈 삭제 및 settings.gradle.kts 확인**

```bash
rm -rf spring/core spring/webflux spring/tests spring/retrofit2
```

`settings.gradle.kts`의 `includeModules("spring", withBaseDir = true)` 는 유지 — `spring/boot3`가 자동으로 `bluetape4k-spring-boot3`로 등록됨.

- [ ] **Step 5: 기존 참조 모듈 업데이트**

```bash
rg "bluetape4k-spring-core|bluetape4k-spring-webflux|bluetape4k-spring-tests|bluetape4k-spring-retrofit2" -l --glob "*.kts"
```

발견된 파일에서 → `bluetape4k-spring-boot3`로 교체

- [ ] **Step 6: 테스트 실행**

```bash
./gradlew :bluetape4k-spring-boot3:test
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "refactor: spring/core+webflux+tests+retrofit2 → bluetape4k-spring-boot3 통합"
```

---

### Task 4-2: spring/jpa → data/hibernate 이동

**Files:**
- Modify: `data/hibernate/build.gradle.kts`
- Move: `spring/jpa/src/` → `data/hibernate/src/` (패키지: `io.bluetape4k.hibernate.spring`)
- Delete: `spring/jpa/`

- [ ] **Step 1: data/hibernate build.gradle.kts에 spring-jpa 의존성 추가**

```bash
cat spring/jpa/build.gradle.kts
```

`data/hibernate/build.gradle.kts`에 spring-jpa 의존성 추가 (compileOnly):
```kotlin
compileOnly(Libs.spring_data_jpa)
compileOnly(Libs.spring_tx)
```

- [ ] **Step 2: 소스 이동 및 패키지 변경**

```bash
cp -r spring/jpa/src/main/kotlin/. data/hibernate/src/main/kotlin/
cp -r spring/jpa/src/test/kotlin/. data/hibernate/src/test/kotlin/
```

이동된 파일의 패키지 변경:
```bash
find data/hibernate/src -name "*.kt" -newer spring/jpa/build.gradle.kts \
  -exec grep -l "package io.bluetape4k.spring.jpa" {} \; \
  | xargs sed -i '' 's/package io\.bluetape4k\.spring\.jpa/package io.bluetape4k.hibernate.spring/g'
```

- [ ] **Step 3: spring/jpa 삭제**

```bash
rm -rf spring/jpa
```

- [ ] **Step 4: 기존 참조 업데이트**

```bash
rg "bluetape4k-spring-jpa" -l --glob "*.kts"
```

발견된 파일에서 → `bluetape4k-hibernate`로 교체

- [ ] **Step 5: 테스트 실행**

```bash
./gradlew :bluetape4k-hibernate:test
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "refactor: spring/jpa → data/hibernate 이동 (io.bluetape4k.hibernate.spring)"
```

---

## Chunk 5: infra/cache 통합

**목표:** `cache-core` + `cache-hazelcast` + `cache-lettuce` + `cache-redisson` → `cache` (기존 umbrella 모듈 실화)
**위험도:** 중간 — 여러 외부 모듈이 참조 (hibernate, lettuce, redisson, bucket4j, micrometer 등)

### Task 5-1: cache umbrella → 실 구현 모듈 전환

**Files:**
- Modify: `infra/cache/build.gradle.kts`
- Move: `infra/cache-core/src/` → `infra/cache/src/`
- Move: `infra/cache-hazelcast/src/` → `infra/cache/src/`
- Move: `infra/cache-lettuce/src/` → `infra/cache/src/`
- Move: `infra/cache-redisson/src/` → `infra/cache/src/`
- Delete: `infra/cache-core/`, `infra/cache-hazelcast/`, `infra/cache-lettuce/`, `infra/cache-redisson/`

- [ ] **Step 1: 현재 cache-core, cache-hazelcast, cache-lettuce, cache-redisson build.gradle.kts 분석**

```bash
cat infra/cache-core/build.gradle.kts
cat infra/cache-hazelcast/build.gradle.kts
cat infra/cache-lettuce/build.gradle.kts
cat infra/cache-redisson/build.gradle.kts
```

- [ ] **Step 2: infra/cache/build.gradle.kts 재작성**

기존 umbrella 내용을 제거하고 아래와 같이 작성:
```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-coroutines"))
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Cache Core (Caffeine, JCache)
    api(Libs.caffeine)
    api(Libs.javax_cache_api)
    compileOnly(Libs.cache2k_core)
    compileOnly(Libs.cache2k_api)
    compileOnly(Libs.ehcache3)

    // 백엔드 (사용자가 필요한 것만 runtime 추가)
    compileOnly(Libs.hazelcast)
    compileOnly(Libs.lettuce_core)
    compileOnly(Libs.redisson)

    // 각 서브모듈 추가 의존성은 Step 1 분석 후 추가
    testImplementation(Libs.kotlinx_coroutines_test)
}
```

> **주의:** 각 서브모듈 build.gradle.kts를 읽고 누락된 의존성 반드시 포함

- [ ] **Step 3: src/main/resources (SPI 파일) 병합**

각 서브모듈에 `META-INF/services/` 파일이 있을 수 있음:
```bash
find infra/cache-core infra/cache-hazelcast infra/cache-lettuce infra/cache-redisson \
     -path "*/META-INF/services/*" -type f
```

발견된 SPI 파일을 `infra/cache/src/main/resources/META-INF/services/`로 병합 (내용 append)

- [ ] **Step 4: 소스 파일 이동**

```bash
mkdir -p infra/cache/src/main/kotlin infra/cache/src/test/kotlin infra/cache/src/test/resources

for sub in cache-core cache-hazelcast cache-lettuce cache-redisson; do
  cp -r infra/$sub/src/main/kotlin/. infra/cache/src/main/kotlin/ 2>/dev/null || true
  cp -r infra/$sub/src/test/kotlin/. infra/cache/src/test/kotlin/ 2>/dev/null || true
  cp -r infra/$sub/src/test/resources/. infra/cache/src/test/resources/ 2>/dev/null || true
done
```

- [ ] **Step 5: 서브 모듈 삭제**

```bash
rm -rf infra/cache-core infra/cache-hazelcast infra/cache-lettuce infra/cache-redisson
```

`includeModules("infra", ...)` 가 자동으로 sub-module 등록을 제거함

- [ ] **Step 6: 외부 참조 업데이트**

참조 모듈 목록 (Task 5-0에서 확인한 것):
- `spring/jpa/build.gradle.kts` → `bluetape4k-cache`
- `data/hibernate/build.gradle.kts` → `bluetape4k-cache`
- `io/http/build.gradle.kts` → `bluetape4k-cache`
- `infra/lettuce/build.gradle.kts` → `bluetape4k-cache`
- `infra/bucket4j/build.gradle.kts` → `bluetape4k-cache`
- `infra/redisson/build.gradle.kts` → `bluetape4k-cache`
- `infra/resilience4j/build.gradle.kts` → `bluetape4k-cache`
- `infra/micrometer/build.gradle.kts` → `bluetape4k-cache`
- `utils/jwt/build.gradle.kts` → `bluetape4k-cache`
- `utils/math/build.gradle.kts` → `bluetape4k-cache`
- `javers/core/build.gradle.kts` → `bluetape4k-cache`
- `javers/persistence-redis/build.gradle.kts` → `bluetape4k-cache`
- `examples/redisson/build.gradle.kts` → `bluetape4k-cache`

각 파일에서 `bluetape4k-cache-core`, `bluetape4k-cache-hazelcast` 등 → `bluetape4k-cache`

- [ ] **Step 7: 전체 테스트 실행**

```bash
./gradlew :bluetape4k-cache:test
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "refactor: cache-core/hazelcast/lettuce/redisson → bluetape4k-cache 통합"
```

---

## Chunk 6: aws/ 통합

**목표:** `aws/core` + `aws/dynamodb` + … (10개) → `aws/` 단일 모듈 (`bluetape4k-aws`)
**위험도:** 높음 — 모듈 수 최대, settings.gradle.kts 수정 필요

### Task 6-1: aws 단일 모듈로 통합

**Files:**
- Create: `aws/build.gradle.kts`
- Move: `aws/core/src/`, `aws/dynamodb/src/`, … → `aws/src/`
- Delete: `aws/core/`, `aws/dynamodb/`, `aws/s3/`, `aws/ses/`, `aws/sns/`, `aws/sqs/`, `aws/kms/`, `aws/cloudwatch/`, `aws/kinesis/`, `aws/sts/`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: 각 서브모듈 build.gradle.kts 확인**

```bash
for sub in core dynamodb s3 ses sns sqs kms cloudwatch kinesis sts; do
  echo "=== aws/$sub ===" && cat aws/$sub/build.gradle.kts
done
```

- [ ] **Step 2: aws/build.gradle.kts 생성**

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-coroutines"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // AWS Core (공통 필수)
    api(Libs.aws_core)
    api(Libs.aws_auth)
    api(Libs.aws_http_client_spi)
    api(Libs.aws_netty_nio_client)
    api(Libs.aws_apache_client)

    // 서비스별 (사용자가 필요한 것만 runtime 추가)
    compileOnly(Libs.aws_dynamodb)
    compileOnly(Libs.aws_s3)
    compileOnly(Libs.aws_ses)
    compileOnly(Libs.aws_sns)
    compileOnly(Libs.aws_sqs)
    compileOnly(Libs.aws_kms)
    compileOnly(Libs.aws_cloudwatch)
    compileOnly(Libs.aws_kinesis)
    compileOnly(Libs.aws_sts)

    testImplementation(Libs.localstack)
    testImplementation(Libs.kotlinx_coroutines_test)
}
```

> **주의:** `Libs.kt`에서 정확한 상수명 확인 필요 (`aws_core`, `aws_dynamodb` 등)

- [ ] **Step 3: 소스 파일 이동**

```bash
mkdir -p aws/src/main/kotlin aws/src/test/kotlin aws/src/test/resources

for sub in core dynamodb s3 ses sns sqs kms cloudwatch kinesis sts; do
  cp -r aws/$sub/src/main/kotlin/. aws/src/main/kotlin/ 2>/dev/null || true
  cp -r aws/$sub/src/test/kotlin/. aws/src/test/kotlin/ 2>/dev/null || true
  cp -r aws/$sub/src/test/resources/. aws/src/test/resources/ 2>/dev/null || true
done
```

패키지는 변경 없음 — 각 서비스가 이미 `io.bluetape4k.aws.dynamodb`, `io.bluetape4k.aws.s3` 등 독립 패키지 사용

- [ ] **Step 4: 서브 모듈 삭제**

```bash
rm -rf aws/core aws/dynamodb aws/s3 aws/ses aws/sns aws/sqs aws/kms aws/cloudwatch aws/kinesis aws/sts
```

- [ ] **Step 5: settings.gradle.kts 수정**

```kotlin
// 기존
includeModules("aws", withBaseDir = true)

// 변경
include("bluetape4k-aws")
project(":bluetape4k-aws").projectDir = file("aws")
```

- [ ] **Step 6: 전체 빌드 확인**

```bash
./gradlew :bluetape4k-aws:build -x test
./gradlew :bluetape4k-aws:test
```
Expected: BUILD SUCCESSFUL (LocalStack 테스트는 Docker 필요)

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "refactor: aws/* 10개 서브모듈 → bluetape4k-aws 단일 모듈 통합"
```

---

## Chunk 7: aws-kotlin/ 통합

**목표:** `aws-kotlin/core` + `aws-kotlin/dynamodb` + … (12개) → `aws-kotlin/` 단일 모듈 (`bluetape4k-aws-kotlin`)
**위험도:** 높음 — Chunk 6과 동일 패턴

### Task 7-1: aws-kotlin 단일 모듈로 통합

Chunk 6과 동일한 절차. 차이점:

- 대상 디렉토리: `aws-kotlin/`
- 서브모듈: `core`, `dynamodb`, `s3`, `ses`, `sesv2`, `sns`, `sqs`, `kms`, `cloudwatch`, `kinesis`, `sts`, `tests`
- 모듈명: `bluetape4k-aws-kotlin`
- 의존성: AWS Kotlin SDK (`Libs.aws_kotlin_*`)
- settings.gradle.kts:

```kotlin
// 기존
includeModules("aws-kotlin", withBaseDir = true)

// 변경
include("bluetape4k-aws-kotlin")
project(":bluetape4k-aws-kotlin").projectDir = file("aws-kotlin")
```

- `aws-kotlin/tests` 서브모듈의 테스트 공통 인프라는 `aws-kotlin/src/test/kotlin/`으로 이동

- [ ] **Step 1: 각 서브모듈 build.gradle.kts 확인**

```bash
for sub in core dynamodb s3 ses sesv2 sns sqs kms cloudwatch kinesis sts tests; do
  echo "=== aws-kotlin/$sub ===" && cat aws-kotlin/$sub/build.gradle.kts
done
```

- [ ] **Step 2: aws-kotlin/build.gradle.kts 생성**

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-coroutines"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // AWS Kotlin SDK Core (공통 필수)
    api(Libs.aws_kotlin_core)

    // 서비스별 (사용자가 필요한 것만 runtime 추가)
    compileOnly(Libs.aws_kotlin_dynamodb)
    compileOnly(Libs.aws_kotlin_s3)
    compileOnly(Libs.aws_kotlin_ses)
    compileOnly(Libs.aws_kotlin_sesv2)
    compileOnly(Libs.aws_kotlin_sns)
    compileOnly(Libs.aws_kotlin_sqs)
    compileOnly(Libs.aws_kotlin_kms)
    compileOnly(Libs.aws_kotlin_cloudwatch)
    compileOnly(Libs.aws_kotlin_kinesis)
    compileOnly(Libs.aws_kotlin_sts)

    testImplementation(Libs.localstack)
    testImplementation(Libs.kotlinx_coroutines_test)
}
```

- [ ] **Step 3: 소스 파일 이동**

```bash
mkdir -p aws-kotlin/src/main/kotlin aws-kotlin/src/test/kotlin aws-kotlin/src/test/resources

for sub in core dynamodb s3 ses sesv2 sns sqs kms cloudwatch kinesis sts tests; do
  cp -r aws-kotlin/$sub/src/main/kotlin/. aws-kotlin/src/main/kotlin/ 2>/dev/null || true
  cp -r aws-kotlin/$sub/src/test/kotlin/. aws-kotlin/src/test/kotlin/ 2>/dev/null || true
  cp -r aws-kotlin/$sub/src/test/resources/. aws-kotlin/src/test/resources/ 2>/dev/null || true
done
```

- [ ] **Step 4: 서브 모듈 삭제**

```bash
rm -rf aws-kotlin/core aws-kotlin/dynamodb aws-kotlin/s3 aws-kotlin/ses \
        aws-kotlin/sesv2 aws-kotlin/sns aws-kotlin/sqs aws-kotlin/kms \
        aws-kotlin/cloudwatch aws-kotlin/kinesis aws-kotlin/sts aws-kotlin/tests
```

- [ ] **Step 5: settings.gradle.kts 수정**

```kotlin
// 기존
includeModules("aws-kotlin", withBaseDir = true)

// 변경
include("bluetape4k-aws-kotlin")
project(":bluetape4k-aws-kotlin").projectDir = file("aws-kotlin")
```

- [ ] **Step 6: 전체 빌드 확인**

```bash
./gradlew :bluetape4k-aws-kotlin:build -x test
./gradlew :bluetape4k-aws-kotlin:test
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "refactor: aws-kotlin/* 12개 서브모듈 → bluetape4k-aws-kotlin 단일 모듈 통합"
```

---

## 완료 후 검증

- [ ] **전체 빌드 확인**

```bash
./gradlew build -x test 2>&1 | tail -20
```
Expected: BUILD SUCCESSFUL

- [ ] **모듈 수 확인**

```bash
./gradlew projects 2>&1 | grep "Project '" | wc -l
```
Expected: ~82개 (기존 ~117개)

- [ ] **CLAUDE.md Architecture 섹션 업데이트**

통합된 모듈 목록 반영 (서브모듈 항목 제거, 통합 모듈 설명 추가)

- [ ] **최종 Commit**

```bash
git add CLAUDE.md && git commit -m "docs: CLAUDE.md 모듈 통합 반영"
```

---

## 통합 결과 요약

| 청크 | 통합 전 | 통합 후 | 절감 |
|------|---------|---------|------|
| 1. jackson/jackson3 | 6 | 2 | -4 |
| 2. utils/geo | 3 | 1 | -2 |
| 3. vertx | 3 | 1 | -2 |
| 4. spring 재편 | 9 | 5 | -4 |
| 5. infra/cache | 5 | 1 | -4 |
| 6. aws | 10 | 1 | -9 |
| 7. aws-kotlin | 12 | 1 | -11 |
| **합계** | **~117** | **~82** | **-35** |
