# #1635: TLS handshake와 HTTP/2 SETTINGS ACK 경계를 구분한다

## 배경과 원인

[이슈 #1635](https://github.com/bluetape4k/bluetape4k-projects/issues/1635)는
gRPC Java `1.84.0`의 stream 제한 보안 수정을 검증한다.
중앙 catalog `9698c9d66bea6fcba373143ee8fa5bfbd9812d4b`가 지정한
`grpc-netty`·`grpc-okhttp`는 `1.84.0`, Netty는 `4.2.17.Final`이다.

[upstream 수정 56205f91c](https://github.com/grpc/grpc-java/commit/56205f91c2f8d2e40ba35bcd586c4bb571707f20)는
`DefaultHttp2Connection` 생성 직후 remote endpoint에 `maxActiveStreams`를 적용한다.
취약 경계는 TLS handshake가 아니라 **클라이언트의 SETTINGS ACK가 도착하기 전**이다.
TLS 상호운용 테스트 통과만으로 이 경계를 검증했다고 판단하면 안 된다.

## 결정

- TLS는 기존 Netty 서버·OkHttp 클라이언트의 실제 loopback 통신으로 검증한다.
- 제한은 실제 `NettyServerHandler`와 `EmbeddedChannel`로 검증한다.
  정상 클라이언트의 자동 SETTINGS ACK를 피하려고 HTTP/2 프레임을 직접 직렬화한다.
  제한 1과 stream 2개만 사용하며 부하 공격이나 외부 서비스 접근은 하지 않는다.
- 초과 stream의 `RST_STREAM(REFUSED_STREAM)`, 정상 stream 유지, 취소 후 슬롯 재사용,
  강제 종료 후 active stream 0개와 입력 buffer 해제를 각각 확인한다.
- 미완성 메시지를 보내 deframer가 보유한 데이터도 강제 종료 시 해제되는지 확인한다.
  transport listener와 metric recorder만 MockK로 대체한다. HTTP/2 decoder·handler는 실제 구현이다.
- package-private 접근은 `io.grpc.netty` 테스트 패키지로 한정한다.
  공개 API·생산 코드·의존성·CI 정책은 변경하지 않는다.

## RED/GREEN에서 수정한 가정

1. TLS enum의 `name`은 JSSE 프로토콜 이름이 아니다.
   `TLS_1_3`을 전달한 기존 테스트는 활성화 직후
   `SSLHandshakeException: No appropriate protocol`로 실패했다.
   `javaName()`의 `TLSv1.3`으로 교정하자 같은 `emptyUnary`가 통과했다.
2. 비활성 테스트의 기대값도 현재 fixture와 대조해야 한다.
   `largeUnary`는 zero-filled body를 기대했지만 `TestServiceImpl`은 임의 데이터로
   지정한 크기의 응답을 만든다. 테스트를 활성화하자 이 불일치가 드러났다.
   payload 존재와 요청한 응답 크기를 검증하도록 수정했다. 생산 동작은 바꾸지 않았다.
3. 해제된 pooled ByteBuf 객체의 나중 `refCnt()`는 최초 소유권의 증거가 아니다.
   객체가 다른 출력 buffer로 재사용되어 1로 보일 수 있다.
   직렬화 결과를 unpooled 입력으로 복사하고 원래 writer buffer는 즉시 해제하여
   관찰 대상을 분리했다. Netty의 unreleasable preface 상수도 복사해서 전달한다.
4. 보안 테스트는 설정값 확인에 그치지 않아야 한다.
   임시로 시작 제한을 `Int.MAX_VALUE`로 되돌리고 설정값 assertion을 제외하자
   실제 active stream이 2개가 되어 실패했다. 이후 임시 변이는 전부 제거했다.
   이는 이전 artifact를 실행한 결과가 아니라 과거 상태를 모사한 mutation 검증이다.
5. 구현 리뷰에서 writer 원본의 해제는 서버 복사본의 해제를 증명하지 못한다는 지적이 나왔다.
   DATA 전달분만 반환하고 그중 서버가 보유한 정확한 buffer를 골라,
   강제 종료 전 양수였던 참조 수가 같은 객체에서 0으로 바뀌는지 검증한다.
   앞으로 소유권 테스트는 전달 경계마다 검증 대상 객체를 명시한다.
6. TLS 회귀가 무한 대기로 바뀌지 않도록 클래스에 30초 제한을 두고,
   Future와 streaming 완료 대기에도 각각 10초 제한을 적용한다.
7. 로컬 macOS ARM 통과만으로 Linux TLS fixture를 검증할 수 없다.
   첫 CI에서 HTTP/2 보안 테스트 3개는 통과했지만 TLS 테스트 4개는 세 번 모두
   `SSLHandshakeException: Unknown authType: GENERIC`으로 실패했다.
   `TestUtils.installConscryptIfAvailable()`는 ARM에서는 설치를 건너뛰지만 Linux x86_64에서는
   Conscrypt를 전역 등록한다. 이후 OkHttp `Platform`은 Conscrypt를 선택하고,
   `newSslSocketFactoryForCa`는 JDK 기본 trust manager와 선택된 TLS provider를 조합한다.
   CI stack의 `ConscryptEngineSocket`과 `X509TrustManagerImpl` 혼용이 이 경계와 일치했다.
   이 저장소는 Java 25를 사용하므로 테스트의 Conscrypt 전역 설치를 제거하고
   JDK TLS provider·trust manager 경로를 일관되게 사용한다.

## 검증

- 기존 보안 기준 테스트: 5개 통과.
- 대상 TLS 4개·stream 경계 3개: `cleanTest --no-build-cache`로 3회, 매회 7개 통과.
- gRPC 전체: 75개 통과, 실패·오류·비활성 0개.
- 독립 구현 리뷰의 P2 buffer 검증 지적을 수정하고 재검토했다.
  최종 판정은 P0=0, P1=0, P2=0이며 수정 후 전체 75개를 다시 통과했다.
- PR #1653의 첫 CI는 gRPC 75개 중 TLS 4개가 실패했다.
  `Coverage Report`와 `CI Status` 실패는 `Test / IO` 실패를 전파한 결과였다.
  provider 수정 후 로컬 TLS 4개를 통과했으며 Linux 결과는 후속 CI에서 확인한다.
- Detekt 성공 종료. 기존 지적 28건은 별도이며 전체 정적 분석 무결함으로 표현하지 않는다.
- 조사 에이전트는 응답 지연으로 중단한 뒤 공식 `TestUtils`·OkHttp `Platform` 소스 근거를 회수했다.
  주 세션이 같은 소스를 다시 확인했으며, 조사 결과를 독립 코드 리뷰로 계산하지 않는다.

## 다음 변경 시 확인할 사항

gRPC 버전 변경 시 package-private factory와 오류 프레임 계약을 재확인한다.
TLS fixture가 provider를 전역 등록하면 지원 OS·아키텍처와 trust manager provider 조합을 CI에서 확인한다.
통신 성공, HTTP/2 stream 거부, buffer 소유권, 실제 서버 종료를 서로 다른 증거로 다룬다.
이 테스트는 작은 결정적 입력의 회귀 검증이지 운영 부하 한계나 모든 메모리 누수의 증명은 아니다.
