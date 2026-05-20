# Issue 519 FlociServer Stable Promotion

## Context

Issue #519 asked to remove the circular deprecation between `LocalStackServer`
and `FlociServer`, update the pinned Floci image, and verify that Floci service
tests cover the LocalStack replacement path.

## Decision

Promote `FlociServer` to the stable open-source AWS emulator wrapper and keep
`LocalStackServer` deprecated. The current upstream stable release is Floci
`1.5.17`, published on 2026-05-18, so the default pinned tag should move past
the issue's original `1.5.16` target.

Do not use the `-compat` image as the default. Floci documents `x.y.z-compat`
for images that include AWS CLI and boto3, while the standard pinned image is
the right default for AWS SDK and Testcontainers usage.

## Outcome

`FlociServer` no longer carries `@Deprecated`, its default tag is pinned to
`1.5.17`, and the Floci test package no longer suppresses deprecation warnings.
`LocalStackServer` keeps a clear deprecation message that points open-source
users to `FlociServer`.

## Verification

- GitHub Releases confirmed `floci-io/floci` `1.5.17` as the latest stable
  non-draft, non-prerelease release at implementation time.
- Docker Hub confirmed matching `floci/floci:1.5.17` and
  `floci/floci:1.5.17-compat` tags.
- Floci README confirmed standard vs compat image semantics.
- `rg` confirmed that `FlociSTSTest` already exists, closing the issue's STS
  coverage question.

## Future Agents

Use pinned stable Floci tags for reproducible testcontainers defaults. Reach for
`-compat` only when an init-script workflow needs AWS CLI or boto3 inside the
Floci image.
