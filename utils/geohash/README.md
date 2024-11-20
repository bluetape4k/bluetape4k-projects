# Module bluetape4k-geohash

An implementation of Geohashes in Kotlin.

The produced hashes, when using character precision (multiples of 5 bits) are compatible
to the reference implementation geohash.org.

You can however also encode Geohashes down to the full available precision of a long i.e. 64 bits.


Building/Testing the code
-------------------------

The geohash-java code can be built using Apache Maven.

Maven targets are the usual suspects.

	- clean    
    - compile
    - test
    - package   # pack a versioned jar containing the compiled class files

Digits and precision in km
--------------------------

| geohash length | lat bits | lng bits | lat error | lng error | ~km error |
|----------------|----------|----------|-----------|-----------|-----------|
| 1              | 2        | 3        | ±23       | ±23       | ±2500     |
| 2              | 5        | 5        | ±2.8      | ±5.6      | ±630      |
| 3              | 7        | 8        | ±0.70     | ±0.70     | ±78       |
| 4              | 10       | 10       | ±0.087    | ±0.18     | ±20       |
| 5              | 12       | 13       | ±0.022    | ±0.022    | ±2.4      |
| 6              | 15       | 15       | ±0.0027   | ±0.0055   | ±0.61     |
| 7              | 17       | 18       | ±0.00068  | ±0.00068  | ±0.076    |
| 8              | 20       | 20       | ±0.000085 | ±0.00017  | ±0.019    |

Compatibility
-------------------------
This branch ditches 1.6 compatibility. If you need that look at the release-1.0 branch
and artifacts in maven central. I will backport important bugfixes to the release-1.0 branch.
