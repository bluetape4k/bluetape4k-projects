#!/usr/bin/env ruby

require "fileutils"
require "json"
require "yaml"

module ManualDocs
  class ManifestExporter
    def initialize(source_path:, output_path:)
      @source_path = source_path
      @output_path = output_path
    end

    def write
      FileUtils.mkdir_p(File.dirname(@output_path))
      File.write(@output_path, rendered)
    end

    def current?
      File.file?(@output_path) && File.binread(@output_path) == rendered.b
    end

    private

    def rendered
      JSON.pretty_generate(normalized_manifest) + "\n"
    end

    def normalized_manifest
      manifest = YAML.safe_load(File.read(@source_path))
      if manifest.is_a?(Hash) && manifest["modules"].is_a?(Array)
        manifest = manifest.merge("modules" => manifest["modules"].sort_by { |entry| entry.fetch("id") })
      end
      sort_keys(manifest)
    end

    def sort_keys(value)
      case value
      when Hash
        value.keys.sort.each_with_object({}) do |key, result|
          result[key] = sort_keys(value[key])
        end
      when Array
        value.map { |entry| sort_keys(entry) }
      else
        value
      end
    end
  end
end

if $PROGRAM_NAME == __FILE__
  check = ARGV.delete("--check")
  source_path = ARGV.fetch(0, "docs/manual/manifest.yaml")
  output_path = ARGV.fetch(1, "docs/manual/generated/manifest.json")
  exporter = ManualDocs::ManifestExporter.new(source_path: source_path, output_path: output_path)

  if check
    abort("Manual manifest snapshot is out of date.") unless exporter.current?
    puts "Manual manifest snapshot is current."
  else
    exporter.write
    puts "Manual manifest snapshot written."
  end
end
