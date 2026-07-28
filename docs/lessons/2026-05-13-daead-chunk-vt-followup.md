# DAEAD Chunk and Virtual Thread Follow-up Lessons

## 배경

PR #242, #264, #271, #293 merge 이후 review에서 DAEAD chunk format이 각 chunk를
독립적으로 authenticate하지만 chunk order와 final-frame marker를 묶지 않는다는 점이 발견됐다.
같은 review는 Virtual Thread test가 direct fork/join flow만 다루고
`bluetape4k-junit5` stress harness를 사용하지 않는다는 점도 확인했다.

## 결정 또는 발견

Security-sensitive chunked encryption은 각 ciphertext payload만이 아니라 stream structure까지
authenticate해야 한다. Per-frame DAEAD associated data에는 최소한 caller-provided associated data,
monotonically increasing chunk index, final-frame state가 포함되어야 한다. Test는 reorder,
duplicate, whole-frame truncation, trailing-data-after-final case를 포함해야 한다.

Virtual Thread와 `StructuredTaskScope` 변경에서는 direct example만으로 부족하다. Behavior가
concurrency, coroutine scheduling, virtual-thread propagation에 의존하면 `StructuredTaskScopeTester`,
`MultithreadingTester`, `SuspendedJobTester` coverage를 추가한다.

## 결과

- DAEAD chunk frame은 final flag를 갖고 chunk index/final state를 DAEAD associated data에 묶는다.
  v2 frame format은 이전 length-only frame과 의도적으로 wire-compatible하지 않다.
- DAEAD test는 reordered, duplicated, final-frame-dropped, trailing-data-after-final stream을 거부한다.
- `StructuredTaskScopeTester` coverage가 `StructuredTaskScopes`와 `TaskContext` propagation에 추가됐다.
- StructuredTaskScope provider discovery는 망가진 ServiceLoader entry에서 discovery를 중단하지 않고 건너뛴다.

## 검증

Targeted verification에는 다음이 포함되어야 한다.

- `repo-test-summary -- ./gradlew :bluetape4k-okio:test --tests "io.bluetape4k.okio.tink.DaeadChunk*Test"`
- `repo-test-summary -- ./gradlew :bluetape4k-virtualthread-api:test --tests "io.bluetape4k.concurrent.virtualthread.*Test"`
- `git diff --check`

## 향후 지침

Framed encryption format을 추가할 때는 frame-level adversarial test를 포함한다:
reorder, duplicate, drop final, truncate header/body, wrong associated data,
trailing data after final. Empty stream도 authenticated final marker가 필요하므로 empty payload
coverage를 유지한다. Concurrency change를 review할 때는 적절한 `bluetape4k-junit5` harness를
사용했는지 명시적으로 확인한다. 그렇지 않으면 single-shot example에만 의존하지 말고 follow-up PR에서 추가한다.
