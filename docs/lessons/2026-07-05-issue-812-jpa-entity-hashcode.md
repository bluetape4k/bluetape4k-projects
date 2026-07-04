# Issue #812 Lesson

## Context

`AbstractJpaEntity` compared transient entities through `equalProperties`, but returned `System.identityHashCode(this)` when `id == null`.

## Decision

Use the Hibernate-resolved entity class hash for transient entities and keep identifier-based hashing for persisted entities.

## Outcome

Equal transient entities now land in the same hash bucket, so hash-based collections treat them as one logical element before persistence assigns an identifier.

## Future Guidance

When entity equality has a transient business-signature path, add a hash-based collection regression test. Do not use identity hash for objects that can compare equal by business fields.
