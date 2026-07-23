# Issue #756 Fory buffer 경계와 증거 기반 승격

## Context

PR #1072의 Lettuce caller-owned `ByteBuf` 경로에는 JDK, Kryo, Jackson 2/3만 포함되었고
Fory/FastFory는 안전성과 allocation 이득이 확인되지 않은 fallback으로 남았다. 후속 작업은
raw Fory/FastFory에 한정해 Lettuce encode와 Redisson decode/encode 후보를 서로 독립적으로
검증했다.

## Decision or Finding

- Lettuce encode는 Fory의 `OutputStream` API를 bounded absolute-index `ByteBuf` writer에
  연결한다. 성공 시에만 `writerIndex`를 commit하며 실패 시 caller 상태를 보존한다.
- Redisson decode는 readable range와 정확히 일치하는 single-component read-only NIO view만
  사용한다. Heap/direct 여부가 아니라 NIO component 수와 view 생성 가능성이 correctness
  gate다. Composite/non-NIO 입력은 copied compatibility path를 유지한다.
- Redisson encode는 production 구현 전에 별도 feasibility probe로 판정했다. Fory는
  `232 -> 272 B/op`, FastFory는 `216 -> 272 B/op`로 두 run 모두 allocation이 증가했으므로
  구현하지 않았다.
- Benchmark 결과는 cell별 terminal disposition으로 고정한다. 두 canonical run이 모두 gate를
  통과한 cell만 `accepted`, correctness-only copied route는 `fallback`, gate 실패는
  `rejected`로 남긴다.

## Outcome

- Lettuce raw Fory/FastFory heap/direct encode 4개 cell이 `accepted`다.
- Redisson raw Fory/FastFory direct decode 2개 cell이 `accepted`다.
- Redisson heap decode 2개 cell은 `rejected`, composite decode 2개 cell은 `fallback`이다.
- Redisson encode 2개 backend는 feasibility 단계에서 `rejected`되어 기존 allocating 경로를
  유지한다.
- Chart에는 accepted 6개 cell만 표시하며, `99.9995%` label은 반올림된 allocation 감소율이지
  zero-allocation 또는 zero-copy 주장이 아니다.
- Fory/FastFory의 old-write/new-read와 new-write/old-read는 4/4 통과했다. Redis가 없어서
  rollback smoke는 `codec-level`, `status=limited`, `publicationGate=blocked`로 기록했다.

## Verification

- `:bluetape4k-io`, `:bluetape4k-lettuce`, `:bluetape4k-redisson`의 test와
  순차 `check`/`build`가 통과했다.
- Canonical evidence는 20개 JMH method, module별 고정 JAR, A/B 두 run, `gc.alloc.rate.norm`,
  throughput guard를 검증했다.
- Aggregate manifest SHA-256은
  `68f81d30c406ab24770127b92c4bef2a11ebfc66169a7ccf648d02d7efd50aae`다.
- Compatibility runner는 known-good Maven Central JAR을 exact coordinate/SHA-256으로
  checksum-gate하고, current input commit/tree와 모든 directory classpath content hash를
  기록한다.
- 여섯 독립 관점의 최종 검토에서 P0=0, P1=0을 확인했다. 운영, developer/API,
  caller/user 관점의 수정 후 재검토도 승인되었다.

## Review Dispositions

- NIO view capability probe가 `Throwable`을 copied route로 전환하는 동작은 fatal failure를
  가릴 수 있다는 P2가 남았다. 현재 명세와 regression test가 기존 broad normalization을
  의도적으로 보존하므로 이 PR에서 예외 taxonomy를 바꾸지 않는다. Fatal/control failure
  현대화는 별도 보안 slice로 다룬다.
- Aggregate validator의 ancestry/changed-path proof가 manifest assertion에 의존한다는 P2가
  남았다. 이번 결과는 독립 검토에서 20개 raw method를 재계산하고 measurement 이후 production
  path가 바뀌지 않았음을 확인했다. 다음 evidence runner에서는 Git ancestry와 실제 changed
  path를 validator가 직접 계산하게 한다.
- Canonical argv에 절대 worktree 경로가 포함된 P3는 credential 유출이 없고 raw evidence
  변경이 hash authority를 깨므로 유지한다. 이후 runner는 `$REPOSITORY_ROOT`로 정규화한다.
- Feature flag와 per-call telemetry가 없다는 운영 잔여 위험은 문서화했다. Rollback은 artifact
  version/hash downgrade이며, live Redis smoke가 통과하기 전 publication gate는 열리지 않는다.

## Future Guidance

1. Backend capability를 API 존재만으로 승격하지 말고 transport/storage별 correctness와
   allocation gate를 분리한다.
2. Feasibility가 실패한 candidate는 production code를 만들지 않는다.
3. Fory 내부 `MemoryBuffer`가 남는 한 handoff array 제거를 zero-copy로 표현하지 않는다.
4. Evidence script는 generated output을 제외한 clean input commit/tree를 확인하고, directory
   classpath도 content hash로 고정한다.
5. Codec-only fallback은 진단 자료일 뿐 Redis rollback smoke의 대체 성공이 아니다.
6. Fory와 FastFory는 wire-incompatible mode이므로 mode 전환에는 migration 또는 eviction이
   필요하고, registration-off decode는 trusted payload에만 사용한다.
