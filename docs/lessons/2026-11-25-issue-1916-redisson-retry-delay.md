# Redisson 4.8.0 YAML 재시도 지연 계약 교훈

## 배경

이슈 [#1916](https://github.com/bluetape4k/bluetape4k-projects/issues/1916)에서 Redisson 4.8.0 `BaseConfig.setRetryInterval(int)`의 deprecated 사용과 재시도 설정 설명을 조사했다. 구현 코드는 이미 `retryDelay = ConstantDelay(1초)`를 사용했지만, test YAML은 오래된 설정 이름과 Redisson 4.8.0에 없는 속성을 포함했다.

## 발견

- Redisson 4.8.0 `ConfigSupport.CustomPropertyUtils`의 무시 목록에는 `retryInterval`이 있다. 그러나 실제 `Config.fromYAML` 실행은 해당 이름 처리 중 `Property.isWritable()` 호출에 null property가 전달되어 예외를 낸다. 소스의 ignored-property 선언만으로 설정 파일이 성공적으로 파싱된다고 판단하면 안 된다.
- 기존 fixture에서는 `pingTimeout`이 먼저 같은 parser 오류를 일으켰다. `reconnectionTimeout`, `failedAttempts`, `dnsMonitoring`, `useLinuxNativeEpoll`도 `BaseConfig`, `SingleServerConfig`, `Config`의 JavaBean 속성에 없었다.
- `Config.getSingleServerConfig()`는 protected이므로 Kotlin 테스트에서 `config.singleServerConfig`로 읽을 수 없다. 파싱된 현재 설정은 public `config.useSingleServer()`로 가져온다.
- 테스트 fixture의 `getResource` 결과는 전제 검사 `requireNotNull` 대신 Bluetape assertion `shouldNotBeNull()`로 검증해 실패를 테스트 assertion으로 남긴다.
- 모듈 디렉터리에서 Gradle project 경로를 파일 경로로 추론하지 않는다. 이 저장소의 실제 모듈 task 이름은 `./gradlew projects` 출력에서 `:bluetape4k-redisson`으로 확인했다.

## 결정

- test YAML의 deprecated `retryInterval`은 `retryDelay: !<org.redisson.config.ConstantDelay> { delay: PT1.5S }`로 바꾼다.
- Redisson 4.8.0에서 지원하지 않는 오래된 fixture 속성은 새 API로 임의 변환하지 않고 제거한다. `reconnectionTimeout`을 `reconnectionDelay`로 바꾸면 전략 의미를 새로 정하게 되므로 이번 test fixture에서는 생략했다.
- 제품 설정의 1초 `ConstantDelay`, `DEFAULT_RETRY_INTERVAL_MILLIS` 공개 이름과 값, `RedissonCacheConfig.writeRetryInterval`, API/ABI, Catalog 버전은 유지한다.
- Redis 연결 없이 `Config.fromYAML` 결과의 `retryAttempts`, `ConstantDelay` 타입, `calcDelay(0/1)` 값을 검증한다.

## 검증

- TDD RED: 처음 제시된 `:bluetape4k-infra-redisson` 경로는 실제 프로젝트 목록에 없어 태스크 선택에서 실패했다. 올바른 경로를 확인했다.
- TDD RED: YAML 계약 테스트는 `singleServerConfig` protected 접근 컴파일 오류를 먼저 드러냈다. 공개 `useSingleServer()`로 고쳐 실행했다.
- TDD RED: 기존 fixture는 `pingTimeout`, `retryInterval` 및 기타 미지원 속성에서 parser 예외를 냈다. 지원 속성 목록을 대조해 fixture를 정리한 뒤 목표 계약을 통과했다.
- Targeted GREEN: `repo-test-summary -- ./gradlew :bluetape4k-redisson:test --tests "io.bluetape4k.redis.redisson.RedissonRetryDelayConfigTest"` — 2 tests PASS.
- Module GREEN: `repo-test-summary -- ./gradlew :bluetape4k-redisson:test` — 456 tests PASS, failures 0, errors 0, skipped 0.
- Static analysis: `./gradlew :bluetape4k-redisson:detekt --console=plain` — BUILD SUCCESSFUL, 45 Kotlin files. 기존 진단은 수정하지 않은 네 파일에서만 출력됐다.
- `git diff --cached --check` — PASS; staged 구현은 네 파일이다.

## 재발 방지

Redisson YAML 설정을 수정할 때는 source의 `ignoredProperties`만 읽지 말고 실제 `Config.fromYAML` 계약 테스트를 실행한다. 오류가 나면 parser가 지목한 property와 고정된 Redisson 버전의 JavaBean getter/setter를 대조한다. 유지해야 할 의미가 확인되지 않은 오래된 timeout/deprecated 값을 새 전략으로 임의 치환하지 않는다. Gradle 명령은 `./gradlew projects`에서 task path를 확인하고, Java protected getter는 Kotlin synthetic property처럼 접근하지 않는다.

## 리뷰 과정에서 확인한 guard

독립 코드 리뷰어가 사용할 로컬 host 실행 파일을 찾지 못해 exact diff verdict를 내지 못했다. workspace router는 이 경우 main-session inline fallback을 요구한다. fallback은 독립 attestation이 아니며, 가능한 경우 정확한 diff hash와 source를 기록하고 P0/P1을 수렴한다. 계획에서 독립 리뷰 실행만을 PR의 필수 조건으로 두면 router와 충돌하므로, inline review PASS 이후 PR 단계로 진행하되 hosted CI, live review/thread, merge 경계를 별도로 유지한다. workflow receipt가 없는 과거 agent 시도에 terminal lane을 소급 생성하지 않는다.

이번 검토에서는 새 테스트의 `requireNotNull` 사용도 발견했다. 테스트 기대값은 프로젝트의 `shouldNotBeNull()`로 검증하고, 반환된 non-null 값에 이어 assertion을 적용한다.
