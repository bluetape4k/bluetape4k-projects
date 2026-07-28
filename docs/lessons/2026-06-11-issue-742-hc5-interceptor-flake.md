# 이슈 #742: HC5 async interceptor ordering test

## 배경

`AsyncClientInterceptors`는 많은 async request를 발행하면서 request interceptor와
execution interceptor event를 하나의 shared list에 기록했다. 기존 assertion은 전체
list를 serialized expected sequence와 비교했다.

## 결정

production behavior는 변경하지 않고 execution id별 interceptor ordering을 assert한다.
cross-request interleaving은 async execution에서 유효하므로 test contract에 포함하지
않는다.

## 검증

- focused `AsyncClientInterceptors` test.
- full `:bluetape4k-http:test` module test.

## 향후 가드

test가 concurrent 또는 async request의 event를 기록할 때는 먼저 request/execution
identity로 group한다. feature가 global ordering contract를 명시적으로 약속하지 않는 한
per-request invariant를 assert한다.
