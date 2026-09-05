# #1634: Exposed 문서와 실제 catalog 소비 버전 맞추기

## 배경과 판단 수정

Projects의 개발 버전 `2.1.0-SNAPSHOT`, 최신 배포 버전 `2.0.0`,
JetBrains Exposed 의존성 버전은 서로 다른 값이다.
README는 Exposed `1.2.x`를 안내했고, 기존 기본 catalog `850959d0ea5f76ac7e2c442400f47653d5f95eed`는
Exposed `1.4.0`이었다. 승인된 중앙 catalog `9698c9d66bea6fcba373143ee8fa5bfbd9812d4b`의 값은 `1.5.0`이다.

Codex는 오래된 소비 값을 문서의 목표로 제안했고 버전 대상도 명확히 설명하지 않았다.
사용자의 “Exposed는 catalog의 1.5.0을 사용” 지적으로 이 판단을 수정했다.
오래된 고정값은 현재 상태의 증거이지, 승인된 업데이트 목표를 대체하는 기준이 아니다.

## 결정과 범위

- `settings.gradle.kts` 기본값과 CI의 SHA를 승인된 중앙 catalog에 함께 맞춘다.
- 로컬 `exposed` 버전 override나 중앙 catalog 복사본을 추가하지 않는다.
- 영어·한국어 README를 `1.5.0`으로 맞추고 실제 `batchInsert` 사용 예제로 연결한다.
- Exposed 기능 전체나 hashing API의 Projects 래퍼 지원을 주장하지 않는다.
- 프로젝트 릴리스 버전, 다른 저장소, 배포 설정은 변경하지 않는다.
- catalog SHA 갱신은 여러 의존성에 영향을 주므로 문서 전용 검증만으로 완료하지 않는다.

## 재발 방지 점검

1. 버전을 설명할 때 프로젝트, 외부 bluetape4k artifact, JetBrains Exposed를 구분한다.
2. 승인된 중앙 catalog와 소비 저장소의 기본 SHA, 환경·Gradle override, CI SHA를 따로 확인한다.
3. 두 README의 Exposed 값과 실제 소비 catalog의 `versions.exposed`를 비교한다.
4. 예제 Gradle 경로는 `settings.gradle.kts`의 등록 규칙이나 기존 CI에서 확인한다.
   첫 검증의 `:bluetape4k-redisson-demo`는 존재하지 않았으며, 실제 이름은
   `:bluetape4k-examples-redisson-demo`다. 수정한 명령을 실행한 뒤 문서에 기록한다.
5. workflow helper의 lane scope는 저장소 상대 경로다. 절대 경로 입력 거부 후
   상대 경로와 필수 agent/timestamp 인자로 복구하고 mutation-check를 확인한다.

## 검증

- 중앙 adoption checker: 통과. 로컬 alias catalog는 원본과 동일하며 변경하지 않았다.
- `actionlint .github/workflows/ci.yml`, `git diff --check`: 통과.
- 두 README에 catalog 갱신 시 SHA·`versions.exposed` 대조 절차를 추가했다.
- 실제 소비 버전 확인:

  ```bash
  ./gradlew :bluetape4k-examples-redisson-demo:dependencyInsight \
    --configuration testRuntimeClasspath --dependency org.jetbrains.exposed:exposed-core \
    --no-configuration-cache
  ```

  `exposed-core`, `exposed-dao`, `exposed-jdbc`, `exposed-java-time`,
  `exposed-spring-boot4-starter`, `spring7-transaction`이 모두 `1.5.0`으로 해석됐다.
- 예제 테스트: 99개 통과, 실패·오류·제외 0개(JUnit XML 집계).
- CI 의존성 그래프 계약 테스트: 17개 통과. catalog 변경은 `shared` 경로에 포함된다.
- 다운로드한 catalog의 SHA-256과 sidecar 일치:
  `622761bc3e518f052fe769c7fa057b3e1ec0cacd22ad9a871a9d1d8157120e0a`.
- README locale 값·추가 링크·settings/CI SHA 일치 검사와 한국어 용어 검사 통과.
- CI와 같은 범위의 `build -x test` 통과: 2분 21초, 674개 태스크
  (618 executed, 1 from cache, 55 up-to-date). CI와 동일하게
  `protobuf-codec-benchmark`, `serializer-benchmark`, `web-framework-benchmark`의 build를 제외했다.
  중앙 매뉴얼 루트는 `bluetape4k.github.io/docs/manual/bluetape4k-projects`를 사용했다.
