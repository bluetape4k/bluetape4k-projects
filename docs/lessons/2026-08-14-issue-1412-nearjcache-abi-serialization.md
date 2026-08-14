# 이슈 #1412 NearJCacheConfig ABI·직렬화 호환성 교훈

## 맥락

`NearJCacheConfig`에 `syncRemoteRetryCount`를 data class 주 생성자에 추가하면서
1.12.1 소비자가 호출하던 5-인자 JVM constructor와 `copy()` descriptor가
사라졌다. `serialVersionUID`를 유지해도 기존 stream에는 새 primitive field가
없으므로 JVM default `0`이 새 정책 기본값 `1`을 대체할 수 있었다.

## 결정

- 새 정책은 유지하되 5-인자 constructor와 `copy()`를 additive overload로
  복원한다.
- `ObjectInputStream.GetField.defaulted("syncRemoteRetryCount")`로 legacy
  stream의 누락과 명시적 `0`을 구분해 기본값을 복원한다.
- `cache/` 공개 설정·직렬화 변경은 prior-release Java/Kotlin fixture와
  serialized fixture evidence 없이는 stable publication을 진행하지 않는다.

## 검증

- Java fixture가 5-인자 constructor/copy를 직접 컴파일·호출한다.
- Kotlin fixture, getter/component metadata, legacy stream default, 명시적 `0`
  round-trip을 검증한다.
- cache-core 영향 테스트 58개와 compatibility 테스트 3개가 통과했다.
