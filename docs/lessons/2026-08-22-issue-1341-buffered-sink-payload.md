# Issue #1341: `buffer.size > 0` assertion은 쓰기 계약을 증명하지 못한다

## 맥락

`BufferedSuspendedSinkTest`는 여러 `write`/`writeAll` 경로를 실행했지만 마지막에
하위 sink의 buffer가 비어 있지 않은지만 확인했다. 한 경로가 byte를 누락하거나 잘못된
순서로 기록해도 다른 경로의 출력 때문에 테스트가 통과할 수 있었다.

## 결정

- 각 쓰기 경로에 구분 가능한 sentinel을 사용하고, Okio `Buffer`로 만든 기대
  바이트열과 하위 sink buffer의 `snapshot()`을 정확히 비교한다.
- `Buffer`와 `SuspendedSource`의 고정 길이 쓰기 경로는 source 전체 길이보다 작은
  `byteCount`를 넘겨, 구현이 길이를 무시하는 회귀도 검출한다.
- `writeAll` 반환 count는 최종 바이트열 assertion과 분리한다.
- 완전한 segment와 tail, `flush()`, `close()` 경계를 각각 확인하고 하위 sink 위임
  횟수를 고정한다.

## 결과

테스트는 `ByteString`, `ByteArray`, UTF-8 문자열과 code point, 정수 endian 변형,
decimal/hexadecimal 숫자, `Buffer`, `SuspendedSource`, `writeAll`의 실제 바이트열을
검증한다. `flush()` 전 tail은 내부 buffer에 남고, `flush()`와 `close()`는 각 시점의
tail을 한 번만 전달한다. production 코드는 변경하지 않았다.

## 검증

- 의도적으로 기대값을 `0x2A`에서 `0x2B`로 바꾸자 지정 테스트 1개가 hex payload
  불일치로 실패했다. fixture를 복원한 뒤 같은 테스트 1개가 통과했다.
- `BufferedSuspendedSinkTest`: 8개 통과
- `:bluetape4k-okio:detekt :bluetape4k-okio:build`: 1,437개 통과, 14개 pending,
  Kover 검증 포함 `BUILD SUCCESSFUL`
- 독립 사양 리뷰: `COMPLIANT`, 잔여 지적 0
- 독립 품질 리뷰: `APPROVED`, P0=0, P1=0, P2=0

## 놓치기 쉬운 점

최종 바이트열을 정확히 비교해도 고정 길이 쓰기 경로에 source 전체 길이를 넘기면
`byteCount`를 무시하는 구현을 구분할 수 없다. 기대값의 정확성뿐 아니라 입력 경계가
회귀를 드러내는지도 별도로 확인해야 한다.

## 향후 지침

`byteCount`를 받는 쓰기 테스트는 source 길이를 `byteCount`보다 크게 만들고, 반환
count와 하위 sink의 바이트열을 독립적으로 assertion한다. 수명주기를 검증할 때는 내부
buffer와 하위 sink buffer를 소비하지 않는 `snapshot()`으로 동시에 관찰한다.
