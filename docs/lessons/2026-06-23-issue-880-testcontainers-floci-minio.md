# Lessons Learned - Testcontainers Floci and MinIO policy (#880, 2026-06-23)

Related issue: #880
Affected module: `:bluetape4k-testcontainers`

## L1: Deprecation policy must not make supported compatibility fixtures noisy

`MinIOServer` is still a supported fixture for direct MinIO compatibility
tests. Marking the class itself as deprecated made every internal factory,
launcher, and direct compatibility test look like migration debt even though
the 1.11.0 policy keeps this fixture available.

The class-level deprecation was removed, and a reflection contract test now
checks that `MinIOServer` remains non-deprecated while it is supported.

## L2: Default AWS/S3 emulator guidance belongs on the wrapper boundary

New AWS/S3 emulator tests should use the current AWS emulator path instead of
defaulting to MinIO. The durable guidance now lives in `MinIOServer` KDoc and
the `testing/testcontainers` README locale set: use `FlociServer` or
`MiniStackServer` for new AWS/S3 emulator coverage, and use `MinIOServer` only
for explicit MinIO compatibility behavior.

## L3: Pinned emulator image tags need contract tests

The Docker Hub release tag `1.5.27` was verified on 2026-06-23. `FlociServer`
keeps a pinned release tag for reproducible Testcontainers runs, and
`FlociServerTest` guards that value so future tag changes are deliberate.
