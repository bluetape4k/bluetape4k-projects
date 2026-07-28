# Testcontainers Floci와 MinIO policy 교훈 (#880, 2026-06-23)

관련 이슈: #880
영향 module: `:bluetape4k-testcontainers`

## L1: deprecation policy는 지원 중인 compatibility fixture를 시끄럽게 만들면 안 된다

`MinIOServer`는 direct MinIO compatibility test를 위한 지원 fixture로 남아 있다. class
자체를 deprecated로 표시하자, 1.11.0 policy가 이 fixture를 계속 제공하는데도 모든 internal
factory, launcher, direct compatibility test가 migration debt처럼 보였다.

class-level deprecation은 제거됐고, reflection contract test는 `MinIOServer`가 지원되는
동안 non-deprecated 상태로 남는지 확인한다.

## L2: default AWS/S3 emulator guidance는 wrapper boundary에 둔다

새 AWS/S3 emulator test는 MinIO를 default로 쓰지 말고 현재 AWS emulator path를 사용해야
한다. durable guidance는 이제 `MinIOServer` KDoc과 `testing/testcontainers` README locale
set에 있다. 새 AWS/S3 emulator coverage에는 `FlociServer` 또는 `MiniStackServer`를 쓰고,
explicit MinIO compatibility behavior에만 `MinIOServer`를 사용한다.

## L3: pinned emulator image tag에는 contract test가 필요하다

Docker Hub release tag `1.5.27`은 2026-06-23에 검증됐다. `FlociServer`는 재현 가능한
Testcontainers run을 위해 pinned release tag를 유지하고, `FlociServerTest`가 그 값을 지켜
future tag change가 의도적으로 이루어지게 한다.
