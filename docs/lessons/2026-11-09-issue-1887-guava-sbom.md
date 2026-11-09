# Issue #1887: Guava 요청 edge와 published graph 검사를 함께 수렴한다

## 맥락

Issue [#1887](https://github.com/bluetape4k/bluetape4k-projects/issues/1887)은
`db7d0b9cedc088712e91981507e16066f9e62921`의 Dependency Submission SBOM에
`com.google.guava:guava:33.6.0-jre` 요청 edge가 남았던 문제를 추적했다.
당시 로컬 `issue1883DependencyGraphCheck`는 통과했지만
`apiElements`와 `runtimeElements`가 `non-resolvable`이라는 이유로 실제
선언과 constraint를 검사하지 않았다. 대표 project의 report가 없어도
`mapNotNull`로 조용히 제외할 수 있어, 로컬 PASS가 hosted SBOM 수렴을
증명하지 못했다.

## 원인과 결정

- 중앙 catalog의 Guava 기준을 `33.7.2-jre` 이상으로 고정하고, 더 낮은
  catalog ref는 Gradle configuration 단계에서 거부한다.
- `canBeConsumed`인 non-resolvable published configuration은
  `allDependencies`와 `allDependencyConstraints`를 직접 읽어 forbidden
  requested coordinate를 검사한다. Guava는 등록된 과거 취약 버전만
  비교하지 않고 `33.7.2` 미만이거나 숫자로 판정할 수 없는 요청을 모두
  거부한다.
- 대표 project가 없어지거나 graph report가 생성되지 않으면
  `issue1883DependencyGraphCheck`가 즉시 실패한다. 누락을 빈 목록으로
  바꾸지 않는다.
- hosted SBOM은 local selected version이나 retry-green CI만으로 판정하지
  않고, exact SHA의 Dependency Submission 결과와 Dependabot alert를
  별도로 read-back한다.

## 검증

- RED: `python3.13 -m unittest -v tests.test_catalog_workflows.CatalogWorkflowsTest.test_issue_1887_guava_graph_check_inspects_published_declarations_and_reports`
  가 Guava 최소 버전 guard와 published/report 검사 부재로 실패했다.
- GREEN: 같은 회귀 테스트가 통과했다.
- 추가 GREEN: Guava 취약 버전 판정 helper와 published declaration 경로를
  포함한 회귀 계약이 통과했다.
- `./gradlew issue1883DependencyGraphCheck --no-configuration-cache --no-daemon`
  이 7개 대표 project의 `apiElements`/`runtimeElements`를
  `non-resolvable-declarations=<count>`로 기록하고 통과했다.
- exact `develop` HEAD `7c3b9f3bf5f70785be70ce095ec09e66433beaaa`의
  Dependency Submission run `42585681379` SBOM에는 Guava
  `33.7.2-jre`만 남고 `33.6.0-jre`는 없다.
- Dependabot alert `#269`은 `fixed`로 read-back됐고, `fixed_at`은
  `2026-11-09T07:25:49Z`이다. 취약 범위의 first patched version은
  `33.7.2`다.

## 놓치기 쉬운 점

Gradle의 `resolutionResult`에서 선택 버전이 패치됐다는 사실과, hosted
Dependency Submission이 원본 요청 edge를 제출하지 않았다는 사실은 같은
증거가 아니다. 특히 published configuration은 해석할 수 없으므로
선언/constraint 검사와 hosted SBOM read-back을 각각 유지해야 한다.

## 다음 변경자를 위한 guard

새 plugin 또는 published module을 representative list에 추가할 때는
`issue1883ProjectDependencyGraphCheck`가 생성하는 report 존재 여부와
published declaration count를 함께 확인한다. 중앙 catalog 버전을 올릴
때도 `bt4kVersion("guava")`의 최소 버전 guard를 우회하지 말고, exact SHA
Dependency Submission과 alert 상태를 다시 읽는다.