- 빌드에 연결된 K3s 테스트 9개 통과. 일반 `test` 전체 실행을 대신하는 증거는 아니다.
- Gradle 10 호환성 경고와 외부 라이브러리의 JVM native/Unsafe 경고는 남아 있다.
- 아래 CI 재검증 기록은 PR 생성 이후 확인한 결과다. 병합·배포는 별도 단계다.

## CI 성공 표시와 실제 테스트 결과를 구분하기

[PR #1646의 최초 CI](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/33930261560)는
26개 job을 성공으로 표시했지만, 업로드된 JUnit에는 Elasticsearch 41개 중 4개 실패가 남았다.
Elasticsearch Gradle 명령이 실패한 뒤 NATS 188개가 통과하면서 retry 스크립트의 마지막 종료 코드가 0이 됐다.
Codex가 작성 중이던 병합 준비 문구는 로그 점검에서 폐기했고, PR에는 실제 실패를 기록했다.

- 잘못된 가정: 모든 job의 성공 표시는 모든 테스트의 성공을 뜻한다.
- 결정: 여러 명령을 실행하는 retry 블록은 `set -euo pipefail`로 첫 실패를 전달한다.
- 회귀 검사: 실제 workflow 명령을 가짜 Gradle로 실행해 Elasticsearch 실패, NATS 실패, 전체 성공을 각각 검증한다.
  수정 전 Elasticsearch 실패가 은폐됐고, 수정 후 CI 계약 테스트 18개가 통과했다.
- 향후 점검: 정확한 HEAD의 job 상태와 JUnit 실패·오류·제외 수를 대조한다.
  Nightly의 search-messaging은 단일 Gradle 호출이므로 이번 실패 은폐 결함에 해당하지 않는다.

## 컨테이너 준비와 coroutine 테스트 제한시간 분리

첫 4개 테스트는 1분 `UncompletedCoroutinesError`로 실패했다. 클래스 병렬도는 4이며,
공유 fixture의 lazy getter가 개별 `runTest` 또는 `@BeforeEach` 안에서 서버를 시작했다.
서버 readiness는 최대 3분이므로 초기화 대기가 개별 테스트의 1분 제한을 소모할 수 있었다.
로컬에서는 기존 41개가 43초 빌드 안에 통과했으며, 이 재실행 성공만으로 CI 결함이 사라졌다고 판단하지 않는다.
catalog 변경이 초기화 지연에 영향을 주었는지는 별도로 입증하지 않았다.

- 결정: 기반 fixture의 `@BeforeAll`에서 공유 서버를 준비한다. 서버 readiness, 테스트 제한시간, 병렬도는 유지한다.
- 회귀 검사: 기반 fixture의 초기화 callback과 테스트 본문 진입 전 endpoint 등록을 확인한다.
  새 JVM의 단독 실행에서 수정 전 2개 실패, 수정 후 2개 통과를 확인했다.
- 전체 모듈: `cleanTest test --no-build-cache --max-workers=1 --no-configuration-cache`에서
  43개 통과, 실패·오류·제외 0개를 JUnit XML로 확인했다.
  `detekt` 태스크는 성공했지만 변경하지 않은 생산 코드의 기존 지적 3개를 출력했다
  (`MatchingDeclarationName`, `TooGenericExceptionCaught`, `LoopWithTooManyJumpStatements`).
  기존 Python 스크립트의 `EXE001`, `SIM117` 및 전체 파일 포맷 차이도 남아 있으며 신규 lint 지적은 없다.
- 향후 점검: 검사 코드가 lazy getter를 먼저 호출해 스스로 서버를 시작하지 않도록 한다.
  느린 초기화와 실제 API 응답 시간 초과를 분리하고, 컨테이너 준비 실패를 테스트 제한시간 확대로 숨기지 않는다.
- 독립 검토: P0=0, P1=0. endpoint 검사는 JVM 전역 프로퍼티에 의존하므로 다른 테스트의 초기화가
  검사를 도울 수 있다는 P2가 남았다. 이번에는 `--tests '*ElasticsearchFixtureLifecycleTest'`의
  새 JVM 단독 RED/GREEN으로 검증했으며, 이 단독 실행을 이후 fixture 변경에서도 유지한다.
- GNO 문서 재색인: 기존 12,701개 문서 변경 없음. 현재 worktree는 `bluetape4k-docs` 수집 범위 밖이므로
  병합 후 기본 checkout 동기화 단계에서 이 lesson을 다시 색인한다.
