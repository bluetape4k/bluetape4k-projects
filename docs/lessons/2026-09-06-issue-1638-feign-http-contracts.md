# Feign HTTP 회귀 테스트는 수정된 client 경계를 먼저 구분한다

## 기준과 결정

- 대상: [#1638](https://github.com/bluetape4k/bluetape4k-projects/issues/1638), Feign 13.14.
- 기준 HEAD: `6a198fa8461637d02da34fee2d2c4df28a6b7b6e`.
- 기본 catalog ref: `9698c9d66bea6fcba373143ee8fa5bfbd9812d4b`.
- `dependencyInsight --dependency feign-core --configuration testRuntimeClasspath`에서 실제 13.14 해석을 확인했다.
- 새 dependency나 production 변경 없이 기존 응답 확장 함수와 HC5/Vert.x 공통 테스트를 보강했다.

## upstream 수정과 적용 범위

공식 [13.14 release](https://github.com/OpenFeign/feign/releases/tag/13.14)와 아래 PR diff를
2026-09-06에 GitHub API로 읽었다. 전체 원문은 복제하지 않고 판단에 필요한 내용을 요약했다.

| 공식 수정 | 적용 경계 | 이번 검증 |
|---|---|---|
| [#3427](https://github.com/OpenFeign/feign/pull/3427) charset fallback | core `Response.charset()` | absent/unsupported/illegal charset, UTF-8·한글·emoji 보존, 유효한 ISO-8859-1 유지 |
| [#3444](https://github.com/OpenFeign/feign/pull/3444) CR/LF 제거 | core `HeaderTemplate`의 literal·resolved value | CR, LF, CRLF 각각의 template 결과와 실제 수신 헤더, 주입 헤더 부재 |
| [#3463](https://github.com/OpenFeign/feign/pull/3463), [#3507](https://github.com/OpenFeign/feign/pull/3507) 빈 요청 | `DefaultClient`의 HttpURLConnection | 이 구현의 기대값을 복제하지 않고 HC5/Vert.x의 GET/POST/PUT/PATCH와 null/empty body를 직접 검증 |
| [#3492](https://github.com/OpenFeign/feign/pull/3492) 음수 길이 | `DefaultClient`·Google client | HC5/Vert.x는 음수 응답 길이를 전송 오류로 거부한다. timeout을 성공으로 허용하지 않는다 |
| [#3431](https://github.com/OpenFeign/feign/pull/3431) 길이 축소 변환 | `Http2Client` | HC5 자체 streaming body의 기존 범위 검사로 Int 초과 길이가 null인지 확인 |
| [#3448](https://github.com/OpenFeign/feign/pull/3448), [#3451](https://github.com/OpenFeign/feign/pull/3451) 중복 길이 헤더 | `DefaultClient` | HC5/Vert.x에서 대소문자별 Content-Length가 자동 생성 헤더와 중복되지 않고 UTF-8 바이트 길이와 일치하는지 확인 |
| [#3417](https://github.com/OpenFeign/feign/pull/3417), [#3432](https://github.com/OpenFeign/feign/pull/3432) multipart 헤더 | multipart encoder | 사용하지 않는 API로 이번 범위에서 제외 |

Vert.x는 전체 body를 받은 뒤 Feign 응답을 만든다. HC5의 streaming 길이 메타데이터 테스트를
그대로 옮기면 거대한 body 또는 의도적인 조기 연결 종료를 테스트하게 된다. 따라서
`Long.MAX_VALUE`를 선언한 fixture는 HC5에만 적용하고 실제 대용량 body는 만들지 않았다.
이 fixture의 close 중 불완전 body 오류는 `closeSafe()`로 정리하며, 전송 완료를 증명한다고 주장하지 않는다.

## 실패한 가정과 다음 검증 기준

1. **테스트 설계:** GET은 길이 헤더가 없을 것이라고 가정했으나 HC5 테스트가
   `Expected ["0"] to equal to []`로 실패했다. 공식 `ApacheHttp5Client.toClassicHttpRequest()`는
   body가 null이어도 빈 entity를 만든다. 현재 HC5와 Vert.x는 네 method 모두 길이 0을 전송한다.
   다음에는 release 제목이 아니라 해당 adapter의 entity 생성과 전송 코드를 먼저 확인한다.
2. **오류 모델:** Vert.x가 음수 길이를 `DecoderException`으로 감쌀 것이라고 가정했지만
   실제 원인은 `IllegalArgumentException: Content-Length value is not a number: -5`였다.
   HTTP decoder의 실패 전달 경로와 실제 cause를 읽은 뒤 assertion을 정한다.
   `TimeoutException`과 `SocketTimeoutException`은 거부 결과로 인정하지 않는다.
3. **조사 운영:** 독립 조사 담당이 응답하지 않아 중단하고 주 세션이 공식 release·PR diff를 확인했다.
   helper의 command deadline 판정과 plain-session 90초 무응답 제한이 달랐다.
   다음에는 native lane의 deadline을 실제 대기 제한과 맞추고, 중단 의도와 결과를 순서대로 기록한다.
   이번 조사를 독립 검증으로 표시하지 않는다.

## 검증

독립 설계 리뷰는 P0/P1 없이 WATCH(P2)를 남겼다. 공통 suite에 두 adapter의 정확한 wire 정책과
core 통합 검증이 함께 있어 향후 다른 adapter를 추가할 때 결합 비용이 생긴다는 지적이다.
현재 HC5/Vert.x로 한정한 KDoc과 후속 adapter의 기대값 검토 지침을 추가했다.
현재 지원 범위를 넘어선 테스트 재배치는 이번 이슈에 포함하지 않는다.
독립 코드 리뷰는 제한 시간 내 결과를 반환하지 않아 중단했으며, 주 세션의 inline fallback review로 대체한다.

- 기존 대상 테스트: 19개 통과.
- 보강 후 대상 테스트: 56개 통과. 신규 37개이며 null/empty body와 literal/resolved template은 각 invocation 안에서 둘 다 검증한다.
- `./gradlew :bluetape4k-feign:cleanTest :bluetape4k-feign:test :bluetape4k-feign:detekt --no-build-cache`:
  280개, 실패·오류·skip 0. 기존 gzip/deflate·coroutine 경로 포함.
- detekt는 기존 production 지적을 출력한다. 이번 변경의 production diff는 없으며, 이를 경고 0으로 해석하지 않는다.
- production 수정이 없으므로 제품 수정의 RED/GREEN을 주장하지 않는다. 위 실패는 테스트 가정 교정 근거다.
- README·public API·module 등록·catalog 변경 없음.
