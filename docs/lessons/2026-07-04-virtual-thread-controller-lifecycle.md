# Virtual Thread Controller Lifecycle

## Context

Issue #952 found that `AbstractVirtualThreadController` exposed a shared
virtual-thread executor without a Spring bean destruction path.

## Decision

Keep the public `virtualThreadExecutor` accessor for compatibility, but make the
controller close the current executor through `@PreDestroy`. The accessor
recreates the executor when a later Spring context accesses it after shutdown.

## Outcome

Controller bean shutdown now closes the executor while repeated test or
application context creation does not reuse a closed executor.

## Verification

- `./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.virtualthread.AbstractVirtualThreadControllerTest'`

## Future Guidance

Public controller base classes that expose executor or coroutine resources must
own a Spring destruction callback and tests should cover both direct destroy and
context shutdown paths.
