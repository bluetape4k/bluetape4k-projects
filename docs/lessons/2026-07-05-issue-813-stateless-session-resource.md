# Issue #813 Stateless Session Resource Binding

## Context

`StatelessSessionFactoryBean` stored provider-created `StatelessSession`
instances in Spring's transaction resource map using the `SessionFactory` itself
as the key. Spring JPA transaction infrastructure also uses the factory key for
its own resource binding, so the stateless proxy path could collide with the
active `EntityManager` lifecycle.

## Decision

Use a dedicated transaction resource key for stateless sessions. The key is tied
to the exact `SessionFactory` identity but has a separate key type, so it cannot
collide with Spring's ordinary `SessionFactory` or `EntityManager` resources.

## Outcome

- The injected `StatelessSession` proxy reuses the same stateless session inside
  one transaction.
- The proxy no longer binds a `StatelessSession` under the raw `SessionFactory`
  key.
- The exact resource created by the factory is unbound and closed when the
  transaction completes.
- Calls outside an active transaction still fail fast.

## Future Rule

When integrating custom resources with Spring's
`TransactionSynchronizationManager`, never reuse a framework-owned resource key
for a different lifecycle participant. Use a dedicated key type and test both
same-transaction reuse and after-completion cleanup.
