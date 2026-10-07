# JDK 역직렬화 필터 우회 API의 trust 경계

## 맥락

`BinarySerializers.JdkUnfiltered`는 테스트에서 `JDK_DEFAULT_OBJECT_INPUT_FILTER`의 허용 목록 밖에 있는 클래스를 다루기 위한 진입점이다. 이 이름은 생략하는 동작을 설명하지만 `Unsafe`를 포함하지 않는다. 저장소의 기존 trust-profile 규칙은 허용 전체 동작을 명시적인 unsafe 이름으로 구분하며, `@Deprecated`만으로는 운영 코드 사용을 막지 못한다.

또한 `objectInputFilter = null`은 Bluetape4k 기본 필터를 생략한다. JVM 전역 `ObjectInputFilter`가 설정되어 있으면 그 필터가 계속 적용될 수 있으므로, 이 경로를 전역 필터까지 제거하는 동작으로 설명하면 안 된다.

## 결정

- 요청된 공개 이름 `JdkUnfiltered`는 유지하고 `@Deprecated`로 표시한다. KDoc은 테스트 또는 신뢰 입력 전용이라고 경고하고, 생략하는 필터가 Bluetape4k 기본 필터임을 밝힌다.
- 이 이름은 `TrustedInternal` 예외로 분류한다. 중앙 trust-profile 문서와 한국어·영어 README에 허용 범위와 JVM 전역 필터 동작을 함께 기록한다.
- 기본 필터는 `java.awt.Color`만 정확히 허용하며 `java.awt.*`는 열지 않는다. 회귀 테스트에서 Color 왕복과 Font의 필터 판정 거부를 고정하고, 기본 allowlist 밖 fixture는 `JdkUnfiltered`로 왕복한다.

## 결과

기본 `Jdk`는 계속 `JDK_DEFAULT_OBJECT_INPUT_FILTER`를 적용하며, 필요한 `java.awt.Color`만 허용 목록에 추가했다. `JdkUnfiltered`는 기본 필터만 생략하고 생성자 계약과 JVM 전역 필터 경계는 유지한다. 테스트는 기본 allowlist를 완화하지 않고 필요한 fixture를 다룰 수 있다. 사용자는 API 경고와 trust-profile 문서에서 이 예외가 일반 운영용 serializer가 아님을 확인할 수 있다.

## 검증

- `repo-test-summary -- ./gradlew :bluetape4k-io:test --tests io.bluetape4k.io.serializer.JdkBinarySerializerSecurityTest --rerun-tasks --no-parallel --max-workers=1 --console=plain`: 15개 통과.
- `repo-test-summary -- ./gradlew :bluetape4k-io:test --rerun-tasks --no-parallel --max-workers=1 --console=plain`: 81개 suite, 1,316개 테스트 중 1,314개 통과, 2개 건너뜀, 실패·오류 0개.
- `:bluetape4k-io:detekt`: 빌드 성공. 기존 진단 4건은 수정 파일 밖에 있다.
- 독립 보안 검토는 승인했고, JVM 전역 필터 alias 직접 검증은 P3 후속 보강으로 남겼다. 같은 `objectInputFilter = null` 생성자 경로의 전역 필터 검증은 기존 별도 JVM 테스트가 다룬다.

## 향후 지침

역직렬화 필터를 생략하거나 허용 전체 동작을 제공하는 API는 우선 이름에 `Unsafe`를 명시한다. 사용자가 지정한 이름을 유지해야 하는 예외는 `@Deprecated`만으로 안전성을 주장하지 말고, 중앙 trust profile과 KDoc 및 README에 사용 경계와 생략되는 필터 계층을 기록한다. JVM 전역 필터의 잔여 적용 여부도 실제 경로 테스트나 동일 생성자 경로의 검증으로 확인한다.
