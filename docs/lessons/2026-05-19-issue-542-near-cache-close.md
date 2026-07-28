# 이슈 542 Near Cache Close Failure

## 배경

`ResilientNearCacheDecorator.close()`는 bare `runCatching`으로 delegate close failure를 무시해
shutdown resource leak을 조용히 만들었다.

## 결정

Blocking decorator를 suspend decorator와 정렬한다. Delegate `close()`는 직접 호출하고,
non-fatal failure는 cache name과 함께 log에 남기되 close path는 best-effort로 유지한다.
Lifecycle log message는 ops team이 일관되게 grep할 수 있도록 영어로 유지한다.

## 결과

Close path는 failure를 조용히 버리는 대신 warning을 낸다. Targeted unit test는 non-throwing
lifecycle behavior와 warning log message를 함께 고정한다.

## 검증

`./gradlew :bluetape4k-cache-core:test --tests "io.bluetape4k.cache.nearcache.ResilientNearCacheDecoratorTest"`가 9 tests로 통과.

Claude Code Opus review는 처음에 missing log assertion을 P2로 지적했다. `InMemoryLogbackAppender`
coverage를 추가하고 message를 영어로 바꾼 뒤 rereview는 남은 P0/P1/P2 finding이 없다고 보고했다.

## 향후 가드

Resource lifecycle code에서 failure를 의도적으로 log하거나 다른 방식으로 observable하게 만들지 않는다면
bare `runCatching { close() }`를 사용하지 않는다. Test는 exception 부재만이 아니라 observable lifecycle
signal을 고정해야 한다.
