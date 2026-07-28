# 이슈 519 FlociServer Stable Promotion

## 배경

Issue #519는 `LocalStackServer`와 `FlociServer` 사이의 circular deprecation 제거, pinned Floci image
갱신, Floci service test가 LocalStack replacement path를 cover하는지 검증을 요청했다.

## 결정

`FlociServer`를 stable open-source AWS emulator wrapper로 승격하고 `LocalStackServer`는 deprecated로
유지한다. 구현 시점의 current upstream stable release는 2026-05-18에 publish된 Floci `1.5.17`이므로,
default pinned tag는 issue의 원래 `1.5.16` target을 넘어선다.

Default로 `-compat` image를 사용하지 않는다. Floci 문서는 AWS CLI와 boto3를 포함하는 image에
`x.y.z-compat`를 사용한다고 설명한다. AWS SDK와 Testcontainers usage에는 standard pinned image가
올바른 default다.

## 결과

`FlociServer`는 더 이상 `@Deprecated`를 갖지 않고 default tag는 `1.5.17`로 pin된다. Floci test package는
deprecation warning suppression을 제거했다. `LocalStackServer`는 open-source user를 `FlociServer`로
안내하는 명확한 deprecation message를 유지한다.

## 검증

- GitHub Releases에서 implementation 당시 `floci-io/floci` `1.5.17`이 latest stable non-draft,
  non-prerelease release임을 확인.
- Docker Hub에서 matching `floci/floci:1.5.17`와 `floci/floci:1.5.17-compat` tag 확인.
- Floci README에서 standard vs compat image semantics 확인.
- `rg`로 `FlociSTSTest`가 이미 있음을 확인해 issue의 STS coverage question 종료.

## 향후 agent 가이드

Reproducible testcontainers default에는 pinned stable Floci tag를 사용한다. Init-script workflow가
Floci image 내부의 AWS CLI 또는 boto3를 필요로 할 때만 `-compat`를 선택한다.
