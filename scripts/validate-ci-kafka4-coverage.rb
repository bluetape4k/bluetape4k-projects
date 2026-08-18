# frozen_string_literal: true

require "optparse"
require "shellwords"
require "yaml"

KAFKA4_PATH = "infra/kafka4/**"
KAFKA4_TEST_TASK = ":bluetape4k-kafka4:test"
KAFKA4_KOVER_TASK = ":bluetape4k-kafka4:koverXmlReport"
KAFKA_PATH_FILTER = "kafka-infra"
KAFKA_JOB = "test-kafka-infra"
KAFKA_JOB_CONDITION = "${{ needs.changes.outputs['kafka-infra'] == 'true' || needs.changes.outputs.shared == 'true' || github.event_name == 'workflow_dispatch' }}"
FORBIDDEN_GRADLE_OPTIONS = ["-x", "--exclude-task", "-m", "--dry-run"].freeze

def fail_validation(message)
  warn message
  exit 1
end

def require_once(values, expected, label)
  count = values.count(expected)
  fail_validation("expected #{expected} exactly once in #{label}, found #{count}") unless count == 1
end

def gradle_tokens(command, label)
  normalized = command.gsub(/\\\r?\n/, " ").strip
  fail_validation("#{label} must use one simple Gradle invocation") if normalized.match?(/[\r\n#;&|<>$`]/)

  tokens = Shellwords.shellsplit(normalized)
  fail_validation("#{label} must run through ./gradlew") unless tokens.first == "./gradlew"

  forbidden = tokens.find do |token|
    FORBIDDEN_GRADLE_OPTIONS.include?(token) ||
      token.start_with?("-x=", "-x:", "--exclude-task=", "--dry-run=")
  end
  fail_validation("#{label} must execute its tasks; forbidden option #{forbidden}") if forbidden
  tokens
rescue ArgumentError => error
  fail_validation("invalid #{label}: #{error.message}")
end

def command_tokens(step, label)
  command = step["run"] || step.dig("with", "command")
  fail_validation("#{label} must define a command") unless command

  gradle_tokens(command, label)
end

options = {
  workflow: ".github/workflows/ci.yml",
  settings: "settings.gradle.kts",
  repo_root: ".",
}
OptionParser.new do |parser|
  parser.on("--workflow PATH", "CI workflow path") { |value| options[:workflow] = value }
  parser.on("--settings PATH", "Gradle settings path") { |value| options[:settings] = value }
  parser.on("--repo-root PATH", "repository root") { |value| options[:repo_root] = value }
end.parse!

repo_root = File.expand_path(options[:repo_root])
workflow_path = File.expand_path(options[:workflow], repo_root)
settings_path = File.expand_path(options[:settings], repo_root)

workflow = YAML.safe_load(File.read(workflow_path), aliases: true)
jobs = workflow.fetch("jobs")

module_path = File.join(repo_root, "infra", "kafka4")
fail_validation("Kafka4 module directory is missing: #{module_path}") unless Dir.exist?(module_path)
fail_validation("Kafka4 module build file is missing: #{module_path}/build.gradle.kts") unless File.file?(File.join(module_path, "build.gradle.kts"))

settings = File.read(settings_path)
unless settings.match?(/includeModules\(\s*[\"']infra[\"']\s*,\s*withBaseDir\s*=\s*false\s*\)/)
  fail_validation("settings.gradle.kts does not auto-register infra modules: #{settings_path}")
end

changes_job = jobs.fetch("changes")
changes_outputs = changes_job.fetch("outputs")
expected_output = "${{ steps.filter.outputs.#{KAFKA_PATH_FILTER} }}"
fail_validation("changes.#{KAFKA_PATH_FILTER} output must use #{expected_output}") unless changes_outputs.fetch(KAFKA_PATH_FILTER) == expected_output

filter_step = changes_job.fetch("steps").find { |step| step["id"] == "filter" }
fail_validation("changes job must define the paths-filter step") unless filter_step

filters = YAML.safe_load(filter_step.fetch("with").fetch("filters"), aliases: true)
kafka_paths = filters.fetch(KAFKA_PATH_FILTER)
require_once(kafka_paths, "infra/kafka/**", "#{KAFKA_PATH_FILTER} paths")
require_once(kafka_paths, KAFKA4_PATH, "#{KAFKA_PATH_FILTER} paths")

kafka_job = jobs.fetch(KAFKA_JOB)
needs = Array(kafka_job.fetch("needs"))
fail_validation("#{KAFKA_JOB} job must depend on changes") unless needs.include?("changes")
unless kafka_job.fetch("if") == KAFKA_JOB_CONDITION
  fail_validation("#{KAFKA_JOB} job condition must select the kafka-infra output")
end

steps = kafka_job.fetch("steps")
test_step = steps.find { |step| step["name"] == "Test Kafka infra modules" }
kover_step = steps.find { |step| step["name"] == "Generate Kover XML report" }
fail_validation("#{KAFKA_JOB} job must define Test Kafka infra modules") unless test_step
fail_validation("#{KAFKA_JOB} job must define Generate Kover XML report") unless kover_step
fail_validation("Test Kafka infra modules must not be conditional") if test_step.key?("if")
fail_validation("Test Kafka infra modules must fail the #{KAFKA_JOB} job") if test_step.key?("continue-on-error")
fail_validation("Generate Kover XML report must always run") unless kover_step["if"] == "always()"

test_tokens = command_tokens(test_step, "Kafka test command")
kover_tokens = command_tokens(kover_step, "Kafka Kover command")
require_once(test_tokens, ":bluetape4k-kafka:test", "Kafka test command")
require_once(test_tokens, KAFKA4_TEST_TASK, "Kafka test command")
require_once(kover_tokens, ":bluetape4k-kafka:koverXmlReport", "Kafka Kover command")
require_once(kover_tokens, KAFKA4_KOVER_TASK, "Kafka Kover command")

all_task_tokens = steps.flat_map do |step|
  [step["run"], step.dig("with", "command")].compact.flat_map do |command|
    command_tokens({ "run" => command }, "Kafka CI command")
  end
end
require_once(all_task_tokens, KAFKA4_TEST_TASK, "all Kafka CI commands")
require_once(all_task_tokens, KAFKA4_KOVER_TASK, "all Kafka CI commands")

test_results_step = steps.find { |step| step["name"] == "Upload test results" }
coverage_step = steps.find { |step| step["name"] == "Upload coverage report" }
fail_validation("#{KAFKA_JOB} job must upload test results") unless test_results_step
fail_validation("#{KAFKA_JOB} job must upload Kover coverage") unless coverage_step

test_results_path = test_results_step.dig("with", "path").to_s
fail_validation("Kafka test results artifact must include XML test results") unless test_results_path.include?("**/build/test-results/test/*.xml")

coverage_path = coverage_step.dig("with", "path").to_s
fail_validation("Kafka coverage artifact must include Kover reports") unless coverage_path.include?("**/build/reports/kover/")

puts "CI Kafka4 coverage is valid"
