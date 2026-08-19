#!/usr/bin/env ruby

require "yaml"

require_relative "release_contract"

USAGE = "usage: ruby scripts/manual/validate_release_manuals.rb --manifest PATH [TAG EXPECTED_SHA] | TAG EXPECTED_SHA"
REPOSITORY_ROOT = File.expand_path("../..", __dir__)

def manifest_provenance(repository_root, manifest_path)
  path = File.expand_path(manifest_path, repository_root)
  manifest = YAML.safe_load(File.read(path))
  unless manifest.is_a?(Hash) && manifest["releaseRef"].is_a?(String) && manifest["releaseCommit"].is_a?(String)
    raise ArgumentError, "manual manifest releaseRef/releaseCommit must be strings: #{path}"
  end

  [manifest["releaseRef"], manifest["releaseCommit"]]
rescue Errno::ENOENT, Psych::SyntaxError => error
  raise ArgumentError, "manual manifest could not be read: #{path}: #{error.message}"
end

begin
  arguments = ARGV.dup
  manifest_path = nil
  if arguments.first == "--manifest"
    arguments.shift
    manifest_path = arguments.shift
    abort(USAGE) unless manifest_path && !manifest_path.empty?
  end

  if arguments.empty? && manifest_path
    tag, expected_sha = manifest_provenance(REPOSITORY_ROOT, manifest_path)
  elsif arguments.length == 2
    tag, expected_sha = arguments
    if manifest_path
      manifest_tag, manifest_sha = manifest_provenance(REPOSITORY_ROOT, manifest_path)
      unless tag == manifest_tag && expected_sha.casecmp?(manifest_sha)
        abort(
          "manual manifest release provenance mismatch: " \
            "manifest=#{manifest_tag} (#{manifest_sha}), " \
            "arguments=#{tag} (#{expected_sha})",
        )
      end
    end
  else
    abort(USAGE)
  end

  result = ManualDocs::ReleaseContract.new(
    repository_root: REPOSITORY_ROOT,
    tag: tag,
    expected_sha: expected_sha,
  ).validate
rescue ArgumentError => error
  warn error.message
  exit 1
end

unless result.errors.empty?
  warn result.errors.join("\n")
  exit 1
end

summary = "Release manuals are compatible with #{tag} (#{expected_sha}): #{result.checked_count} checked, 0 missing."
if result.skipped_manual_count.positive?
  summary += " #{result.skipped_manual_count} snapshot-only manuals skipped."
end
puts summary
