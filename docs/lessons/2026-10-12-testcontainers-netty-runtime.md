# Testcontainers 런타임에서 Netty 호환성 라인을 섞지 않는다

## 실패와 원인

[Nightly storage-cache job](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/38204929098/job/114667080736)은
Redis 테스트에서 `ClassNotFoundException: io.netty.channel.MultiThreadIoEventLoopGroup`로 실패했다.
infra job의 Toxiproxy 테스트도 같은 오류로 실패했다. image gate에서 실패한 항목은
Redis, Redis Cluster, Toxiproxy였다.

Jib 또는 Gradle 버전 문제라는 가설은 실행 로그와 맞지 않았다. storage-cache job은
두 `jibDockerBuild`를 `-x`로 제외했고, image gate job에서는 두 이미지 빌드가 성공했다.
로컬 wrapper와 CI 모두 Gradle 9.7.0이었다. 버전을 추정하지 않고 실행 로그와
`gradle-wrapper.properties`를 각각 확인해야 한다.

Fabric8 7.8.0의 기본 Vert.x 4 HTTP 구현을 지원하려고 모듈 전체에 Netty 4.1을 강제했다.
그러나 Lettuce 7.6.0과 Redisson 4.7.0은 Netty 4.2를 요구한다. `dependencyInsight`에서
Lettuce가 요청한 `netty-transport:4.2.13.Final`이 규칙으로 `4.1.136.Final`로 선택됐음을 확인했다.
Docker 없이 클래스를 로드하는 회귀 테스트도 동일한 `ClassNotFoundException`을 재현했다.

## 결정

Fabric8의 기본 `kubernetes-httpclient-vertx`를 제외하고 같은 버전의
`kubernetes-httpclient-okhttp`를 테스트 런타임에 사용한다. 기존 OkHttp 의존성을 재사용하며,
Kubernetes 전송 구현을 Netty에서 분리한다. 모듈 전체의 Netty 4.1 및 Vert.x 4 재정의는 삭제한다.
공유 카탈로그의 Netty 4.1 호환성 라인은 다른 소비자를 위해 유지한다.

JDK HTTP 구현도 검토했지만 알려진 설정 및 mock server 제한 때문에 최종 구현에는 사용하지 않았다.
Lettuce 하향 조정, native transport 비활성화, configuration cache 설정 변경은
충돌한 런타임 의존성의 책임 경계를 해결하지 못하므로 채택하지 않았다.

## 검증과 재발 방지

- Gradle 9.7.0에서 회귀 테스트는 수정 전 `ClassNotFoundException`으로 실패했다.
- 최종 OkHttp 조합에서 Redis 연결, Redis Cluster, Kubernetes HTTP 요청 및 K3s 입력 검증은
  10개가 통과했다. 기존 비활성 테스트 1개는 실행되지 않았다.
- Gradle 9.8.1은 공식 binary SHA-256을 확인하고 wrapper에 반영했다.
- Gradle 9.8.1에서 storage 및 회귀 테스트 54개 중 50개가 통과했고,
  기존 비활성 테스트 4개는 실행되지 않았다. Toxiproxy 및 회귀 테스트 7개도 통과했다.
  privileged K3s에서 노드 조회, namespace 생성·삭제, Lease 생성·조회·삭제를 포함한 6개가 통과했다.
- Gradle 9.8.1에서 두 mock 서버의 `jibDockerBuild`가 모두 성공했다. 마지막 회귀 테스트 2개,
  actionlint 및 CI/release 계약 테스트 16개도 통과했다. 기존 Detekt 위반 46건은 범위 밖이며
  새 회귀 테스트에는 위반이 없다. 원격 Nightly 재실행은 수행하지 않았다.
- 의존성 업데이트 때 `testRuntimeClasspath`의 선택 이유와 실제 클라이언트 초기화·요청을 함께 확인한다.
  BOM 이름 또는 컴파일 성공만으로 런타임 호환성을 판단하지 않는다.
- Jib 실패 가설은 실제 task 실행·제외 여부와 첫 번째 예외를 확인한 뒤 판단한다.

## 공식 근거

- [Fabric8 7.8.0 FAQ](https://github.com/fabric8io/kubernetes-client/blob/v7.8.0/doc/FAQ.md): HTTP 구현 교체 방법
- [Fabric8 JDK 구현의 제한](https://github.com/fabric8io/kubernetes-client/blob/v7.8.0/httpclient-jdk/README.md)
- [Netty 4.2 migration guide](https://github.com/netty/netty/wiki/Netty-4.2-Migration-Guide)
- [Lettuce 7.6.0 release](https://github.com/redis/lettuce/releases/tag/7.6.0.RELEASE)
- [Gradle 9.8.1 release notes](https://docs.gradle.org/9.8.1/release-notes.html)
- [Jib Gradle 9 configuration cache 추적 이슈](https://github.com/GoogleContainerTools/jib/issues/4468)
