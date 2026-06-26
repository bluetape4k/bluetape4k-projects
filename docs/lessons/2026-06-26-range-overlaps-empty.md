# Lessons Learned - Range Empty Overlap (2026-06-26)

Related issue: #783
Affected module: `:bluetape4k-core`

## L1: Empty ranges must short-circuit overlap checks

### Problem

`Range.overlaps()` compared only endpoint ordering and boundary inclusiveness. Empty ranges such as `(1, 1)`,
`[1, 1)`, and `(1, 1]` could therefore report an overlap with a non-empty range even though no common element
exists.

### Lesson

When a range operation's contract is element-based, check `isEmpty()` before applying endpoint comparisons.
Boundary comparisons alone are not enough because empty ranges can still sit inside a non-empty interval.

### Future guard

Range helper changes should include regression coverage for empty operands in both receiver and argument positions,
plus existing boundary inclusiveness tests for non-empty ranges.
