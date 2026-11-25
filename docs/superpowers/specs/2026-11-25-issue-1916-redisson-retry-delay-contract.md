# #1916 설계: Redisson 재시도 지연 계약

- 상태: 승인 명세의 구현 및 로컬 검증 완료. 인라인 fallback 리뷰 PASS; 독립 코드 리뷰는 UNASSESSABLE이다. PR/hosted CI/merge 상태는 live GitHub 기록을 기준으로 확인한다.
- 대상: bluetape4k-projects, 이슈 [#1916](https://github.com/bluetape4k/bluetape4k-projects/issues/1916)
- 기준 브랜치: `develop`
- 작성일: 2026-11-25

## 문제와 근거

Projects는 중앙 Catalog ref `c44ec6c47aa3a8dc3e862dbd7d041eb9d78355f3`를 사용한다. 해당 Catalog의 Redisson은 `4.8.0`이다. 2026-11-25 확인 당시 Redisson GitHub의 `/releases/latest`는 `redisson-4.8.0` 태그로 이동했다. 이 날짜를 최신 릴리스 관찰 시점으로 기록하고, 불변 태그의 구현을 계약 근거로 사용한다.

Redisson 4.8.0의 `BaseConfig.setRetryInterval(int)`는 deprecated이며, 직접 호출하면 값을 `ConstantDelay(Duration.ofMillis(retryInterval))`로 변환한다. `ConfigSupport.CustomPropertyUtils`는 `retryInterval`을 무시 목록에 두지만, 이 저장소 fixture를 `Config.fromYAML`로 읽으면 Redisson의 YAML 생성자가 해당 키 처리 중 null property에 `isWritable()`을 호출해 예외를 낸다. 따라서 기존 fixture는 `EqualJitterDelay`로 성공적으로 파싱되는 설정이 아니며, `BaseConfig`의 기본 전략만으로 그 fixture의 유효 동작을 설명할 수 없다. 현재 fixture에는 `pingTimeout`, `reconnectionTimeout`, `failedAttempts`, `dnsMonitoring`, `useLinuxNativeEpoll`도 Redisson 4.8.0의 JavaBean 속성에 없어서 파싱을 막는다. `RedissonClientSupport.kt`의 고동시성 설정은 이미 새 API인 `retryDelay = ConstantDelay(...)`로 1초 고정 지연을 사용하지만, KDoc에는 여전히 `retryInterval: 1초`가 기재되어 있다.

이 설계는 중앙 Catalog의 버전을 바꾸지 않고 문서와 YAML 테스트 설정을 실제 Redisson 계약에 맞춘다. 테스트 YAML의 미지원 속성을 제거하고 `retryDelay: ConstantDelay(1.5초)`를 지정해 Redisson 4.8.0에서 실제 파싱 가능한 설정으로 만든다. 이 변경은 테스트 fixture 범위에 한정하며, 고동시성 기본 설정의 1초 지연이나 제품 코드의 지연 동작은 바꾸지 않는다. Redisson 설정 파싱 테스트로 두 경로를 각각 고정한다.

## 결정

- Redisson `4.8.0`을 유지한다. 2026-11-25 기준 `/releases/latest`가 `redisson-4.8.0`을 가리켰고, 현재 Catalog의 고정 참조와 일치한다.
- `Config.applyHighConcurrencyDefaults`의 1초 고정 지연 동작을 유지한다.
- 테스트 YAML의 의도된 1.5초 고정 지연을 `retryDelay`와 `ConstantDelay`로 명시하고 Redisson 4.8.0에서 JavaBean 속성으로 인식되지 않는 오래된 fixture 키를 제거한다. `retryInterval`은 Redisson의 무시 목록에 있지만 현재 파서 경로에서는 예외를 일으키므로, 새 설정은 기존 fixture를 파싱 가능하게 하고 1.5초 고정 지연을 실제로 검증한다.
- `RedissonConst.DEFAULT_RETRY_INTERVAL_MILLIS`는 Bluetape4k 공개 상수로서 값과 이름을 유지한다. 이 식별자는 Redisson 설정 키나 deprecated setter가 아니므로 KDoc만 고정 지연 용어로 갱신한다.
- Redisson 설정을 Redis에 연결하지 않고 읽는 계약 테스트를 추가한다. 테스트는 YAML의 `retryAttempts: 3`, 파싱 결과의 `ConstantDelay` 형식, `calcDelay(0)`과 `calcDelay(1)`이 각각 1500ms를 반환하는지 확인한다. 두 입력값은 고정 지연 전략의 반환값만 검증하며, Redisson 명령 실행 순서나 `retryAttempts`가 지연 인덱스에 대응하는 방식을 정의하지 않는다.
- 새 공개 API, 의존성, Catalog 변경은 추가하지 않는다.
- `RedissonCacheConfig.writeRetryInterval`은 Bluetape4k 자체의 캐시 쓰기 재시도 설정이다. Redisson의 deprecated `retryInterval`과 다른 계약이므로 이 변경에서 이름이나 동작을 바꾸지 않는다.

YAML의 `singleServerConfig` 아래 목표 표현은 다음과 같다. 구현에서 `Config.fromYAML` 파싱 테스트로 Redisson 파서의 실제 계약을 검증했다.

```yaml
singleServerConfig:
    retryDelay: !<org.redisson.config.ConstantDelay> { delay: PT1.5S }
```

## 검토한 대안

| 대안 | 결과 | 판단 |
|---|---|---|
| 고동시성 설정의 1초 `ConstantDelay`를 유지하고 테스트 YAML의 의도된 1.5초 지연을 새 키로 실제 적용한다 | 제품 코드 지연은 유지한다. 테스트 설정은 Redisson 4.8.0에서 파싱되지 않던 legacy 키를 지원 키로 바꾸고 명시적 `ConstantDelay`를 사용한다 | 채택 |
| 기본 `EqualJitterDelay` 또는 다른 jitter 전략으로 전환한다 | 재시도 시점 분포가 달라져 동작 계약이 바뀐다 | 성능·운영 요구와 호환성 근거가 없어 이번 변경에서 제외 |
| Redisson 버전을 올린다 | Catalog pin이 2026-11-25에 확인한 공식 최신 릴리스 `4.8.0`과 일치한다 | 변경 근거가 없어 제외 |
| 오래된 KDoc와 YAML 항목을 그대로 둔다 | deprecated 설정과 현재 구현 간의 불일치 및 회귀 가능성이 남는다 | 제외 |

## 범위

### 포함

- `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt`의 KDoc과 `RedissonConst.kt`의 재시도 상수 KDoc을 `retryDelay` 및 고정 지연 용어로 갱신한다. `DEFAULT_RETRY_INTERVAL_MILLIS`의 공개 이름과 값은 보존한다.
- `infra/redisson/src/test/resources/redisson.yaml`에서 `retryInterval: 1500`을 실제 적용되는 1.5초 `retryDelay`/`ConstantDelay` 표현으로 바꾸고, Redisson 4.8.0에 없는 `pingTimeout`, `reconnectionTimeout`, `failedAttempts`, `dnsMonitoring`, `useLinuxNativeEpoll` 키를 제거한다.
- YAML 테스트 설정을 파싱해 지연 구현체와 반복 시 지연 시간을 확인하는 Redis 비의존 테스트를 추가한다.
- 이슈 #1916의 대상 근거에 Redisson 버전 선택, deprecated 대체, 문서·테스트 공백과 7-Tier 판단을 기록한다.

### 제외

- 중앙 Dependencies 저장소의 Catalog·버전·태그 변경
- Redisson 또는 다른 외부 라이브러리의 버전 변경
- 제품 코드의 재시도 지연 동작 변경. 단, 테스트 YAML은 무시되던 키 대신 1.5초 고정 지연을 실제 적용하도록 갱신한다.
- Bluetape4k 공개 API나 ABI 변경
- 별도 근거가 없는 다른 외부 라이브러리의 대규모 업그레이드
- `writeRetryInterval`을 포함한 Bluetape4k 자체 재시도 API 이름 변경

## 동작 및 호환성 계약

1. `Config.applyHighConcurrencyDefaults`는 `DEFAULT_RETRY_INTERVAL_MILLIS`에 해당하는 1초의 `ConstantDelay`를 계속 설정한다.
2. `redisson.yaml`은 `retryAttempts: 3`과 1500ms 고정 지연을 명시적으로 설정한다. 기존 `retryInterval` 및 Redisson 4.8.0에 없는 fixture 키 때문에 설정 파싱이 실패했으며, 이 변경으로 유효한 fixture에서 `ConstantDelay(1500ms)`를 검증한다.
3. YAML 테스트는 설정 파일을 Redisson `Config`로 읽고 `retryAttempts == 3`이며 `retryDelay.calcDelay(0)`과 `retryDelay.calcDelay(1)`의 결과가 각각 1500ms인지 확인한다. 이 두 입력 인덱스는 고정 지연 전략의 계산값만 검증하며 Redisson 명령 실행 순서나 재시도 횟수와 지연 인덱스의 대응은 정의하지 않는다.
4. 고동시성 기본 설정 테스트는 `applyHighConcurrencyDefaults` 결과의 `retryAttempts == 3`, `ConstantDelay` 형식, 1초 지연을 확인한다.
5. 새 테스트는 Redis나 Testcontainers를 시작하지 않는다. 설정 변환만 검증한다.
6. 테스트가 YAML tag 또는 `Duration` 변환을 거부하면 임의로 값을 낮추거나 테스트를 건너뛰지 않는다. Redisson 공식 파서가 지원하는 표현을 확인해 테스트 설정을 수정하고, 같은 의미 검증을 유지한다.

## 7-Tier 검토 기준

| 계층 | 검토 판단과 구현 조건 |
|---|---|
| 보안 | 자격 증명이나 입력 경계를 추가하지 않는다. 저장소에 포함된 고정 설정만 파싱하며 비밀 값은 넣지 않는다. |
| 운영 안정성 | 고동시성 기본 설정의 재시도 횟수와 1초 고정 지연을 유지한다. 테스트 YAML은 파싱 실패를 일으키던 오래된 키를 제거하고 1.5초 고정 지연을 명시한다. 제품의 다른 지연 전략을 바꾸지 않는다. |
| 구조·재사용 | Redisson이 제공하는 `DelayStrategy`와 `ConstantDelay`를 사용한다. 자체 추상화나 새 의존성은 만들지 않는다. |
| Kotlin/API | 기존 공개 함수 시그니처와 JVM ABI를 유지한다. 변경은 KDoc 및 설정 표현에 한정한다. |
| 테스트 | Redis 연결 없이 YAML 파싱, 설정별 재시도 횟수와 전략 타입, 입력 인덱스 0과 1에서 계산한 지연값을 검증한다. |
| 성능 | 고정 지연 계산을 유지하며 요청 경로에 새 작업이나 할당을 추가하지 않는다. |
| 문서·근거 | KDoc의 1초와 테스트 YAML의 1.5초가 서로 다른 설정값임을 명시한다. 기존 YAML이 deprecated/미지원 키로 파싱되지 않는 점과 새 키로 유효한 fixture의 1.5초 `ConstantDelay`를 확인하는 점을 기록한다. Catalog ref와 2026-11-25 기준 공식 릴리스 관찰 및 불변 태그를 기록한다. |

## 수용 기준

- [x] 2026-11-25 기준 Redisson `4.8.0`을 최신 릴리스로 선택한 근거와 Catalog 고정 참조를 기록했다.
- [x] 대상 KDoc은 고정 지연 계약을 설명하고 테스트 YAML에서 deprecated/미지원 Redisson 키를 제거했다. `DEFAULT_RETRY_INTERVAL_MILLIS`와 자체 API인 `writeRetryInterval`의 Bluetape 식별자는 유지했다.
- [x] 고동시성 기본 설정의 1초 고정 지연을 보존했다. 테스트 YAML은 1.5초 `ConstantDelay`를 명시하며, 기존 fixture가 Redisson 4.8.0에서 파싱되지 않던 원인과 보정 범위를 기록했다.
- [x] Redis 비의존 테스트가 두 설정 경로의 `retryAttempts == 3`, `ConstantDelay` 타입, 각각의 지연값(1,000ms 및 1,500ms)을 검증한다. YAML 전략의 `calcDelay(0)`과 `calcDelay(1)`은 모두 1,500ms이며, 명령 실행 순서나 재시도 횟수와 지연 인덱스의 대응은 추론하지 않았다.
- [x] Redisson 설정 코드의 `setRetryInterval(...)` 호출 또는 `retryInterval` 프로퍼티 설정과 YAML 키 `retryInterval`이 남지 않았는지 검색했다. Bluetape 상수 `DEFAULT_RETRY_INTERVAL_MILLIS`와 자체 API `writeRetryInterval`은 보존했다.
- [x] 대상 모듈 테스트, 적용 가능한 정적 검사, `git diff --cached --check`가 통과했다.
- [x] Kotlin 패턴 기준 7-Tier 검토를 마쳤다. 인라인 검토는 P0=0/P1=0이며, 독립 리뷰는 로컬 코드 호스트를 찾지 못해 UNASSESSABLE로 기록했다.
- [x] 공개 API/ABI와 중앙 Catalog 변경이 없다.

## 구현 결과 — 2026-11-25

- `repo-test-summary -- ./gradlew :bluetape4k-redisson:test --tests "io.bluetape4k.redis.redisson.RedissonRetryDelayConfigTest"` — 2 tests PASS.
- `repo-test-summary -- ./gradlew :bluetape4k-redisson:test` — 456 tests PASS, failures=0, errors=0, skipped=0.
- `./gradlew :bluetape4k-redisson:detekt --console=plain` — exit 0; 45 Kotlin files. 진단은 변경하지 않은 기존 파일에만 남아 있다.
- `git diff --cached --check` 및 deprecated Redisson 설정 검색 — PASS.
- 7-Tier 인라인 fallback 검토에서 P0/P1은 0건이다. 독립 리뷰 시도는 `/opt/homebrew/Caskroom/codex/0.171.0/bin/codex-code-mode-host` 부재로 exact diff를 확인하지 못했고, PR 전 독립 attestation은 없다. 추적과 근거는 [구현 검토](../../review/2026-11-25-issue-1916-redisson-retry-delay-review.md)에 있다.
- 구현 review artifact는 주 세션 inline fallback verdict와 독립 reviewer의 UNASSESSABLE 출처를 구분한다. 이후 PR/hosted CI/merge 상태는 live GitHub 기록을 기준으로 갱신한다.

## 테스트 및 검증

- 새 Redis 비의존 설정 계약 테스트를 우선 실행한다.
- `bluetape4k-redisson` 모듈 테스트와 Detekt를 실행했다. Testcontainers 테스트는 저장소 지침대로 다른 모듈과 병렬 실행하지 않았다.
- 최종 diff에서 관련 경로만 변경했는지 확인하고, KDoc·테스트 설정·테스트를 서로 대조한다.
- 실제 Gradle 태스크는 `./gradlew projects` 결과의 `:bluetape4k-redisson`으로 확정했다.

## 실패 모드와 복구

- YAML tag 또는 기간 표기 파싱에 실패하면 테스트가 실패해야 한다. 오래된 fixture 속성 때문에 파싱이 실패하면 Redisson 4.8.0 JavaBean 설정과 소스를 대조해 미지원 키만 제거하고 동일한 1500ms 값을 다시 검증한다.
- 고동시성 기본 설정이 1초가 아닌 값을 반환하면 KDoc을 맞춰 바꾸지 말고 구현 원인을 먼저 확인한다. 이번 설계는 해당 동작 보존을 요구한다.
- 기존 Catalog와 다른 외부 버전이 확인되면 중앙 Dependencies 수정으로 확장하지 않는다. Projects 범위와 별도 권한을 다시 정리한다.
- 변경을 되돌릴 때는 KDoc, 테스트 설정, 테스트를 함께 되돌려 문서와 실행 설정이 다시 불일치하지 않도록 한다.

## 근거 자료

- Projects Catalog loader: `settings.gradle.kts`의 기본 Catalog ref와 ref/SHA 검증
- 중앙 Catalog: `c44ec6c47aa3a8dc3e862dbd7d041eb9d78355f3`의 `redisson = 4.8.0`
- Redisson 공식 최신 릴리스 관찰(2026-11-25): [`/releases/latest`](https://github.com/redisson/redisson/releases/latest)는 [`redisson-4.8.0` 고정 태그](https://github.com/redisson/redisson/releases/tag/redisson-4.8.0)로 이동했다.
- Redisson 4.8.0 deprecated 대체 구현: [BaseConfig.java](https://github.com/redisson/redisson/blob/redisson-4.8.0/redisson/src/main/java/org/redisson/config/BaseConfig.java)
- Redisson 4.8.0 고정 지연 계약: [ConstantDelay.java](https://github.com/redisson/redisson/blob/redisson-4.8.0/redisson/src/main/java/org/redisson/config/ConstantDelay.java)
- Redisson 4.8.0 YAML 파서의 `retryInterval` 무시 처리, `ConstantDelay` 태그와 `Duration` 해석: [ConfigSupport.java](https://github.com/redisson/redisson/blob/redisson-4.8.0/redisson/src/main/java/org/redisson/config/ConfigSupport.java)
- Projects 현행 구현과 테스트 설정: `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt`, `infra/redisson/src/test/resources/redisson.yaml`
