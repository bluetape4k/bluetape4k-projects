# 교훈: 이슈 1000 Kafka4 catalog regression (2026-07-09)

## 배경

Catalog가 `kafka4`를 `4.3.1`로 되돌린 뒤 Nightly(full) run `28964461250`이
`Test / Infra (kafka-resilience)`에서 실패했다.

## 결정

Spring Kafka 4.1 embedded-test support가 Kafka 4.3.x와 호환될 때까지
`bluetape4k-projects`에 source-of-truth `kafka4 = "4.2.1"` compatibility line을
구체화한다.

## 결과

Kafka runtime artifact를 `4.2.1`로 정렬한 뒤 실패하던 local equivalent가 통과했다.

## 향후 방지책

`kafka4`가 바뀌면 dependency insight와 `:bluetape4k-kafka4:test`를 모두 확인한다.
이 실패는 embedded-test ABI mismatch이므로 dependency sync green만으로는 충분하지 않다.
