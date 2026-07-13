#!/usr/bin/env ruby

require_relative "release_contract"

tag, expected_sha = ARGV
unless tag && expected_sha && ARGV.length == 2
  warn "usage: ruby scripts/manual/validate_release_manuals.rb TAG EXPECTED_SHA"
  exit 1
end

errors = ManualDocs::ReleaseContract.new(
  repository_root: Dir.pwd,
  tag: tag,
  expected_sha: expected_sha,
).errors

unless errors.empty?
  warn errors.join("\n")
  exit 1
end

puts "Release manuals are compatible with #{tag} (#{expected_sha})."
