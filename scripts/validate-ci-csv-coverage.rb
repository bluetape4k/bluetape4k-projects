# frozen_string_literal: true

require "shellwords"
require "yaml"

CSV_PATH = "io/csv/**"
CSV_TEST_TASK = ":bluetape4k-csv:test"
CSV_KOVER_TASK = ":bluetape4k-csv:koverXmlReport"
VALIDATOR_COMMAND = ["ruby", "scripts/validate-ci-csv-coverage.rb"].freeze
IO_OUTPUT = "${{ steps.filter.outputs.io }}"
TEST_IO_CONDITION = "${{ needs.changes.outputs.io == 'true' || needs.changes.outputs.shared == 'true' || github.event_name == 'workflow_dispatch' }}"
FORBIDDEN_GRADLE_OPTIONS = ["--exclude-task", "-m", "--dry-run"].freeze

def fail_validation(message)
  warn message
  exit 1
end

def gradle_tokens(command, label, allowed_excluded_tasks: [])
  normalized = command.gsub(/\\\r?\n/, " ").strip
  fail_validation("#{label} must use one simple Gradle invocation") if normalized.match?(/[\r\n#;&|<>$`]/)

  tokens = Shellwords.shellsplit(normalized)
  fail_validation("#{label} must run through ./gradlew") unless tokens.first == "./gradlew"

  forbidden = tokens.find do |token|
    FORBIDDEN_GRADLE_OPTIONS.include?(token) ||
      token.start_with?("-x=", "-x:", "--exclude-task=", "--dry-run=")
  end
  fail_validation("#{label} must execute its tasks; forbidden option #{forbidden}") if forbidden

  tokens.each_index do |index|
    next unless tokens[index] == "-x"

    excluded_task = tokens[index + 1]
    unless allowed_excluded_tasks.include?(excluded_task)
      fail_validation("#{label} must execute its tasks; forbidden excluded task #{excluded_task || '(missing)'}")
    end
  end
  tokens
rescue ArgumentError => error
  fail_validation("invalid #{label}: #{error.message}")
end

def require_once(values, expected, label)
  count = values.count(expected)
  fail_validation("expected #{expected} exactly once in #{label}, found #{count}") unless count == 1
end

workflow_path = ARGV.fetch(0, ".github/workflows/ci.yml")
workflow = YAML.safe_load(File.read(workflow_path), aliases: true)
jobs = workflow.fetch("jobs")

changes_job = jobs.fetch("changes")
change_steps = changes_job.fetch("steps")
validator_steps = change_steps.select do |step|
  command = step["run"]
  command && Shellwords.shellsplit(command) == VALIDATOR_COMMAND
end
fail_validation("expected the CSV CI validator exactly once in changes job, found #{validator_steps.size}") unless validator_steps.one?

validator_step = validator_steps.first
fail_validation("CSV CI validator must run unconditionally") if validator_step.key?("if")
fail_validation("CSV CI validator must fail the changes job") if validator_step.key?("continue-on-error")

io_output = changes_job.fetch("outputs").fetch("io")
fail_validation("changes.io output must use steps.filter.outputs.io") unless io_output == IO_OUTPUT

filter_step = change_steps.find { |step| step["id"] == "filter" }
fail_validation("changes job must define the paths-filter step") unless filter_step

filters = YAML.safe_load(filter_step.fetch("with").fetch("filters"), aliases: true)
io_paths = filters.fetch("io")
require_once(io_paths, CSV_PATH, "changes.io paths")

test_io_job = jobs.fetch("test-io")
fail_validation("test-io job must depend on changes") unless Array(test_io_job.fetch("needs")).include?("changes")
fail_validation("test-io job must use the io scheduling condition") unless test_io_job.fetch("if") == TEST_IO_CONDITION

io_steps = test_io_job.fetch("steps")
test_step = io_steps.find { |step| step["name"] == "Test io modules" }
kover_step = io_steps.find { |step| step["name"] == "Generate Kover XML report" }
fail_validation("test-io job must define Test io modules") unless test_step
fail_validation("test-io job must define Generate Kover XML report") unless kover_step
fail_validation("Test io modules must run unconditionally within test-io") if test_step.key?("if")
fail_validation("Test io modules must fail the test-io job") if test_step.key?("continue-on-error")

test_tokens = gradle_tokens(test_step.fetch("with").fetch("command"), "CSV test command")
kover_tokens = gradle_tokens(kover_step.fetch("run"), "CSV Kover command", allowed_excluded_tasks: ["test"])
fail_validation("Test io modules must include #{CSV_TEST_TASK}") unless test_tokens.include?(CSV_TEST_TASK)
fail_validation("Generate Kover XML report must include #{CSV_KOVER_TASK}") unless kover_tokens.include?(CSV_KOVER_TASK)

task_tokens = io_steps.flat_map do |step|
  commands = [step["run"], step.dig("with", "command")].compact
  commands.select { |command| [CSV_TEST_TASK, CSV_KOVER_TASK].any? { |task| command.include?(task) } }
          .map do |command|
            allowed_excluded_tasks = command.include?(CSV_KOVER_TASK) ? ["test"] : []
            gradle_tokens(command, "test-io CSV command", allowed_excluded_tasks: allowed_excluded_tasks)
          end
end.flatten
require_once(task_tokens, CSV_TEST_TASK, "all test-io commands")
require_once(task_tokens, CSV_KOVER_TASK, "all test-io commands")

puts "CI CSV coverage is valid"
