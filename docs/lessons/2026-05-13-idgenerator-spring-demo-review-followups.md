# idgenerator Spring Boot demo review follow-ups

## 배경

PR #422 merge 이후 review에서 blocking runtime issue는 없었지만, 새 Spring Boot idgenerator demo에
작은 follow-up이 드러났다.

- `IdGeneratorEntry`는 supplier lambda가 value semantics가 아니라 implementation detail인데도
  `data class`였다.
- `IdGeneratorProperties`는 request가 service layer에 도달할 때까지 invalid batch-size 조합을 허용했다.
- 당시 demo module main-source KDoc은 한국어였고, 그 시점 repository policy는 contributor-facing
  public KDoc을 영어로 기대했다.

## 결정

- REST response DTO는 data class로 유지하되, behavior나 private supplier를 감싸는 registry entry는
  plain class로 둔다.
- 잘못된 값이 모든 정상 request를 나중에 실패하게 만들면 configuration-property invariant를 construction time에 검증한다.
- 당시 follow-up은 demo module main-source KDoc을 contributor-facing code documentation으로 보고 영어로 작성했다.
  2026-07-28 이후 workspace policy는 public/internal Kotlin KDoc을 Korean-first로 정리했으므로,
  새로 수정하는 KDoc은 현재 정책에 따라 한국어로 작성한다.
- 머지 후 리뷰가 보정 PR을 만들면 같은 PR에 lesson document를 포함해 이후 에이전트가 리뷰 결과를 찾을 수 있게 한다.

## 결과

PR #426은 PR #422 review follow-up을 반영했다.

- `IdGeneratorEntry`는 더 이상 ID supplier 주변에 generated `copy`/component/equality semantics를 노출하지 않는다.
- `IdGeneratorProperties`는 non-positive 또는 inconsistent batch limit을 거부한다.
- Spring Boot idgenerator demo main-source KDoc은 당시 정책에 따라 영어로 정리됐다. 현재 기준으로 같은 KDoc을 다시 수정하면 한국어로 작성한다.
- Invalid batch limit combination에 대한 regression coverage가 추가됐다.

## 검증 근거

```bash
repo-test-summary -- ./gradlew :idgenerator-spring-boot-demo:compileKotlin :idgenerator-spring-boot-demo:compileTestKotlin :idgenerator-spring-boot-demo:test --parallel
```

결과: 8 tests passing.

```bash
git diff --check
```

결과: whitespace error 없음.

## 향후 지침

Example module에서는 merge 전에 registry/helper holder class가 accidental data class semantics를 갖는지 review한다.
Class가 lambda, resource, generator 또는 다른 behavior를 감싸면 value equality와 `copy`가 명시적 contract의 일부가 아닌 한
plain class를 선호한다.

Configuration property는 invalid value 때문에 default endpoint behavior가 사용할 수 없어지는 경우 fail fast한다.
각 invariant마다 direct regression test를 추가한다.

머지 후 리뷰 보정은 코드 변경과 lesson 항목을 같은 후속 PR에 포함한다.
