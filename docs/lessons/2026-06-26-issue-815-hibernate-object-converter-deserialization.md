# 이슈 815: Hibernate 객체 컨버터 역직렬화 경계

## 배경

범용 Hibernate 객체 컨버터는 `Any?`를 받아 명시적인 타입 경계 없이 DB payload를
역직렬화했다. 행이 변조되거나, 외부에서 import되거나, tenant 간에 공유될 수 있는
환경에서는 persisted object column 자체가 trust boundary가 된다.

## 결정

호환성을 위해 기존 컨버터는 유지하되 trusted storage 전용으로 deprecate한다.
대상 클래스와 serializer를 요구하는 typed converter base를 추가하고, 역직렬화된
값이 예상 타입과 맞지 않으면 거부한다. Kryo와 Fory 경로에서는
`KryoBinarySerializer.secure(...)`, `ForyBinarySerializer.secureFory(...)` 같은
secure serializer factory를 우선 사용한다.

## 결과

공개 API는 호출자가 범용 `Any?` 컨버터에서 typed converter subclass로 이동할
수 있는 경로를 제공한다. Negative test는 malformed payload, 예상하지 못한 JDK
payload 타입, secure Kryo/Fory에서 허용되지 않는 payload를 검증한다.

## 향후 지침

binary serialization 기반 persistence converter를 추가할 때는 API 형태와 README
예시 모두에서 trusted/untrusted 경계를 명시한다. 호환성 wrapper는 남길 수 있지만,
persisted 외부 mutable data에 권장되는 경로가 되어서는 안 된다.
