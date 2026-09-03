# frozen_string_literal: true

require "open3"
require "rbconfig"
require "tempfile"

ROOT = File.expand_path("..", __dir__)
VALIDATOR = File.join(__dir__, "validate-ci-csv-coverage.rb")
WORKFLOW = File.join(ROOT, ".github", "workflows", "ci.yml")

def run_validator(workflow = WORKFLOW)
  Open3.capture3(RbConfig.ruby, VALIDATOR, workflow)
end

def assert_success(stdout, stderr, status, label)
  return if status.success?

  warn "#{label} failed: #{stdout}\n#{stderr}"
  exit 1
end

def assert_failure(stdout, stderr, status, label, expected_message)
  if status.success? || ![stdout, stderr].any? { |output| output.include?(expected_message) }
    warn "#{label} unexpectedly passed or emitted the wrong error: #{stdout}\n#{stderr}"
    exit 1
  end
end

stdout, stderr, status = run_validator
assert_success(stdout, stderr, status, "live CI manifest")

Tempfile.create(["ci-csv-coverage-", ".yml"]) do |file|
  content = File.read(WORKFLOW)
  csv_kover_index = content.index(":bluetape4k-tink:koverXmlReport")
  exclude_index = content.index("-x test", csv_kover_index)
  raise "CSV Kover exclusion fixture was not found" unless exclude_index

  updated = content.dup
  updated[exclude_index, "-x test".length] = "-x :bluetape4k-csv:koverXmlReport"
  file.write(updated)
  file.flush

  stdout, stderr, status = run_validator(file.path)
  assert_failure(stdout, stderr, status, "excluded CSV Kover task", "forbidden excluded task")
end

puts "CSV CI coverage validator regression tests passed"
