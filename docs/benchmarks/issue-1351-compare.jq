def median:
  sort | length as $n |
  if $n == 0 then null
  elif $n % 2 == 1 then .[$n / 2 | floor]
  else (.[($n / 2) - 1] + .[$n / 2]) / 2
  end;

def max($a; $b):
  if $a > $b then $a else $b end;

def key:
  [.benchmark, (.threads | tostring), (.params | tojson)] | join("|");

def indexed:
  map({key: key, value: .}) | from_entries;

($b[0] | indexed) as $baseline |
($c[0] | indexed) as $candidate |
if (($baseline | keys | sort) != ($candidate | keys | sort)) then
  error("baseline/candidate benchmark cardinality or keys differ")
else
  [
    ($baseline | keys[]) as $name |
    ($baseline[$name]) as $before |
    ($candidate[$name]) as $after |
    ($before.primaryMetric.rawData | flatten | median) as $beforeMedian |
    ($after.primaryMetric.rawData | flatten | median) as $afterMedian |
    ($before.secondaryMetrics["gc.alloc.rate.norm"].score) as $beforeAlloc |
    ($after.secondaryMetrics["gc.alloc.rate.norm"].score) as $afterAlloc |
    (($before.secondaryMetrics["gc.alloc.rate.norm"].scoreError // 0) +
     ($after.secondaryMetrics["gc.alloc.rate.norm"].scoreError // 0)) as $allocError |
    {
      benchmark: $before.benchmark,
      threads: $before.threads,
      params: $before.params,
      throughput: {
        beforeMedian: $beforeMedian,
        afterMedian: $afterMedian,
        minimumAllowed: ($beforeMedian * 0.95),
        ratio: ($afterMedian / $beforeMedian),
        beforeScore: $before.primaryMetric.score,
        beforeScoreError: $before.primaryMetric.scoreError,
        beforeRawData: $before.primaryMetric.rawData,
        afterScore: $after.primaryMetric.score,
        afterScoreError: $after.primaryMetric.scoreError,
        afterRawData: $after.primaryMetric.rawData
      },
      allocation: {
        beforeScore: $beforeAlloc,
        beforeScoreError: $before.secondaryMetrics["gc.alloc.rate.norm"].scoreError,
        beforeRawData: $before.secondaryMetrics["gc.alloc.rate.norm"].rawData,
        afterScore: $afterAlloc,
        afterScoreError: $after.secondaryMetrics["gc.alloc.rate.norm"].scoreError,
        afterRawData: $after.secondaryMetrics["gc.alloc.rate.norm"].rawData,
        errorBudget: max(0.001; $allocError),
        maximumAllowed: ($beforeAlloc + max(0.001; $allocError))
      },
      pass: (($afterMedian >= $beforeMedian * 0.95) and
             ($afterAlloc <= $beforeAlloc + max(0.001; $allocError)))
    }
  ] as $rows |
  if ($rows | all(.pass)) then
    $rows
  else
    error("performance regression threshold failed")
  end
end
