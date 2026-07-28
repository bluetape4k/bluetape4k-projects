# 이슈 #820 MongoDB provider cache semantics

## 배경

`MongoClientProvider.getOrCreate(connectionString, builder)`는 URL만으로 cache했다.
같은 URL에 대해 나중 호출자가 다른 builder setting을 제공해도 첫 호출자가 모든 후속
호출자의 effective setting을 결정했다.

## 결정

Provider-managed client는 raw connection string이 아니라 최종 immutable
`MongoClientSettings`로 cache한다. 기존 overload의 source compatibility는 유지하되
모든 overload를 settings cache로 보낸다.

## 결과

- 같은 URL과 동일한 setting은 같은 shared client를 반환한다.
- 같은 URL과 다른 setting은 서로 다른 shared client를 반환한다.
- Provider-owned shared client에는 명시적인 `close(...)`, `closeAll()` lifecycle
  API가 생겼고, README/KDoc은 반환된 shared instance를 직접 닫지 말라고 경고한다.

## 향후 규칙

Provider overload가 builder나 custom option을 받는다면 cache key는 최종 effective
configuration을 표현해야 한다. 호출자가 제공한 setting이 runtime behavior를 바꿀 수
있을 때는 "base" identifier만으로 cache하지 않는다.
