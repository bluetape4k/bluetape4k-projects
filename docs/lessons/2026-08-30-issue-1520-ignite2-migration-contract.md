# Issue #1520 Ignite2 마이그레이션 계약 보강 교훈

## 맥락

`Ignite2Server`의 production 동작은 유지하면서 custom image tag와 unknown
architecture fail-fast 동작을 문서·테스트·JVM descriptor 계약으로 고정했다.

## 잘못된 가정과 확인 증거

Custom tag가 canonical architecture resolver를 우회하는지 확인하려고
`GenericContainer.dockerImageName`을 단순 metadata 조회로 사용했다. 그러나 이
프로퍼티는 `RemoteDockerImage` 해석을 시작했고, 존재하지 않는
`custom/ignite:2.18.0-custom`을 pull하려다 404로 실패했다.

KDoc에 고정된 custom tag literal을 추가한 뒤에는
`scripts.test_testcontainers_contract`가 이를 stale default tag로 판정했다.
공개 예제가 정확해도 기존 문서 drift 계약을 함께 통과해야 한다는 점을 확인했다.

## 결정

- Architecture resolver 우회는 unknown architecture에서 명시적인 custom tag로
  `Ignite2Server`를 생성한 뒤, test-only reflection으로 아직 해석하지 않은
  `RemoteDockerImage.imageNameFuture`의 `DockerImageName`을 확인한다. 실제 image
  pull을 시작하는 `dockerImageName` 프로퍼티는 이 계약의 증거로 사용하지 않는다.
- Published JVM baseline은 descriptor literal만 기록하지 않는다. Maven Central의
  1.12.1 JAR을 내려받아 SHA-256을 검증하고, 해당 artifact에서 추출한 descriptor와
  현재 compile output을 직접 비교한다.
- Custom image KDoc은 `customTag` 같은 symbolic value를 사용하고, 실제 migration
  예시는 EN/KO README와 release 문서에서 `custom/ignite:2.18.0-custom`으로
  제공한다.
- KDoc을 변경하면 Kotlin test보다 먼저
  `python3 -m unittest scripts.test_testcontainers_contract -v`를 실행해 기존
  tag drift 규칙과 충돌하는지 확인한다.

## 결과와 검증

Network-triggering assertion을 제거한 뒤 targeted test 2개와
`Ignite2ServerTest` 전체 6개가 통과했다. Testcontainers 문서 계약 6개,
JVM release 계약 11개, public `invoke` descriptor 4개도 모두 통과했다.
