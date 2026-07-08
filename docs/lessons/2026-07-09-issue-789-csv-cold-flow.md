# Issue 789 - CSV Cold Flow File Readers

## Context

File-based suspending CSV/TSV reader overloads opened `FileInputStream` before
returning the `Flow`. That violated the documented cold-Flow contract and made
the returned flow one-shot.

## Decision

Move file opening into the flow builder and close the stream with `use` during
collection. Keep InputStream overloads unchanged because callers own those
streams.

## Outcome

Regression tests now prove that file-backed suspending readers defer missing
file failures until collection, survive recollection after early termination,
and cover CSV/TSV plus transform overload behavior.

## Future Guidance

File-backed `Flow` APIs must open resources inside collection, not before
returning the flow. When the flow wraps blocking file I/O, run upstream work on
`Dispatchers.IO` and test both cold creation and recollection.
