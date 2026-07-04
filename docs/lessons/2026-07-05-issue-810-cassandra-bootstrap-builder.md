# Issue 810 - Cassandra Bootstrap Builder

## Context

`CqlSessionProvider` created an admin session to bootstrap the keyspace before it applied the caller builder block. Secured clusters could fail even when the final session builder contained valid credentials, TLS, contact point, or driver options.

## Decision

Apply the shared builder block to both bootstrap and final sessions. Bind the provider-managed keyspace after bootstrap, and expose explicit bootstrap/session builder overloads for callers whose final session needs settings that are invalid before the keyspace exists.

## Outcome

The regression test uses a bare `CqlSessionBuilder` supplier and relies on the caller builder block for contact point and datacenter. It fails without bootstrap builder application and passes with the new behavior.

## Future Guidance

Do not hide administrative side effects behind a final-session-only builder. When a helper performs DDL before returning a client/session, document which builder settings apply to the admin path and provide separate hooks when some settings are phase-specific.

