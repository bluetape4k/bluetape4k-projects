# Progression stream overflow 회귀 방지

## 증상

`IntProgression`/`LongProgression`을 Java primitive stream으로 바꿀 때 마지막
원소 직후의 overflow 값이 stream에 섞이거나 종료하지 않을 수 있다.

## 원인

`IntStream.iterate`와 `LongStream.iterate`의 update lambda가 `current + step`을
직접 계산하면 Kotlin progression이 이미 계산한 `last` 계약과 Java predicate가
분리된다.

## 적용 규칙

Kotlin progression의 종료 규칙을 재사용해야 하는 adapter는 progression iterator를
lazy primitive `Spliterator`로 감싼다. primitive update lambda에서 경계 산술을
반복하지 않으며, `step == 1`처럼 검증된 범위 최적화만 별도 유지한다.

## 검증

`Int.MIN_VALUE`/`Int.MAX_VALUE`, `Long.MIN_VALUE`/`Long.MAX_VALUE`에서
`step == -1`, `step < -1`, `step > 1`을 bounded stream 테스트로 확인하고,
`:bluetape4k-core:check`까지 통과시킨다.

## 연결 이슈

- [#1620](https://github.com/bluetape4k/bluetape4k-projects/issues/1620)
