# Byte-safe UTF-8 truncation은 먼저 bluetape4k-core로 승격

**날짜**: 2026-05-23
**관련**: bluetape4k-leader#270

## 배경

`bluetape4k-leader`에는 history error-message truncation을 위한 internal
`String.truncateUtf8(maxBytes)` helper가 있다. 이 helper는 general-purpose이므로 shared support package에
속한다. 하지만 public API가 published `bluetape4k-core` artifact에 들어가기 전에는 downstream
repository가 안전하게 consume할 수 없다.

## 결정

Leader-internal implementation과 같은 byte-boundary contract로
`io.bluetape4k.support.truncateUtf8(maxBytes)`를 `bluetape4k-core`에 추가한다. API는 작게 유지한다:
ellipsis 없음, grapheme-cluster guarantee 없음, nullable receiver overload 없음.

## 후속

이 API가 `bluetape4k-leader`가 consume하는 BOM version으로 release되면, leader repository는 internal
copy를 제거하고 Maven Central symbol missing으로 CI를 깨뜨리지 않고 shared support function을 import할
수 있다.
