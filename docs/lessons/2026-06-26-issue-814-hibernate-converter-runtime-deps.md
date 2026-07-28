# 이슈 814: Hibernate 컨버터 런타임 의존성

## 배경

`bluetape4k-hibernate`는 Tink 암호화, Jackson3 JSON, Kryo/Fory 직렬화,
LZ4/Snappy/Zstd/Commons Compress 압축용 컨버터 클래스를 제공한다. 그런데
이 컨버터 엔진 의존성은 `compileOnly` 또는 테스트 전용으로 선언되어 있었고,
모듈 테스트는 `compileOnly`를 `testImplementation`으로 확장했다. 그 결과
모듈 테스트는 통과했지만 downstream 소비자는 런타임 클래스가 빠질 수 있었다.

## 결정

문서화된 내장 컨버터 엔진은 `bluetape4k-hibernate` artifact 계약의 일부로
다룬다.

- 의존성 타입이 공개 컨버터 API에 드러나는 경우 `api`를 사용한다
  (`bluetape4k-tink`, `bluetape4k-jackson3`).
- 의존성이 내장 컨버터 구현에는 필요하지만 메서드 시그니처에는 드러나지 않으면
  `runtimeOnly`를 사용한다(Kryo, Fory, LZ4, Snappy, Zstd, Commons Compress).
- 정규 테스트 classpath가 아니라 `sourceSets.main.output`과
  `runtimeClasspath`만으로 smoke test를 컴파일하고 실행하는
  `consumerRuntimeTest` source set을 추가한다.

## 결과

새 consumer smoke test는 배포 런타임 classpath에서 Tink 암호화, Kryo/Fory
byte-array 컨버터, 압축 컨버터가 동작하는지 검증한다. README 의존성 예시는
더 이상 소비자에게 해당 컨버터 엔진을 `compileOnly`로 추가하라고 안내하지
않는다.

## 향후 지침

모듈이 main artifact 안에서 optional처럼 보이는 구체 구현을 제공한다면,
unit test와 별도로 consumer runtime classpath를 검증해야 한다. `compileOnly`를
확장한 테스트에 의존하지 않는다. 그런 테스트는 누락된 전이 런타임 의존성을
숨길 수 있다.
