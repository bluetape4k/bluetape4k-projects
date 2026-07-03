# Lettuce Lock Duration And Token Preservation

## Context

Issue #949 found that Lettuce lock APIs converted invalid durations to Redis PX
values and cleared the local owner token before Redis release success was known.

## Decision

Validate lock durations at the API boundary and clear the local token only after
the Redis Lua release script confirms success.

## Outcome

Invalid lease/wait durations fail before Redis commands are issued. If a release
fails because the Redis key expired or no longer matches, the local token remains
available so callers can retry when release evidence becomes available.

## Verification

- `./gradlew :bluetape4k-lettuce:test --tests 'io.bluetape4k.redis.lettuce.lock.LettuceLockTest' --tests 'io.bluetape4k.redis.lettuce.lock.LettuceSuspendLockTest'`

## Future Guidance

Distributed lock implementations must validate Redis TTL inputs before command
construction and must not discard local ownership evidence until remote release
success is confirmed.
