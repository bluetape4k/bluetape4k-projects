# 이슈 #1266 Fory 동시성 테스트의 codegen 경로 검증

## Context

`SerializerEdgeCaseTest`의 멀티스레드 직렬화 테스트는 메서드 내부에 `Item` data class를
선언하고 있었다. Fory는 이 local class의 enclosing test 이름까지 포함해 Java codec 이름을
생성하므로, 한국어와 공백이 들어간 이름이 Janino 소스의 Java class 이름으로 사용되었다.
기존 비동기 compilation 설정에서는 생성 실패 후 interpreter serializer로 fallback했지만,
직렬화 결과와 동시성 assertion만으로는 이 경로 이탈을 감지하지 못했다.

## Decision or Finding

- 동시성 fixture를 테스트 클래스의 안정적인 `ConcurrentItem` nested data class로 옮겼다.
  메서드 이름이나 display name이 generated Java codec 이름에 포함되지 않도록 fixture의
  binary name을 고정한다.
- 동시성 테스트는 기존 JDK/Kryo/Fory parameterized coverage와 worker/round 수를 그대로
  유지한다.
- 별도 Fory regression test는 `withCodegen(true)`와 동기 compilation을 사용하고, serialize
  직후 `Fory#getSerializer`가 `Generated.GeneratedSerializer` 구현체인지 확인한다. 따라서
  payload round-trip만 통과하는 interpreter fallback을 성공으로 취급하지 않는다.
- production serializer configuration이나 public API는 변경하지 않는다. 문제의 원인은
  serializer 구현이 아니라 test fixture 이름의 codegen 적합성이다.

## Outcome

- 기존 local `Item`을 그대로 둔 RED 단계에서는 Janino가
  `SerializerEdgeCaseTest_Fory codegen path uses a generated serializer for the concurrency fixture_ItemForyRefCodec_0`
  를 컴파일하지 못하고 `'{' expected instead of 'codegen'` 오류를 냈다.
- `ConcurrentItem`으로 교체한 GREEN 단계에서는 generated serializer assertion과 기존
  멀티스레드 round-trip assertion이 함께 통과한다.

## Verification

```bash
./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.serializer.SerializerEdgeCaseTest' \
  --rerun-tasks --no-configuration-cache --console=plain
```

- RED: `Executed 32 tests in 2.5s (1 failed)` — local fixture의 Janino codegen 실패를 확인했다.
- GREEN: `SUCCESS: Executed 32 tests in 2.4s`, `BUILD SUCCESSFUL`.
- GREEN 로그에 `CompileException`이 없고 generated serializer regression test가 PASSED다.

## Future Guidance

1. Fory codegen을 검증하는 테스트 fixture는 method-local class로 선언하지 말고 stable
   top-level 또는 static nested class를 사용한다.
2. serializer round-trip 성공만으로 codegen 성공을 주장하지 않는다. codegen을 의도한
   경로는 generated serializer type 또는 equivalent direct evidence를 assertion한다.
3. Fory async compilation을 사용하는 테스트는 실패 시 interpreter fallback이 가능한지
   확인하고, 필요한 경우 동기 compilation probe를 별도 regression gate로 둔다.
4. 테스트 display name은 사람이 읽는 라벨로만 취급하며 generated source/class name의
   입력으로 유입되지 않도록 local fixture의 위치와 이름을 점검한다.
