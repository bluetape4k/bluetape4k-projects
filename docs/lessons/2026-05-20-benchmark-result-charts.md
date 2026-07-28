# Benchmark Result Charts

## 배경

Benchmark 결과 문서는 숫자 표만으로 regression과 개선 폭을 파악해야 했다. Review 단계에서
reader가 allocation, throughput, latency 변화를 빠르게 비교할 수 있는 chart artifact가 필요했다.

## 결정

Benchmark 결과를 보존하는 문서에는 source metric과 함께 chart image를 둔다. Chart는 benchmark
claim을 대체하지 않고, 원본 숫자를 해석하기 쉽게 만드는 보조 artifact로 사용한다.

## 결과

Benchmark note는 chart와 source metric을 함께 제시한다. Reviewer는 숫자와 시각화를 비교해
claim이 과장되었는지 확인할 수 있다.

## 검증

- Chart artifact 생성 여부 확인.
- Benchmark source value와 chart label/value 대조.
- `git diff --check`.

## 향후 가이드

Benchmark chart는 항상 원본 metric table 또는 raw result와 함께 둔다. Chart만으로 performance
claim을 증명하지 않는다.
