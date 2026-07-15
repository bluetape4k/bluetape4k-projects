# frozen_string_literal: true

require "shellwords"
require "yaml"

SERVER_TASK = ":bluetape4k-mock-web-server:koverXmlReport"
WEBFLUX_TASK = ":bluetape4k-mock-webflux-server:koverXmlReport"
KOVER_TASKS = [SERVER_TASK, WEBFLUX_TASK].freeze

def fail_validation(message)
  warn message
  exit 1
end

def gradle_tokens(command)
  normalized = command.gsub(/\\\r?\n/, " ").strip
  fail_validation("mock server Kover tasks require one simple Gradle invocation") if normalized.match?(/[\r\n#;&|<>$`]/)

  tokens = Shellwords.shellsplit(normalized)
  fail_validation("mock server Kover tasks must run through ./gradlew") unless tokens.first == "./gradlew"
  tokens
rescue ArgumentError => error
  fail_validation("invalid mock server Kover command: #{error.message}")
end

workflow_path = ARGV.fetch(0, ".github/workflows/nightly-tests.yml")
workflow = YAML.safe_load(File.read(workflow_path), aliases: true)
steps = workflow.fetch("jobs").fetch("test-misc").fetch("steps")
commands = steps.map { |step| step["run"] }.compact
task_commands = commands.select { |command| KOVER_TASKS.any? { |task| command.include?(task) } }
invocations = task_commands.map { |command| gradle_tokens(command) }

task_counts = KOVER_TASKS.to_h do |task|
  [task, invocations.sum { |tokens| tokens.count(task) }]
end

invalid_counts = task_counts.reject { |_task, count| count == 1 }
fail_validation("expected each mock server Kover task exactly once: #{invalid_counts}") unless invalid_counts.empty?

if invocations.any? { |tokens| KOVER_TASKS.all? { |task| tokens.include?(task) } }
  fail_validation("mock server Kover tasks must use separate Gradle invocations")
end

puts "nightly mock server Kover isolation is valid"
