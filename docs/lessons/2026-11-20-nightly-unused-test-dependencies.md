# Nightly 테스트 의존성 해석 실패 교훈

## 맥락

2026-11-20 Nightly run [44717633879](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/44717633879)은
merge commit `71be777ceb7a50fd04cde6762e8484435d0bab30`에서 테스트 의존성을
해석하지 못해 실패했다. 후속 Snapshot run [44723334201](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/44723334201)은
Nightly 성공 조건을 만족하지 못해 `skipped` 상태가 됐다.

`Test / Data (rdb)` job `136227283323`은
`:bluetape4k-jdbc:compileTestKotlin` 단계에서 Maven Central의
`org.mockito:mockito-core:5.23.0` 다운로드가 HTTP 429를 반환해 실패했다.
`data/jdbc/src`에는 Mockito 또는 MockK 참조가 없었지만,
`data/jdbc/build.gradle.kts`의 `spring-boot-starter-test`가
`mockito-junit-jupiter`를 거쳐 `mockito-core`를 전이 의존성으로 끌어왔다.

`Test / Spring Boot (core-redis-r2dbc)` job `136227283216`은
`:bluetape4k-spring-boot-core:compileTestKotlin` 단계에서
`org.apache.httpcomponents.client5:httpclient5-testing:5.6.5`를 저장소에서
찾지 못해 실패했다. `spring-boot/core/src`에는 Apache HttpComponents 테스트
참조가 없었고, `spring-boot/core/build.gradle.kts`가 해당 테스트 아티팩트를
직접 선언하고 있었다.

## 결정

- `data/jdbc`의 `spring-boot-starter-test`에서
  `org.mockito:mockito-core`와 `org.mockito:mockito-junit-jupiter`를 모두
  제외한다. Spring Boot dependency management가 `mockito-core`를 별도로
  선택할 수 있으므로 두 항목을 함께 고정한다. JUnit 관련 기존 제외 규칙은
  유지한다.
- `spring-boot/core`에서 사용하지 않는 `httpclient5.testing` 직접 테스트
  의존성을 제거한다.
- 소스 코드, 공개 API, ABI, 런타임 의존성은 변경하지 않는다. 테스트
  클래스패스에서 실제로 사용하지 않는 외부 테스트 라이브러리만 정리한다.

## 결과

두 모듈 모두 테스트 컴파일이 성공했고, 같은 `dependencyInsight` 조회에서
`mockito-core`와 `httpclient5-testing`이 더 이상 선택되지 않았다. 추가
클래스패스 조회에서도 Mockito 아티팩트가 남지 않았다. JDBC 테스트 170개와
Spring Boot core 테스트 336개가 모두 통과했다. 실패 0개, 오류 0개, skip
0개였으며 두 모듈의 Detekt도 통과했다.

## 검증

| 검사 | 결과 |
|---|---|
| TDD RED | 수정 전 `dependencyInsight`에서 `mockito-core:5.23.0`과 `httpclient5-testing:5.6.5`가 각각 `testCompileClasspath`에 존재했다. |
| 의존성 그래프 GREEN | 수정 후 `mockito-core`, `mockito-junit-jupiter`, `httpclient5-testing` 조회가 모두 `No dependencies matching given input were found`를 반환했다. |
| 테스트 컴파일 | `./gradlew :bluetape4k-jdbc:compileTestKotlin :bluetape4k-spring-boot-core:compileTestKotlin --no-daemon --no-configuration-cache --console=plain` — 성공 |
| JDBC 테스트 | `./gradlew :bluetape4k-jdbc:test --no-daemon --no-configuration-cache --no-parallel --max-workers=1 --console=plain` — 170개 통과 |
| Spring Boot core 테스트 | `./gradlew :bluetape4k-spring-boot-core:test --no-daemon --no-configuration-cache --no-parallel --max-workers=1 --console=plain` — 336개 통과 |
| 정적 분석 | `./gradlew :bluetape4k-jdbc:detekt :bluetape4k-spring-boot-core:detekt --no-daemon --no-configuration-cache --console=plain` — 성공 |
| 패치 형식 | `git diff --check` — 성공 |

테스트컨테이너 검증은 Colima가 실행 중이고 Docker context가 `default`인지
확인한 뒤 모듈별로 순차 실행했다.

## 놓친 점과 주의사항

소스에서 Mockito를 호출하지 않는다는 사실만으로는 Mockito가 빌드에 영향을
주지 않는다고 판단할 수 없다. Gradle은 테스트 컴파일 전에 전이 의존성을
해석하므로, 사용하지 않는 라이브러리도 저장소 장애나 rate limit 때문에
Nightly를 실패시킬 수 있다. 반대로 직접 테스트 아티팩트를 제거할 때는 현재
소스 참조와 의존성 그래프를 함께 확인해야 한다.

## 향후 지침

- Spring Boot 테스트 starter를 추가하거나 변경할 때
  `testCompileClasspath`에 대한 `dependencyInsight`와 전체 `dependencies`
  조회로 불필요한 mocking 프레임워크와 어댑터가 선택되는지 확인한다.
- 테스트 전용 아티팩트는 먼저 `src`의 실제 참조를 검색하고, 참조가 없으면
  직접 선언하지 않는다.
- Nightly 실패를 읽을 때는 의존성 해석 실패와 그에 따른 Coverage 또는
  Snapshot skip을 별도 원인으로 구분한다. 수동 Snapshot 발행으로 실패한
  Nightly의 성공 조건을 우회하지 않는다.

## 문서 SPW 감사

- SPW-01: PASS — Nightly run, 실패 job, merge commit, 두 build script, 소스
  검색과 Gradle 출력으로 사실을 대조했다.
- SPW-02: PASS — 맥락, 결정, 결과, 검증, 놓친 점, 향후 지침을 기록했다.
- SPW-03: PASS — 한국어 기술 문장을 사용하고 명령, 버전, 식별자, 정확한
  오류와 링크를 보존했다.
- SPW-04: PASS — `mockito-core`, `mockito-junit-jupiter`,
  `httpclient5-testing`, `testCompileClasspath` 용어를 문서 전체에서 같은
  의미로 사용했다.
- SPW-05: PASS — 최종 Markdown을 다시 읽고 KO-01~KO-07 자연스러움 및 용어
  감사를 실행했다.
