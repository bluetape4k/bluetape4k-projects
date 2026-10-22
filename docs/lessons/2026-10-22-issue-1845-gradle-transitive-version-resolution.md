# Issue #1845: Catalog 별칭은 전이 버전을 제한하지 않는다

## 현상

중앙 `bt4k` catalog에 `org.bouncycastle:bcprov-lts8on:2.73.13` 별칭이 있어도,
`io.nats:jnats:2.26.2`가 요청한 `2.73.10`은 자동으로 바뀌지 않았다.
수정 전 `:bluetape4k-nats:dependencyInsight --dependency bcprov-lts8on
--configuration runtimeClasspath` 결과는 `jnats` 경로에서 `2.73.10`을 선택했다.

## 결정

- Version catalog는 의존성을 선언할 때 사용할 alias와 버전을 제공한다. 다른 의존성이
  요청한 전이 버전을 전체 빌드에 자동 적용하는 constraint는 아니다.
- 이 라이브러리는 Maven 사용자도 지원하므로 `constraints {}`만으로는 충분하지 않다.
  Gradle dependency constraint는 Gradle Module Metadata로 전달되지만 Maven POM에는
  동등한 constraint가 표현되지 않는다. NATS 모듈은 중앙 alias를 `runtimeOnly`로 직접
  선언해 Gradle과 Maven 소비자 모두에게 패치된 런타임 버전을 요청한다.
- 중앙 catalog SHA와 alias만 수정하지 않는다. 소비 모듈의 실제 resolved graph와 생성된
  POM 및 Gradle Module Metadata를 확인한다.

## 재발 방지와 검증

보안 업데이트의 catalog alias가 존재하는지만으로 전이 의존성이 고쳐졌다고 판단하지 않는다.
수정 전후 `dependencyInsight`의 선택 버전과 이유를 비교하고, 게시 라이브러리라면 생성된
POM과 `module.json`에 런타임 dependency 좌표·버전이 반영됐는지 확인한다. 로컬 그래프와
metadata는 GitHub alert의 종료를 증명하지 않으므로 병합 후 dependency submission과 alert
상태를 별도로 확인한다.

## 참고

- [Gradle dependency constraints](https://docs.gradle.org/current/userguide/dependency_constraints.html)
- [Gradle Module Metadata](https://docs.gradle.org/current/userguide/publishing_gradle_module_metadata.html)
