# Lessons Learned — R2DBC PostgreSQL JSON Conversion (2026-06-26)

**Issue**: #824
**Module**: `:bluetape4k-r2dbc`

## L1: Converter fallbacks can become silent data loss

### Problem

The PostgreSQL JSON converters logged Jackson failures and returned valid empty values. A malformed database value became an empty map, and an unserializable application value became `{}`.

### Lesson

Converters that sit on persistence boundaries should fail with the original cause unless the API explicitly models fallback semantics. A valid empty value is not a safe substitute for invalid stored data or failed serialization.

### Future Guard

For R2DBC converter changes, add regression tests for both the success path and the failure path. Failure-path tests should assert the exception type and cause, not only that logging happened.
