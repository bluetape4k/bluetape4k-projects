# #1371 NearJCache front 정리 실패 관측성 교훈

## 맥락

`NearJCache.close()`와 생성 rollback이 front cache 정리 실패를 `runCatching`으로
삼키고 있었다. listener deregister와 front close가 함께 실패하면 호출자는
실패 원인을 알 수 없었고, 성공한 `close()`도 front 정리를 매번 다시 호출했다.
Hazelcast `nearJCache` 팩토리도 생성 실패 시 같은 best-effort 정리 경계를
사용하고 있었다.

## 결정

- 정리 단계의 첫 실패를 주 예외로 전달하고 이후 실패는 `suppressed` 예외로
  연결한다.
- `close()`는 wrapper가 등록한 listener와 소유한 front cache만 닫으며, 전달받은
  back cache/provider는 닫지 않는다.
- 성공한 `close()`는 idempotent로 처리한다. front close가 실패하면 성공할 때까지
  다음 호출에서 front 정리를 재시도한다. listener deregister가 실패해도 등록
  상태를 보존해 다음 호출에서 재시도한다.
- close가 시작된 뒤에는 listener 재등록을 거부해, 닫힌 front를 다시 캡처하는
  backend listener가 남지 않게 한다.
- 생성 rollback도 동일한 예외 보존 정책을 사용하고, 로그에는
  `operation`, `cache`, `provider` 메타데이터만 기록한다.

## 결과

`NearJCache`는 listener 정리와 front 정리를 독립적으로 시도한 뒤 실패 체인을
호출자에게 보존한다. listener 정리 실패는 pending registration으로 남겨 다음
`close()`에서 재시도하며, close 이후 재등록은 거부한다. Hazelcast 팩토리는
`NearJCache` 생성 실패 뒤 front 정리 실패를 원래 예외의 `suppressed` 예외로
남긴다. 두 README와 public KDoc에 front와 back의 lifecycle 소유 경계를 명시했다.

## 검증

- RED: 기존 구현에서 cleanup 예외 전파, suppressed 보존, idempotent close 계약
  테스트 4개가 실패했다.
- GREEN: `NearJCacheContractTest` 34개 통과. listener deregister 재시도,
  combined cleanup 재시도, close 이후 재등록 거부, registration failure 로그
  metadata 회귀 테스트를 포함한다.
- `./gradlew :bluetape4k-cache-core:test --no-configuration-cache --no-build-cache
  --rerun-tasks --max-workers=1` 결과 570개 통과. 최초 캐시 미강제 실행은
  570개 통과 후 Gradle test-result `EOFException`으로 종료되어 fresh rerun으로
  확인했다.
- `TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :bluetape4k-cache-hazelcast:test
  --no-configuration-cache --no-build-cache --rerun-tasks --max-workers=1` 결과
  226개 통과. 기본 Ryuk 경로는 원격 Docker context에서 `/Users/debop/.colima/default/docker.sock`
  mount 오류가 발생해 비활성화했으며, Testcontainers/Hazelcast 계열 검증은 다른
  모듈과 병렬 실행하지 않았다.
- 두 모듈의 `detekt` 태스크는 성공했다. 출력에는 `NearJCache.kt:82`의
  `LargeClass`와 기존 테스트의 generic exception 위반이 포함됐지만, 모두 이번
  변경 hunk와 무관한 baseline 결과로 확인했다.
- `git diff --check` 통과.

## 향후 지침

새로운 resource lifecycle cleanup을 추가할 때는 `runCatching`으로 예외를
숨기지 말고, (1) 각 cleanup 단계를 독립 실행하고, (2) 첫 실패와 후속 실패를
분리해 보존하며, (3) 성공/실패 후 재호출과 close 이후 재등록 거부 정책을
테스트하고, (4) 로그에 키·값·페이로드를 넣지 않는지 확인한다. shared back cache의 tenant/owner 권한과
`getAll` residency 상한은 각각 #1368과 #1369의 별도 설계를 따른다.

## 참고

- [Issue #1371](https://github.com/bluetape4k/bluetape4k-projects/issues/1371)
- [Epic #1408](https://github.com/bluetape4k/bluetape4k-projects/issues/1408)
- `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt`
- `cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt`
