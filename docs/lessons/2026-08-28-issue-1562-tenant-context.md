# Issue #1562 TenantContext 구현 lesson

## Maven group은 기억이 아니라 publication source로 확정한다

- 실패한 가정/판단: 초기 설계의 dependency group을 `io.bluetape4k`로 적었다.
- 발견 증거 또는 교정: `gradle.properties`의 `projectGroup`과 생성 POM은
  `io.github.bluetape4k`를 사용했다.
- 수정 결정: spec, plan과 모든 README coordinate를 `io.github.bluetape4k`로 교정하고
  generated POM/module metadata/BOM 검증을 실행했다.
- 향후 예방 확인: consumer coordinate를 작성할 때 `projectGroup`, 생성 POM, 공개 metadata의
  세 source를 대조하고 문자열 fixture가 wrong group을 fail-closed로 거부하게 한다.

## 넓은 review lane의 deadline 초과는 finding closure가 아니다

- 실패한 가정/판단: 전체 파일을 한 번에 맡긴 세 review lane이 5분 안에 끝날 것으로 보았다.
- 발견 증거 또는 교정: lane이 command deadline 안에 결과를 반환하지 못했고 actionable evidence가
  없었다.
- 수정 결정: 초과 lane 결과는 판정에서 제외하고 production, docs/architecture, retention,
  SNAPSHOT, publication, operations/security의 좁은 read-only 관점으로 다시 배정했다.
- 향후 예방 확인: review는 파일과 질문을 한 관점으로 제한하고 90초 동안 fresh evidence가 없으면
  범위를 줄여 회수한다. 완료 메시지가 없는 lane을 P0/P1 closure 증거로 사용하지 않는다.

## matrix prefix 하나는 full validation 증거가 아니다

- 실패한 가정/판단: Infra, Data, Spring Boot, Testcontainers matrix마다 prefix job 하나만 있으면
  full Nightly를 증명할 수 있다고 보았다.
- 발견 증거 또는 교정: 한 shard가 누락되어도 같은 prefix의 다른 shard가 있으면 publish validation이
  통과할 수 있었다.
- 수정 결정: exact full job set을 모두 요구하고 Nightly source의 matrix group과 required set의 drift를
  policy test로 연결했다.
- 향후 예방 확인: matrix 추가·이름 변경은 Nightly source와 SNAPSHOT validation set의 equality
  검증을 함께 통과해야 한다.

## 재시도는 transport 일시 오류에만 허용한다

- 실패한 가정/판단: public read-back 실패를 모두 같은 propagation delay로 보고 20회 재시도했다.
- 발견 증거 또는 교정: TLS 인증, 잘못된 metadata identity와 checksum mutation도 재시도되어 원인이
  최대 5분 늦게 드러날 수 있었다.
- 수정 결정: 제한된 HTTP/network 오류만 exit `75`로 분류하고 TLS 및 semantic 오류는 exit `1`로
  즉시 실패한다. metadata identity도 strict format과 timestamped version equality를 검증한다.
- 향후 예방 확인: retry loop는 명시적 transient exit code만 소비하고 permanent fixture가 한 번에
  실패하는지 policy/fixture test로 고정한다.

## GC retention 검증은 결정적 cleanup과 보조 신호를 분리한다

- 실패한 가정/판단: `System.gc()`와 사용하지 않는 1 MiB 배열을 10초 반복하면 retention을 충분히
  증명한다고 보았다.
- 발견 증거 또는 교정: GC는 강제 계약이 아니며 최적화된 할당은 pressure가 되지 않을 수 있었다.
- 수정 결정: lexical 종료 직후 `currentOrNull()==null`과 예외 cleanup을 결정적 계약으로 유지하고,
  별도 nightly task는 256 MiB heap, retained 64 MiB pressure, 30초 bounded WeakReference probe와
  실패 진단 artifact를 사용한다.
- 향후 예방 확인: GC probe는 결정적 API test를 대체하지 않으며 bounded stress/diagnostic gate로만
  사용한다. timeout을 늘리는 것만으로 성공을 만들지 않는다.
