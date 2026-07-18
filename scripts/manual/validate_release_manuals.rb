#!/usr/bin/env ruby

require_relative "release_contract"

tag, expected_sha = ARGV
unless tag && expected_sha && ARGV.length == 2
  warn "usage: ruby scripts/manual/validate_release_manuals.rb TAG EXPECTED_SHA"
  exit 1
end

result = ManualDocs::ReleaseContract.new(
  repository_root: File.expand_path("../..", __dir__),
  tag: tag,
  expected_sha: expected_sha,
).validate

unless result.errors.empty?
  warn result.errors.join("\n")
  exit 1
end

summary = "Release manuals are compatible with #{tag} (#{expected_sha}): #{result.checked_count} checked, 0 missing."
if result.skipped_manual_count.positive?
  summary += " #{result.skipped_manual_count} snapshot-only manuals skipped."
end
puts summary
