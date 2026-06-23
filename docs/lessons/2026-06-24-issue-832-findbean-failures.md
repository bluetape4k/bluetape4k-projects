# Issue 832 - findBean failure boundaries

## Context

`BeanFactory.findBean(...)` is an optional lookup helper. It should return
`null` when the requested bean is absent, but it must not hide Spring
configuration failures.

The previous implementation wrapped `get(...)` in `runCatching` and returned
`null` for every exception. That made bean creation failures and duplicate-bean
ambiguity look the same as an optional missing bean.

## Decision

Keep the optional lookup contract for absent beans only:

- `NoSuchBeanDefinitionException` maps to `null`
- `NoUniqueBeanDefinitionException` is rethrown even though it is a
  `NoSuchBeanDefinitionException` subclass
- bean creation, type, and configuration failures propagate

The shared `findOptionalBean` helper keeps the exception boundary identical
across class-based, named, and constructor-argument `findBean` overloads.

## Follow-up Guard

When adding optional Spring lookup helpers, do not catch broad `BeansException`
unless every Spring configuration failure is intentionally optional. Add tests
for missing-bean, creation-failure, and ambiguity boundaries together.
