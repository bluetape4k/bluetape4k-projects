# ByteArray를 내용으로 비교하면 hashCode도 내용으로 계산한다

## 맥락

[이슈 #1800](https://github.com/bluetape4k/bluetape4k-projects/issues/1800)에서 `JwtReaderDto.equals()`는 `digest.contentEquals()`로 바이트 배열의 내용을 비교했지만, `hashCode()`는 `hashOf(headers, claims, digest, tokenString)`을 사용했다. `hashOf()`는 `Objects.hash()`에 값을 전달하므로, 인자로 중첩된 `ByteArray`는 내용이 아니라 배열 객체의 해시값으로 처리된다. 따라서 내용이 같은 별도 배열을 가진 두 DTO는 `equals()`가 `true`여도 `hashCode()`가 달라질 수 있었고, `HashSet` 포함 검사와 `HashMap` 키 조회도 실패했다.

이 계약은 `22236e6fcafcd585b445802b170c7a2df0b98911`의 스타일 정리에서 기존 내용 기반 해시 조합을 제거하면서 깨졌다. 문제는 코드 모양이 아니라 `equals()`와 `hashCode()`가 배열을 다르게 비교한 데 있었다.

## 결정

- 기존 필드 순서를 유지한 31 기반 조합으로 해시를 계산한다. `digest`는 `contentHashCode()`를 사용하고 `null`은 `0`을 더한다.
- 별도 `ByteArray`에 같은 바이트를 담은 DTO의 동등성뿐 아니라 해시값 일치도 확인한다. 이어 실제 `HashSet` 포함 검사와 `HashMap` 키 조회를 검증한다.
- 서로 다른 내용은 `shouldNotBeEqualTo`로 동등하지 않음을 확인한다. 다만 동등하지 않은 객체의 해시값이 반드시 달라야 한다고 단정하지 않는다. 해시 충돌은 허용된다.
- 배열을 내용으로 비교하는 사용자 정의 `equals()`를 추가하거나 정리할 때는 대응하는 `hashCode()`가 같은 내용 모델을 쓰는지 함께 확인한다. 양쪽 구현과 collection 조회를 하나의 회귀 테스트로 고정한다.

## 검증

- 수정 전 `JwtReaderDtoTest`는 컴파일에 성공했고, 같은 내용을 가진 별도 배열의 hashCode 비교와 `HashSet` 조회에서 의도한 실패 2건을 재현했다. 다른 내용 및 `null` 케이스는 통과했다.
- 수정 후 `JwtReaderDtoTest` 4개가 모두 통과했다. 별도 배열 동등성, 다른 내용, `null`, `HashSet`/`HashMap` 조회를 검증한다.
- `:bluetape4k-jwt:test`: 124개 통과, provider 유형 조건에 따라 `Assumptions.assumeTrue`에서 중단된 테스트 2개, 실패와 오류 0개. 두 상속 테스트는 `DefaultJwtProvider`를 사용하는 실행에서 통과한다.
- `:bluetape4k-jwt:detekt`: `BUILD SUCCESSFUL`; 진단 12건은 변경하지 않은 파일에 있고 변경한 두 Kotlin 파일의 진단은 0건이다.
- `DtoSerializationTest` 2개가 Java 직렬화 왕복 및 UID 검증을 포함해 통과했다. `serialVersionUID`는 `6285801536022841892L`로 유지했다. 변경은 `hashCode()` 구현과 사용하지 않는 import 제거뿐이며, JWT 모듈에는 ABI 검증 Gradle task가 노출되지 않는다.
- 실행 명령과 결과는 [`docs/testlogs/2026-10.md`](../testlogs/2026-10.md)에 기록했다.

## 재발 방지

배열 같은 참조 타입을 사용자 정의 `equals()`에서 값으로 비교하면 `hashCode()`도 그 값 표현에 맞춰야 한다. 테스트는 `equals()`만 확인하지 말고, 내용이 같은 별도 배열에서 해시값이 같으며 그 객체가 기존 `HashSet`/`HashMap` 항목을 찾는지 확인한다. `null`과 서로 다른 내용을 함께 두어 경계 조건을 검증하되, 서로 다른 객체의 해시값 차이는 요구하지 않는다.
