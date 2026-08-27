# Testcontainers release gate의 대표 테스트 선택자 lesson

## 맥락

Full Nightly run `33049975063`가 exact head
`45260871f58433a78f2d633c235010f661d22c6e`에서 LocalStack·RedisCluster
family를 `blocked`로 집계했다. 두 Gradle 명령은 `BUILD SUCCESSFUL`로 끝났지만
release-required JUnit 증적은 `tests=3, skipped=1`이었다.

## 원인과 결정

두 테스트 클래스에는 플랫폼 제약으로 의도적으로 비활성화한 메서드가 하나씩
있다. 클래스 전체를 `--tests`로 선택하면 대표 테스트의 성공과 무관한 skipped
메서드가 JUnit suite에 포함된다. release gate의 fail-closed `skipped=0` 계약은
완화하지 않고, manifest에 `testSelector`를 명시해 대표 메서드만 실행한다.

이 선택자는 `testPattern` 아래의 하나의 구체적인 `@Test` 메서드를 가리켜야
하며 validator가 클래스 경계, 제어 문자, wildcard, 실제 Kotlin 테스트 선언을
검사한다. 선택자가 없는 family는 기존 클래스 전체 선택 동작을 유지한다.

## 결과

LocalStack `run S3 Service`와 RedisCluster `create redis cluster server`를
명시적으로 선택하면 두 family 모두 JUnit `tests=1, skipped=0`과
`release_gate=true`를 생성한다. 모든 테스트를 실행하는 일반 모듈 경로의
플랫폼별 비활성화 테스트 의미는 바뀌지 않는다.

## 검증

- Python 계약 테스트 78개 통과
- `actionlint`, JVM release contract, `git diff --check`, manifest JSON 검증 통과
- 격리 worktree의 LocalStack·RedisCluster end-to-end gate 각각 `1/1` 성공
- Hosted PR CI 및 Full Nightly 재실행은 PR head에서 대기

## 향후 지침

release-required Testcontainers 클래스에 의도적인 `@Disabled` 메서드를 추가할
때는 실행 가능한 대표 메서드를 `testSelector`로 함께 등록한다. all-disabled
family를 release inventory에 남겨야 한다면 `releaseRequired=false` 지원 전용
경계와 별도 증적을 사용하며, JUnit skipped 허용 범위를 전역으로 넓히지 않는다.
