# LZ4 yawkat 마이그레이션 설계 (Issue #203)

- 작성일: 2026-04-28
- 브랜치: `feat/lz4-yawkat-migration`
- 워크트리: `.worktrees/feat-lz4-yawkat-migration`
- 관련 이슈: [#203](https://github.com/bluetape4k/bluetape4k-projects/issues/203)
- Rev: v2 (2026-04-28, Spec Review 반영)

---

## 1. 배경 및 목표

### 1.1 배경

`org.lz4:lz4-java` (lz4/lz4-java repo) 에서 **2개의 HIGH CVE** 가 보고되었고,
원본 repo `lz4/lz4-java` 는 **2025년 12월 archived** 상태로 전환되어 유지보수가 중단되었다.

이에 따라 동일 패키지 (`net.jpountz.lz4.*`) namespace 를 유지하면서 fork 형태로 활발히 유지되는
`at.yawk.lz4:lz4-java` 로 전환한다.

### 1.2 사실 자료 (Step 1-R + Spec Review 결과)

**CVE 현황:**

| CVE | CVSS | 내용 | fix 버전 |
|-----|------|------|---------|
| **CVE-2025-12183** | 8.8 HIGH | out-of-bounds read (OOB) | `at.yawk.lz4:lz4-java:1.8.1+` |
| **CVE-2025-66566** | 8.2 HIGH | Java decompressor 미초기화 버퍼 정보 유출 | `at.yawk.lz4:lz4-java:1.10.1+` |

> **주의**: `org.lz4:lz4-java:1.8.1` 은 실제 JAR 바이너리가 없는 **Sonatype relocation POM** 이다.
> Gradle 이 이를 resolve 하면 `at.yawk.lz4:lz4-java:1.8.1` 로 리디렉션된다.
> CVE-2025-66566 은 1.8.1 에서 미패치이므로, relocation 경로로는 CVE 를 완전히 해소할 수 없다.
> **목표 버전 `1.11.0` 이 두 CVE 모두 패치된 최신 안전 버전이다.**

**아티팩트 현황:**

- **`org.lz4:lz4-java`**: 최신 버전 1.8.1 (relocation POM), 원본 repo 2025-12 archived (유지 중단)
- **`at.yawk.lz4:lz4-java`**: Maven Central 존재 확인
  - URL: `repo1.maven.org/maven2/at/yawk/lz4/lz4-java/maven-metadata.xml`
  - 최신 버전: **1.11.0** (2026-04-09), Maven Central GPG 서명 포함
  - 버전 히스토리: 1.8.1 → 1.9.0 → 1.10.0 → 1.10.1 (CVE-2025-66566 fix) → 1.10.4 → 1.11.0
  - Sonatype 이 `org.lz4:lz4-java:1.8.1` 을 `at.yawk.lz4` 로 relocation 한 것은 공식 후계자 인정

**Binary compatibility:**
- 두 artifact 모두 `net.jpountz.lz4.*` 동일 패키지 namespace
- CVE-2025-12183 fix 로 `LZ4Factory.nativeInstance()` / `unsafeInstance()` 가 안전한 Java 구현을 반환하도록 변경됨 (native 성능 의존 코드에서 성능 저하 가능 — 기능은 동일)

**직접 사용 모듈**: 30개 모듈 `build.gradle.kts` + 루트 `build.gradle.kts` BOM = 총 31개 파일에서 `Libs.lz4_java` 참조
(`compileOnly`/`runtimeOnly`/`testImplementation`/`implementation` 혼합)

**Kafka transitive chain (직접 확인됨):**
- `infra/kafka` — `api(Libs.kafka_clients)`, `implementation(Libs.spring_kafka)`, `implementation(Libs.reactor_kafka)`, `compileOnly(Libs.kafka_streams)`, `testImplementation(Libs.kafka_streams_test_utils)` → 모두 `kafka-clients` → `org.lz4:lz4-java` pull
- `testing/testcontainers` — `compileOnly(Libs.kafka_clients)`, `compileOnly(Libs.spring_kafka)` → 동일 경로

**추가 transitive chain (구현 전 검증 필요):**
- `infra/pulsar` — `api(Libs.pulsar_client)`: Pulsar 은 LZ4 codec 내장
- `io/avro` — `api(Libs.avro)` + `runtimeOnly(Libs.lz4_java)`: Avro LZ4 codec 사용
- `infra/redisson` / `data/*-redisson` — Redisson 이 LZ4 를 선택적 압축에 사용
- 기타: `./gradlew dependencies --configuration runtimeClasspath | rg "org.lz4"` 전체 스캔으로 최종 확인

### 1.3 목표

`at.yawk.lz4:lz4-java:1.11.0` 으로 **단일 전환** — `org.lz4:lz4-java` 를 runtime classpath 에서 완전 제거.

CVE-2025-12183 + CVE-2025-66566 동시 패치 + 장기적으로 활발히 유지되는 fork 사용.

### 1.4 승인 체크리스트

- [ ] 두 CVE (CVE-2025-12183, CVE-2025-66566) 의 영향도 및 우선순위 합의
- [ ] `at.yawk.lz4:lz4-java:1.11.0` 단일 전환 방향 합의
- [ ] "단일 전환 = runtime classpath 완전 제거 (transitive 포함)" 의미 합의

---

## 2. 리스크 분석

### 2.1 리스크 1 — `org.lz4:lz4-java` transitive 유입 경로 (다수)

`kafka-clients` 뿐만 아니라 `spring_kafka`, `reactor_kafka`, `kafka_streams`, `kafka_streams_test_utils`, `pulsar_client`, `avro` 등이 모두 `org.lz4:lz4-java` 를 transitively pull 한다.

단일 dependency 에 `exclude` 를 적용하는 방식은 **다른 경로를 통해 re-entry 를 막지 못한다**.

**완화 전략 (CRITICAL-1 수정):**

`infra/kafka` 와 `testing/testcontainers` 에서 **module-wide** exclude 적용:

```kotlin
configurations.all {
    exclude(group = "org.lz4", module = "lz4-java")
}
```

이 방식은 해당 모듈의 모든 configuration (api, implementation, compileOnly, testImplementation, runtimeOnly 등) 에서 `org.lz4:lz4-java` 를 차단한다.

추가로 `infra/pulsar`, `io/avro`, `infra/redisson` 등 다른 경로에서의 유입은 구현 전 dep-tree 전체 스캔으로 확인 후 필요 시 추가 exclude 적용.

**DoD 검증**: `./gradlew dependencies --configuration runtimeClasspath | rg "org.lz4"` 를 영향 모듈 전체에 실행 후 결과 없음 확인.

### 2.2 리스크 2 — Binary-compatible 가정 검증

두 artifact 모두 동일한 `net.jpountz.lz4.*` 패키지를 사용하지만, 다음을 검증해야 한다:

- **Native library 플랫폼 커버리지**: `at.yawk.lz4:lz4-java:1.11.0` JAR 내 native binary 가 Linux x86_64, Linux aarch64, macOS aarch64, macOS x86_64 플랫폼을 지원하는지 확인 필요
  - `LZ4Factory.fastestInstance()` 가 실제로 JNI 구현(`LZ4JNIFactory`)을 반환하는지 테스트로 assert
  - 네이티브 로드 실패 시 pure Java 로 fallback (에러 없이 성능만 저하)
- **API 시그니처 동등성**: 1.11.0 에서 핵심 클래스(`LZ4Factory`, `LZ4Compressor` 등) 시그니처 유지 확인 → 컴파일 + 테스트 통과로 검증

**완화 전략**:
- exclude 규칙으로 classpath 에 하나의 lz4 artifact 만 존재하게 함
- DoD 에 native library 플랫폼 검증 항목 추가

### 2.3 리스크 3 — 루트 BOM `dependencyManagement` (버전 정합)

루트 `build.gradle.kts` 의 `dependencyManagement` 블록의 `org.lz4:lz4-java` 핀은 `Libs.lz4_java` 좌표 변경 후 **다른 groupId 의 artifact 를 가리키게 된다**.

> ⛔ **중요 제약**: `dependencyManagement` BOM 핀은 **동일 groupId 내 버전만 강제**한다.
> `at.yawk.lz4:lz4-java` 를 핀해도 `org.lz4:lz4-java` transitive 유입을 막지 못한다 — groupId 가 다르기 때문.
> 실제 eviction 은 §2.1 의 `configurations.all { exclude(...) }` 가 수행한다.

따라서 BOM 변경은 `at.yawk.lz4` 의 버전을 일관되게 관리하기 위한 문서화 목적이며, `org.lz4` 제거 메커니즘은 exclude 규칙이다.

**완화 전략**: BOM 에서 `org.lz4:lz4-java` 핀을 `at.yawk.lz4:lz4-java:1.11.0` 으로 교체 (버전 정합) + exclude 규칙 (실제 eviction).

### 2.4 리스크 4 — Rollback

이번 변경은 Kotlin 소스 변경 없이 build.gradle.kts + Libs.kt 만 수정하므로 rollback 이 단순하다.

**Rollback 절차**: `Libs.kt` + `build.gradle.kts` (루트 BOM, infra/kafka, testing/testcontainers) 를 git revert 한 단일 커밋으로 복구. 압축 데이터 형식은 LZ4 표준 포맷으로 두 구현 모두 동일하게 처리하므로 영속화 데이터 재처리 불필요.

### 2.5 승인 체크리스트

- [ ] 4개 리스크가 충분히 식별되었는가
- [ ] exclude 범위 (`configurations.all`) 가 단일 dependency exclude 보다 안전함에 동의
- [ ] BOM 변경이 org.lz4 제거 메커니즘이 아님을 확인

---

## 3. 접근 방식 비교

### 3.1 Option A — `org.lz4:lz4-java:1.8.1` 로만 업그레이드 ❌ **기각**

**개요**: artifact 좌표 변경 없이 버전만 1.8.0 → 1.8.1 로 올린다.

> **⚠️ 중요**: `org.lz4:lz4-java:1.8.1` 은 Maven Central 에 **실제 JAR 가 없는 relocation POM** 이다.
> Gradle 이 resolve 하면 `at.yawk.lz4:lz4-java:1.8.1` 로 자동 리디렉션된다.
> 즉 Option A 는 의도치 않게 Option B 를 수행하는 것과 같으나, **CVE-2025-66566 이 미패치된 1.8.1 버전으로 고정**된다.

| 구분 | 내용 |
|------|------|
| 변경량 | `Libs.lz4_java` 1줄 |
| CVE-2025-12183 패치 | ✅ (1.8.1 에서 fix) |
| CVE-2025-66566 패치 | ❌ **미패치** (1.10.1+ 에서 fix) |
| 장기 유지보수 | ❌ archived repo 의존 지속 |
| 추가 작업 | 없음 |

### 3.2 Option B — `at.yawk.lz4:lz4-java:1.11.0` 로 전환 ✅ **선택**

**개요**: artifact 자체를 yawkat fork 최신 버전으로 전환.

| 구분 | 내용 |
|------|------|
| 변경량 | `Libs.lz4_java` + 루트 BOM + `configurations.all { exclude }` (2개 모듈) + README |
| CVE-2025-12183 패치 | ✅ |
| CVE-2025-66566 패치 | ✅ (1.10.1+ 에서 fix, 1.11.0 포함) |
| 장기 유지보수 | ✅ 활발히 유지 (1.11.0 이 2026-04-09 릴리즈) |
| 추가 작업 | `configurations.all { exclude }` (2개 모듈) |

### 3.3 선택 사유

- `org.lz4` repo 가 archived → 미래 CVE 발생 시 패치 불가
- Option A 는 CVE-2025-66566 (CVSS 8.2 HIGH, 정보 유출) 을 미패치
- yawkat fork 는 `net.jpountz.lz4.*` namespace 를 유지하므로 직접 사용 코드 (30개 모듈) 무수정
- Sonatype 이 공식 relocation POM 을 통해 yawkat fork 를 후계자로 인정

### 3.4 승인 체크리스트

- [ ] Option A 가 relocation POM 이며 CVE-2025-66566 미패치임에 동의
- [ ] Option B 의 추가 작업 (`configurations.all { exclude }` × 2 모듈) 이 수용 가능함에 동의

---

## 4. 구현 계획

### 4.0 사전 작업 — Transitive dep-tree 전체 스캔

구현 전, `org.lz4:lz4-java` 가 어떤 경로로 유입되는지 전체 파악:

```bash
./gradlew dependencies --configuration runtimeClasspath | rg "org.lz4" 2>&1 | sort -u
```

또는 변경 예상 모듈별:

```bash
for m in bluetape4k-kafka bluetape4k-testcontainers bluetape4k-pulsar bluetape4k-avro bluetape4k-redisson bluetape4k-lettuce; do
    echo "=== $m ==="
    ./gradlew :$m:dependencies --configuration runtimeClasspath | rg "org.lz4"
done
```

결과에 따라 §4.1.3-4.1.4 외 추가 exclude 모듈을 결정한다.

### 4.1 변경 대상

#### 4.1.1 `buildSrc/src/main/kotlin/Libs.kt`

```kotlin
// Before
const val lz4_java = "org.lz4:lz4-java:1.8.0"                     // https://mvnrepository.com/artifact/org.lz4/lz4-java
// kafka clients 내부에 기존 lz4-java 를 사용한다.
// const val lz4_java = "at.yawk.lz4:lz4-java:1.8.1"              // https://mvnrepository.com/artifact/at.yawk.lz4/lz4-java

// After
// CVE-2025-12183 (CVSS 8.8) + CVE-2025-66566 (CVSS 8.2) fix.
// org.lz4/lz4-java archived 2025-12. yawkat fork keeps net.jpountz.lz4.* namespace (binary-compatible).
const val lz4_java = "at.yawk.lz4:lz4-java:1.11.0"                // https://mvnrepository.com/artifact/at.yawk.lz4/lz4-java
```

#### 4.1.2 루트 `build.gradle.kts` — `dependencyManagement`

`org.lz4:lz4-java` BOM pin → `at.yawk.lz4:lz4-java:1.11.0` 로 교체 (버전 정합 목적; eviction 은 §4.1.3-4.1.4 exclude 가 수행).

#### 4.1.3 `infra/kafka/build.gradle.kts`

```kotlin
// org.lz4:lz4-java 가 kafka-clients, spring-kafka, reactor-kafka, kafka-streams 경로 모두를 통해 유입된다.
// 단일 dependency exclude 가 아닌 configuration-wide exclude 로 모든 경로를 차단한다.
configurations.all {
    exclude(group = "org.lz4", module = "lz4-java")
}
```

#### 4.1.4 `testing/testcontainers/build.gradle.kts`

동일한 `configurations.all { exclude(group = "org.lz4", module = "lz4-java") }` 추가.

#### 4.1.5 추가 exclude (사전 dep-tree 스캔 결과에 따라)

`infra/pulsar`, `io/avro`, `infra/redisson` 등에서 `org.lz4` 유입이 확인되면 동일한 `configurations.all { exclude }` 적용.

#### 4.1.6 README 업데이트

- `infra/kafka/README.md` (영문) — CVE-2025-12183 + CVE-2025-66566 + yawkat 전환 안내 + 직접 lz4 사용자 마이그레이션 가이드
- `infra/kafka/README.ko.md` (한글) — 동일 내용

README 포함 내용:
- CVE 배경 (두 CVE 번호 + CVSS)
- `org.lz4` → `at.yawk.lz4` 전환 근거
- 직접 `org.lz4:lz4-java` 를 사용하는 다운스트림 사용자를 위한 안내: `at.yawk.lz4:lz4-java:1.11.0` 으로 교체, 코드 변경 불필요 (동일 namespace)
- kafka-clients 와 함께 사용 시 exclude 안내

### 4.2 변경 불필요 항목

| 항목 | 사유 |
|------|------|
| 30개 직접 참조 모듈의 build.gradle.kts | `Libs.lz4_java` 값만 바뀌므로 수정 불필요 |
| Kotlin 소스 코드 (`net.jpountz.lz4.*` import) | binary-compatible (동일 namespace) |
| KDoc 신규 작성 | dependency 변경만이므로 public API 변경 없음 |

### 4.3 작업 순서

1. `Libs.kt` 변경 (`org.lz4` → `at.yawk.lz4:1.11.0`)
2. 루트 `build.gradle.kts` `dependencyManagement` 교체
3. `infra/kafka/build.gradle.kts` `configurations.all { exclude }` 추가
4. `testing/testcontainers/build.gradle.kts` 동일
5. **dep-tree 전체 스캔** — `org.lz4` 흔적 있으면 §4.1.5 에 따라 추가 exclude
6. `:bluetape4k-kafka:test`, `:bluetape4k-core:test`, `:bluetape4k-io:test`, `:bluetape4k-lettuce:test` 실행
7. `:bluetape4k-testcontainers:test`, `:bluetape4k-hibernate-cache-lettuce:test`, `:bluetape4k-avro:test` 실행
8. `infra/kafka/README.md` + `README.ko.md` 업데이트
9. 커밋 (`fix: org.lz4 → at.yawk.lz4:1.11.0 (CVE-2025-12183, CVE-2025-66566)`)
10. PR 생성

### 4.4 승인 체크리스트

- [ ] 변경 대상 (Libs.kt, 루트 BOM, kafka, testcontainers, README × 2) 이 충분
- [ ] `configurations.all { exclude }` 가 단일 dep exclude 보다 안전함에 동의
- [ ] 사전 dep-tree 스캔 → 추가 exclude 발견 시 반영 계획 합의

---

## 5. DoD (Definition of Done)

| 항목 | 검증 방법 |
|------|-----------|
| `at.yawk.lz4:lz4-java:1.11.0` 으로 전환 완료 | `Libs.kt` grep 확인 |
| 루트 BOM `at.yawk.lz4:lz4-java:1.11.0` 로 교체 | 루트 `build.gradle.kts` grep 확인 |
| `org.lz4:lz4-java` runtime classpath 에서 완전 제거 | `./gradlew :bluetape4k-kafka:dependencies --configuration runtimeClasspath \| rg "org.lz4"` 결과 없음 |
| `infra/kafka` + `testing/testcontainers` `configurations.all { exclude }` 적용 | 파일 diff 확인 |
| 추가 transitive 경로 (pulsar/avro/redisson) dep-tree 스캔 완료 | 스캔 결과 `org.lz4` 없음 또는 추가 exclude 적용 |
| native library 플랫폼 커버리지 확인 | `at.yawk.lz4:lz4-java:1.11.0` JAR 내 native binary (linux-x86_64, linux-aarch64, darwin-aarch64, darwin-x86_64) 존재 확인 |
| `:bluetape4k-kafka:test` 통과 | `./gradlew :bluetape4k-kafka:test` |
| `:bluetape4k-core:test` 통과 | `./gradlew :bluetape4k-core:test` |
| `:bluetape4k-io:test` 통과 | `./gradlew :bluetape4k-io:test` |
| `:bluetape4k-lettuce:test` 통과 | `./gradlew :bluetape4k-lettuce:test` |
| `:bluetape4k-testcontainers:test` 통과 | `./gradlew :bluetape4k-testcontainers:test` |
| `:bluetape4k-hibernate-cache-lettuce:test` 통과 | `./gradlew :bluetape4k-hibernate-cache-lettuce:test` |
| `:bluetape4k-avro:test` 통과 | `./gradlew :bluetape4k-avro:test` |
| `infra/kafka` README.md + README.ko.md 업데이트 (CVE 안내 포함) | 파일 diff 확인 |

### 5.1 승인 체크리스트

- [ ] DoD 14개 항목이 마이그레이션의 완료 기준으로 충분
- [ ] 검증 방법이 명확하고 실행 가능
- [ ] `hibernate-cache-lettuce` 테스트 추가 이유 (유일한 `implementation` 범위) 에 동의

---

## 6. Rollback 전략

**Rollback 절차 (단일 commit revert 로 충분):**

```bash
git revert <commit-hash>
./gradlew :bluetape4k-kafka:build :bluetape4k-core:build
```

변경 파일은 `Libs.kt`, 루트 `build.gradle.kts`, `infra/kafka/build.gradle.kts`, `testing/testcontainers/build.gradle.kts`, README 2개 — 소스 코드 변경 없음.

**데이터 호환성**: LZ4 압축 포맷은 표준 포맷이므로 두 구현이 동일하게 처리. 영속화 데이터 재처리 불필요.

---

## 7. 후속 단계

본 spec 승인 후:
1. **Plan 단계**: 본 spec → 단계별 plan (`docs/superpowers/plans/2026-04-28-lz4-yawkat-migration-plan.md`) 작성
2. **구현 단계**: §4.0 dep-tree 스캔 → §4.1 변경 → §4.3 작업 순서 실행
3. **PR 단계**: DoD 전수 검증 → `gh pr create`
