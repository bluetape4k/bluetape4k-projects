# bounded HTTP body는 크기와 소유권을 함께 설계해야 한다

## 맥락

HTTP 본문을 `ByteArray`나 `String`으로 만드는 편의 함수는 호출자가 허용할 크기를 넘는
본문을 끝까지 메모리에 적재할 수 있다. 단순히 읽기 길이만 제한하면 partial body가 정상
결과처럼 보이거나, adapter가 획득한 stream을 누가 닫아야 하는지 불명확해진다.

## 결정과 발견

- strict 상한은 최대 `maxBytes`까지 성공하고, 한 byte라도 초과하면 partial 결과 없이
  `ByteLimitExceededException`을 던진다.
- 판정용 1 byte는 stream에서 소비되며 복원하지 않는다. raw primitive는 caller stream을
  닫지 않고, HC5·JDK adapter는 자신이 획득한 stream을 닫는다.
- 작은 body에 큰 `maxBytes`가 들어와도 상한 크기 배열을 미리 만들지 않는다. segment를
  점진 할당하고 `maxBytes + 1` 정수 산술을 사용하지 않는다.
- bulk read가 0을 반환하면 단일 byte read로 진행을 보장한다.
- 원래 overflow/read/accessor 실패는 primary로 유지한다. cleanup 중 후속 실패는 동일
  instance가 아닐 때만 suppressed로 추가한다.
- blocking helper에서 발생한 `java.util.concurrent.CancellationException`을 coroutine
  cancellation처럼 특별 승격하면 승인된 primary 계약을 깨뜨린다. suspend API가 필요하면
  별도 cancellation 계약을 설계한다.

## 결과

`io/io`에 재사용 가능한 bounded read primitive를 두고, `io/http`의 HC5와 JDK adapter가
같은 strict byte 상한과 cleanup 규칙을 공유한다. 기존 truncation API는 호환성을 위해
유지하며, caller가 strict 거부와 prefix 수용을 명시적으로 선택한다.

## 검증

- primitive 테스트 9개 통과
- HC5·JDK adapter 테스트 33개 강제 재실행 통과
- `io` module check 1,274개 통과
- 공개 JVM signature를 `javap`로 확인
- 초기 failure-composition P1은 2개 RED 테스트로 재현한 뒤 GREEN으로 고정
- 로컬 `http` 전체 check는 테스트 이미지 `bluetape4k/mock-web-server:2.1.0` 404만 남아
  PR의 정확한 head CI에서 재검증

## 향후 지침

1. bounded read API는 크기 판정, partial 결과 정책, stream 위치, close 소유권을 하나의
   계약으로 설계한다.
2. metadata의 알려진 길이는 조기 거부에만 쓰고, 실제 stream에도 같은 상한을 적용한다.
3. primary/suppressed 우선순위는 일반적인 예외 관례를 기계적으로 적용하지 말고 승인된
   동기·비동기 경계와 테스트 계약을 기준으로 판단한다.
4. decoded 또는 decompressed payload는 wire/stream byte 상한과 별도로 제한한다.
5. 소비자 전환은 library publish, catalog 또는 허용된 repo-local override, smoke test,
   소비자별 PR 순서로 분리하고 rollback은 strict API 호출을 되돌리는 방식으로 준비한다.
