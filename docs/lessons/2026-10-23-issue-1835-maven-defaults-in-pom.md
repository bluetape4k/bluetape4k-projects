# Maven POM 기본값과 dependencyManagement 중복 비교

관련 이슈: [#1835](https://github.com/bluetape4k/bluetape4k-projects/issues/1835)

## 맥락과 놓친 점

Spring dependency-management 플러그인은 관리 의존성을 생성된 Maven POM에 추가한다. 같은 관리 좌표를 다른 `pom.withXml` 설정에서도 추가하면, Maven에서 `type` 생략과 `<type>jar</type>`는 같은 기본값이지만 XML fingerprint는 서로 다른 요소로 계산한다. `classifier`도 생략과 빈 요소가 같은 값인데, 기존 fingerprint는 두 표현을 충돌로 판정했다. Maven POM은 `type` 기본값을 `jar`로 정의하고, Spring 플러그인은 dependency-management 설정을 POM에 반영한다 ([Maven POM](https://maven.apache.org/pom.html), [Spring Dependency Management Plugin](https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/)).

첫 TestKit 실행은 제품 코드에 도달하기 전에 실패했다. `withTestKitDir(...)`가 지정한 별도 Gradle 사용자 홈에 Spring 플러그인이 없어 `--offline`로 해석할 수 없었다. [GradleRunner 문서](https://docs.gradle.org/current/javadoc/org/gradle/testkit/runner/GradleRunner.html)는 이 디렉터리를 TestKit 하위 빌드의 Gradle 사용자 홈으로도 사용한다고 설명한다. 처음에는 온라인 해석으로 결함을 재현했지만, 독립 리뷰에서 fixture가 `1.1.7`을 중앙 catalog와 별도로 고정하고, TestKit이 네트워크에 묶이며, 최종 POM assertion이 플러그인 생성 항목을 충분히 증명하지 못하는 점을 찾았다.

## 결정

중복 키에 포함된 최상위 `type`과 `classifier`는 fingerprint에서 제외한다. 두 필드는 정규화된 Maven 키가 소유하므로 같은 기본값 표현은 하나로 합치고, 서로 다른 값은 별도 좌표로 남긴다. fingerprint의 나머지 구조는 보존해 `version`, `scope`, `optional`, `exclusions`가 다른 같은 좌표는 충돌로 거부한다.

통합 fixture는 `settings.gradle.kts`가 고정한 중앙 catalog에서 Spring 플러그인 버전을 읽는다. buildSrc 테스트는 `@Classpath`가 선언된 `CommandLineArgumentProvider`로 artifact classpath를 Gradle `Test` task에 전달하고, TestKit에는 `withPluginClasspath(...)`로 주입한다 ([Gradle cacheable integration tests](https://docs.gradle.org/9.8.1/userguide/caching_java_projects.html#sec:integration_test_execution)). 하위 빌드는 `--offline`로 실행한다. 생성 POM에서는 관리 의존성의 `version`이 `1.0`인지, 기본값인 `type`과 `classifier` 요소가 생략됐는지 검사해 실제 플러그인 항목이 살아남았음을 확인한다. 단위 테스트는 기본값 표현의 중복 제거와 type/classifier가 다를 때의 별도 보존도 각각 확인한다.

## 결과

Maven 기본값 표현만 다른 중복은 하나로 정리된다. 서로 다른 Maven 좌표와 버전·scope·exclusions 충돌은 기존 계약대로 보존하거나 명시적으로 실패한다. 실제 dependency-management 플러그인, production 후처리, `GenerateMavenPom` 산출물을 함께 검증하며 TestKit 자체는 네트워크에 의존하지 않는다. 저장소 기본 configuration cache 설정에서 `doFirst`가 build script의 `Configuration`을 캡처해 task 복원에 실패하는 문제도 재현했다. classpath를 `@Classpath` 입력을 가진 argument provider로 전달해 strict cache 검사와 재사용을 통과시켰다.

## 검증

- 수정 전 fixture는 `com.example:library@jar`의 `type`·`classifier` 표현 차이로 `conflicting dependencyManagement entries`를 재현했다.
- `./gradlew :buildSrc:test --rerun-tasks --configuration-cache --configuration-cache-problems=fail`: 42 tests, failures/errors/skips 0; configuration cache 저장과 후속 재사용 모두 성공.
- `--no-configuration-cache`로 집계 POM·metadata 작업 통과: `generatePomFileForBluetape4kPublication`, `checkPomFileForBluetape4kPublication`, `generateMetadataFileForBluetape4kPublication`. 같은 작업을 cache-enabled로 실행하면 Gradle이 `GenerateMavenPom` task graph에서 cache serialization 문제 648건을 보고하고 항목을 폐기하므로 cache 검증 근거로 세지 않는다.
- publication audit 4종 통과: 22 runs, 50 assertions, failures/errors/skips 0.
- `ruby scripts/publication/validate_poms.rb`: 82 files, 33,110 dependencies, Maven models 82, failures 0.
- `ruby scripts/publication/validate_module_metadata.rb`: 82 files, 171 variants, 1,574 dependencies, failures 0.

## 향후 지침

- 중앙 catalog가 고정된 저장소에서 외부 플러그인을 쓰는 TestKit fixture는 catalog SHA의 실제 버전과 artifact classpath를 재사용한다. 하위 Gradle build에는 플러그인 classpath를 주입하고 오프라인 실행을 유지한다.
- 생성 산출물 검사는 개수만 보지 말고, 플러그인이 만든 항목의 의미 필드와 기본값 요소 생략 여부를 확인한다.
- fingerprint에서 필드를 제외할 때는 그 필드가 중복 키에 포함되는지 확인하고, 기본값 동등성과 서로 다른 좌표 보존을 각각 테스트한다.
- TestKit setup 단계의 실패는 제품 실패로 분류하지 않는다. 플러그인 해석과 assertion 대상까지 도달했는지 확인한 뒤 결함을 재현한다.
- TestKit에 의존성 classpath를 전달할 때 task action에서 `Configuration`을 직접 해석하지 않는다. `@Classpath` 입력을 가진 `CommandLineArgumentProvider`를 사용하고 strict configuration cache 실행과 캐시 재사용을 확인한다.
- 합성 POM 파일을 반환하는 테스트 helper의 호출자는 성공·실패 경로 모두에서 임시 파일 삭제를 보장한다. jar fixture에는 `compile`, pom BOM fixture에만 `import` scope를 사용한다.
- buildSrc가 중앙 TOML alias를 정규식으로 읽는 방식은 현재 catalog 형식에 맞지만 줄 배치 변경에 민감하다. 해당 형식이 바뀌면 catalog parsing 방식을 함께 갱신하고 실패 동작을 확인한다.
