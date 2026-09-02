# frozen_string_literal: true

require "fileutils"
require "open3"
require "rbconfig"
require "tempfile"

ROOT = File.expand_path("..", __dir__)
VALIDATOR = File.join(__dir__, "validate-ci-kafka4-coverage.rb")
WORKFLOW = File.join(ROOT, ".github", "workflows", "ci.yml")
SETTINGS = File.join(ROOT, "settings.gradle.kts")

def run_validator(workflow: WORKFLOW, settings: SETTINGS, repo_root: ROOT)
  Open3.capture3(
    RbConfig.ruby,
    VALIDATOR,
    "--workflow", workflow,
    "--settings", settings,
    "--repo-root", repo_root,
  )
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

def with_workflow_variant(source, replacement)
  Tempfile.create(["ci-kafka4-", ".yml"]) do |file|
    content = File.read(WORKFLOW)
    updated = yield(content)
    raise "workflow fixture was not changed" if updated == content

    file.write(updated)
    file.flush
    stdout, stderr, status = run_validator(workflow: file.path)
    replacement.call(stdout, stderr, status)
  end
end

stdout, stderr, status = run_validator
assert_success(stdout, stderr, status, "live CI manifest")

with_workflow_variant(nil, ->(out, err, result) {
  assert_failure(out, err, result, "Kafka4 path filter", "infra/kafka4/**")
}) do |content|
  content.sub("              - 'infra/kafka4/**'", "              - 'infra/kafka4-missing/**'")
end

with_workflow_variant(nil, ->(out, err, result) {
  assert_failure(out, err, result, "Kafka4 test task", ":bluetape4k-kafka4:test")
}) do |content|
  content.sub(":bluetape4k-kafka4:test", ":bluetape4k-kafka4:compileTestKotlin")
end

with_workflow_variant(nil, ->(out, err, result) {
  assert_failure(out, err, result, "Kafka4 Kover task", ":bluetape4k-kafka4:koverXmlReport")
}) do |content|
  content.sub(":bluetape4k-kafka4:koverXmlReport", ":bluetape4k-kafka4:tasks")
end

with_workflow_variant(nil, ->(out, err, result) {
  assert_failure(out, err, result, "excluded Kafka4 Kover task", "forbidden excluded task")
}) do |content|
  kafka4_kover_index = content.index(":bluetape4k-kafka4:koverXmlReport")
  exclude_index = content.index("-x test", kafka4_kover_index)
  raise "Kafka4 Kover exclusion fixture was not found" unless exclude_index

  content.dup.tap do |updated|
    updated[exclude_index, "-x test".length] = "-x :bluetape4k-kafka4:koverXmlReport"
  end
end

with_workflow_variant(nil, ->(out, err, result) {
  assert_failure(out, err, result, "Kafka4 job condition", "kafka-infra output")
}) do |content|
  content.sub("needs.changes.outputs['kafka-infra'] == 'true'", "needs.changes.outputs['wrong-filter'] == 'true'")
end

with_workflow_variant(nil, ->(out, err, result) {
  assert_failure(out, err, result, "Kafka4 test condition", "must not be conditional")
}) do |content|
  content.sub(
    "      - name: Test Kafka infra modules\n        uses: nick-fields/retry@v4",
    "      - name: Test Kafka infra modules\n        if: ${{ false }}\n        uses: nick-fields/retry@v4",
  )
end

with_workflow_variant(nil, ->(out, err, result) {
  assert_failure(out, err, result, "Kafka4 Kover condition", "must always run")
}) do |content|
  content.sub(
    "      - name: Generate Kover XML report\n        if: always()\n        run: |\n          ./gradlew \\\n            :bluetape4k-kafka:koverXmlReport",
    "      - name: Generate Kover XML report\n        if: failure()\n        run: |\n          ./gradlew \\\n            :bluetape4k-kafka:koverXmlReport",
  )
end

with_workflow_variant(nil, ->(out, err, result) {
  assert_failure(out, err, result, "Kafka4 test-results artifact", "XML test results")
}) do |content|
  content.sub(
    "name: test-results-kafka-infra\n          path: '**/build/test-results/test/*.xml'",
    "name: test-results-kafka-infra\n          path: '**/build/test-results/test/*.txt'",
  )
end

with_workflow_variant(nil, ->(out, err, result) {
  assert_failure(out, err, result, "Kafka4 coverage artifact", "Kover reports")
}) do |content|
  content.sub(
    "name: coverage-kafka-infra\n          path: '**/build/reports/kover/'",
    "name: coverage-kafka-infra\n          path: '**/build/reports/jacoco/'",
  )
end

Dir.mktmpdir("ci-kafka4-registration-") do |directory|
  module_path = File.join(directory, "infra", "kafka4")
  FileUtils.mkdir_p(module_path)
  File.write(File.join(module_path, "build.gradle.kts"), "")
  workflow = File.join(directory, "ci.yml")
  settings = File.join(directory, "settings.gradle.kts")
  FileUtils.cp(WORKFLOW, workflow)
  FileUtils.cp(SETTINGS, settings)
  File.write(settings, "rootProject.name = \"fixture\"\n")

  stdout, stderr, status = run_validator(workflow: workflow, settings: settings, repo_root: directory)
  assert_failure(stdout, stderr, status, "Gradle module registration", "settings.gradle.kts")
end

puts "Kafka4 CI coverage validator regression tests passed"
