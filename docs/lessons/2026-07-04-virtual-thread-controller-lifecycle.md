# Virtual thread controller lifecycle

## 배경

이슈 #952는 `AbstractVirtualThreadController`가 Spring bean destruction path 없이
shared virtual-thread executor를 노출한다는 점을 확인했다.

## 결정

호환성을 위해 public `virtualThreadExecutor` accessor는 유지하되 controller가
`@PreDestroy`로 현재 executor를 닫게 한다. Shutdown 이후 다른 Spring context가
접근하면 accessor가 executor를 다시 만든다.

## 결과

Controller bean shutdown은 이제 executor를 닫고, 반복적인 test/application context
생성은 닫힌 executor를 재사용하지 않는다.

## 검증

- `./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.virtualthread.AbstractVirtualThreadControllerTest'`

## 향후 지침

Executor나 coroutine resource를 노출하는 public controller base class는 Spring
destruction callback을 소유해야 하며, 테스트는 direct destroy와 context shutdown
경로를 모두 다뤄야 한다.
