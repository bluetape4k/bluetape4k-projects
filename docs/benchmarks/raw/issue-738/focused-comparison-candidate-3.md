| benchmark | threads | baseline ops/s | candidate 2 ops/s | candidate 3 ops/s | candidate 3 delta |
|---|---:|---:|---:|---:|---:|
| `single.ksuidMillisDefaultString` | 1 | 98.918 | 94.135 | 91.332 | -7.67% |
| `single.ksuidMillisFixedInstantString` | 1 | 94.588 | 98.979 | 98.104 | +3.72% |
| `single.ksuidMillisWithUniqueness` | 1 | 82.114 | 82.067 | 82.655 | +0.66% |
| `single.ksuidSecondsDefaultString` | 1 | 86.764 | 88.216 | 86.766 | +0.00% |
| `single.ksuidSecondsFixedInstantString` | 1 | 79.546 | 86.925 | 88.611 | +11.40% |
| `single.ksuidSecondsWithUniqueness` | 1 | 71.890 | 75.532 | 74.321 | +3.38% |
| `single.snowflakeDefaultGenerateOnly` | 1 | 62.515 | 62.532 | 62.530 | +0.02% |
| `single.snowflakeDefaultWithUniqueness` | 1 | 62.504 | 62.506 | 62.495 | -0.01% |
| `single.ulidMonotonicString` | 1 | 577.327 | 581.029 | 580.638 | +0.57% |
| `single.ulidMonotonicValueOnly` | 1 | 831.422 | 836.792 | 847.083 | +1.88% |
| `single.ulidMonotonicWithUniqueness` | 1 | 360.095 | 360.445 | 365.002 | +1.36% |
| `concurrent.ksuidMillisDefaultString` | 12 | 68.632 | 69.767 | 70.704 | +3.02% |
| `concurrent.ksuidMillisWithUniqueness` | 12 | 53.496 | 61.162 | 56.906 | +6.37% |
| `concurrent.ksuidSecondsDefaultString` | 12 | 60.633 | 64.797 | 62.924 | +3.78% |
| `concurrent.ksuidSecondsWithUniqueness` | 12 | 52.908 | 53.841 | 53.445 | +1.01% |
| `concurrent.snowflakeDefaultGenerateOnly` | 12 | 62.415 | 62.144 | 62.625 | +0.34% |
| `concurrent.snowflakeDefaultWithUniqueness` | 12 | 62.104 | 62.084 | 62.245 | +0.23% |
| `concurrent.ulidMonotonicString` | 12 | 38.331 | 41.071 | 45.414 | +18.48% |
| `concurrent.ulidMonotonicWithUniqueness` | 12 | 35.542 | 39.690 | 45.857 | +29.02% |
