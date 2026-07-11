require "json"
require "minitest/autorun"
require "tmpdir"

require_relative "export_manifest"

class ExportManifestTest < Minitest::Test
  def test_writes_deterministic_json_with_sorted_modules_and_keys
    Dir.mktmpdir("manual-manifest") do |root|
      source = File.join(root, "manifest.yaml")
      output = File.join(root, "manifest.json")
      File.write(source, <<~YAML)
        modules:
          - sourceDir: io/zeta
            id: zeta
          - sourceDir: io/alpha
            id: alpha
        schemaVersion: 1
      YAML

      exporter = ManualDocs::ManifestExporter.new(source_path: source, output_path: output)
      exporter.write

      parsed = JSON.parse(File.read(output))
      assert_equal %w[modules schemaVersion], parsed.keys
      assert_equal %w[alpha zeta], parsed["modules"].map { |entry| entry["id"] }
      assert File.binread(output).end_with?("\n")
      assert exporter.current?
    end
  end

  def test_detects_an_outdated_snapshot
    Dir.mktmpdir("manual-manifest") do |root|
      source = File.join(root, "manifest.yaml")
      output = File.join(root, "manifest.json")
      File.write(source, "schemaVersion: 1\nmodules: []\n")
      File.write(output, "{}\n")

      exporter = ManualDocs::ManifestExporter.new(source_path: source, output_path: output)
      refute exporter.current?
    end
  end
end
