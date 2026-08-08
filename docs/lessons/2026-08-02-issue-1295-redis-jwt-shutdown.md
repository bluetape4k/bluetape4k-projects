# 이슈 #1295 Redis 기반 JWT 종료 통합 검증 교훈

## 배경

Issue #1276에서 JWT key-chain/provider의 timer를 명시적으로 닫을 수 있게 한
뒤에도 Redis/Redisson 실제 경로와 proxy 장애 복구는 검증되지 않았다. Issue
#1295는 이 공백을 milestone `1.12.0`에서 닫기 위한 test-only 통합 작업이다.

## 결정

- `gradle/libs.versions.toml`의 기존 `testcontainers-toxiproxy` alias를
  `utils/jwt`의 `testImplementation`으로만 연결했다.
- `RedisServer`와 `ToxiproxyServer`를 `Network.newNetwork()`에 함께 배치하고,
  `0.0.0.0:8666 -> redis:6379` proxy endpoint를 host-mapped Redisson address로
  사용했다.
- Redisson config의 timeout/connectTimeout을 500ms, retryAttempts를 0으로
  낮춰 proxy disable 중의 failure가 bounded하도록 했다. 이는 실제 장애를
  빠르게 재현하기 위한 test-only 설정이며 production defaults를 바꾸지 않는다.
- `RedissonJwtProvider`는 delegate와 cache를 빌려 쓰고, `DefaultJwtProvider`가
  rotation timer를 소유한다. `RedisKeyChainRepository`는 refresh timer만
  소유하며 주입받은 client를 닫지 않는다. wrapper close, delegate close,
  repository close, application owner의 client shutdown 순서를 테스트와 README
  두 locale에 동일하게 기록했다.

## 검증 근거

- RED: direct ToxiProxy dependency를 제거한 상태에서 새 test source set의
  `compileTestKotlin`이 실패했다. 이는 `bluetape4k-testcontainers`의
  `compileOnly` 의존성이 JWT 테스트에 transitive하게 노출되지 않는다는
  classpath 경계를 확인했다.
- GREEN: `./gradlew :bluetape4k-jwt:test --tests
  io.bluetape4k.jwt.provider.cache.RedisJwtShutdownIntegrationTest
  --rerun-tasks --no-build-cache`가 Docker에서 1 passing으로 완료했다.
- 테스트는 정상 Redis JWT parsing, forced rotation, ToxiProxy disable 중
  bounded false 결과, enable 후 recovery, 반복 close, delegate close 후
  expired key-chain 재회전 억제, client의 명시적 terminal shutdown을 검증한다.

## 재사용 가드

실제 Redis를 사용하는 borrowed-client fixture에서는 component close가 외부
client close를 의미하지 않는다. integration test는 proxy/container/client를
각각 소유하고, Testcontainers 실행은 모듈·worktree 간에 순차적으로 수행해야
한다. Docker가 없는 hosted CI에서는 이 통합 행을 성공으로 추정하지 말고
`PENDING`으로 보고한다.

## 후속 작업

로컬에서 표적/모듈 테스트, detekt, 문서 parity, diff-check, runtime
dependency-scope 검사를 통과했다. ToxiProxy dependency는
`testCompileClasspath`에만 나타나며 `runtimeClasspath`에는 나타나지 않는다.
정확한 PR head/CI/review 근거는 PR 전달 단계에서 추가한다. Production
ownership/API 변경은 이 교훈의 범위가 아니다.
