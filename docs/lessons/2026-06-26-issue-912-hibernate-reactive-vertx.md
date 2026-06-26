# Lessons Learned - Hibernate Reactive Vert.x Alignment (2026-06-26)

Related issue: #912
Affected modules: `:bluetape4k-hibernate`, `:bluetape4k-hibernate-reactive`, `:bluetape4k-hibernate-cache-lettuce`

## L1: Hibernate Reactive must track both ORM and Vert.x lines

### Problem

`bluetape4k-hibernate-reactive` used Hibernate Reactive `4.3.3.Final` with the repository-wide Vert.x `5.1.3`.
The module failed at runtime because Hibernate Reactive called an internal Vert.x SQL client constructor that no longer
exists in the resolved Vert.x version.

### Lesson

Hibernate Reactive is coupled to both Hibernate ORM and Vert.x SQL client internals. When Vert.x is upgraded globally,
check the Hibernate Reactive POM line and align Hibernate ORM at the same time instead of bumping only one side.

### Future guard

Run `:bluetape4k-hibernate-reactive:test` after changing Hibernate ORM, Hibernate Reactive, or Vert.x versions. Also
check `dependencyInsight` for `hibernate-reactive-core`, `hibernate-core`, and Vert.x SQL client artifacts.
