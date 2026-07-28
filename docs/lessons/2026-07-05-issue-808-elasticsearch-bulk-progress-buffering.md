# 이슈 808: Elasticsearch bulk progress buffering

## 배경

`bulkProgressListener`는 `Channel.UNLIMITED`를 사용했다. 따라서 느리거나 없는
collector는 bulk request, response, failure, caller context를 hard bound 없이 보관할
수 있었다.

## 결정

기본 capacity 256과 `BufferOverflow.SUSPEND`를 가진 bounded `Channel`을 사용한다.
`trySend`로 listener callback을 non-blocking으로 유지하고, overflow가 보이도록 failed
send를 log한다.

## 결과

Progress listener는 기본적으로 유한한 수의 event만 보관하고, capacity와 overflow
tuning을 노출하며, Elasticsearch I/O 없이 overflow behavior를 검증하는 회귀 테스트를
가진다.

## 향후 지침

Listener-to-Flow adapter는 호출자가 명시적으로 opt-in하지 않는 한 unbounded channel을
피해야 한다. Bounded buffer, non-blocking callback path, 보이는 drop/overflow behavior를
우선한다.
