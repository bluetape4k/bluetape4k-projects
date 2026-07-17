#!/usr/bin/env ruby

require "json"
require "yaml"

module ManualDocs
  class NavigationMetadataSync
    LOCALES = { "en" => "English", "ko" => "Korean" }.freeze

    attr_reader :changes

    def initialize(manifest_path:)
      @manifest_path = File.expand_path(manifest_path)
      @manual_root = File.dirname(@manifest_path)
      @changes = build_changes
    end

    def current?
      @changes.empty?
    end

    def write
      @changes.each { |change| File.write(change.fetch(:path), change.fetch(:content)) }
    end

    private

    def build_changes
      manifest = YAML.safe_load(File.read(@manifest_path))
      manifest.fetch("modules").flat_map do |entry|
        LOCALES.each_with_object([]) do |(locale, language), localized_changes|
          path = File.expand_path(entry.fetch(locale), @manual_root)
          content = File.read(path)
          expected = synchronize(content, entry, locale, language, path)
          next if expected == content
          localized_changes << { path: path, content: expected }
        end
      end
    end

    def synchronize(content, entry, locale, language, path)
      unless content.start_with?("---\n") && (frontmatter_end = content.index("\n---\n", 4))
        raise "#{relative(path)}: #{language} document must have YAML frontmatter"
      end

      title = entry.fetch("title").fetch(locale)
      frontmatter = content.slice(4, frontmatter_end - 4)
      frontmatter = replace_field(frontmatter, "title", JSON.generate(title))
      frontmatter = replace_field(frontmatter, "group", entry.fetch("group"))
      frontmatter = replace_field(frontmatter, "learningOrder", entry.fetch("learningOrder"))
      body = content.slice(frontmatter_end + 5..)
      unless body.sub!(/^# .+$/, "# #{title}")
        raise "#{relative(path)}: #{language} document must have a leading H1"
      end
      "---\n#{frontmatter}\n---\n#{body}"
    end

    def replace_field(frontmatter, key, value)
      line = "#{key}: #{value}"
      return frontmatter.sub(/^#{Regexp.escape(key)}:.*$/, line) if frontmatter.match?(/^#{Regexp.escape(key)}:/)

      anchor = key == "learningOrder" ? "group" : "manualId"
      unless frontmatter.match?(/^#{anchor}:.*$/)
        raise "frontmatter is missing #{anchor} before #{key}"
      end
      frontmatter.sub(/^#{anchor}:.*$/, "\\0\n#{line}")
    end

    def relative(path)
      path.delete_prefix(@manual_root + File::SEPARATOR)
    end
  end
end

if $PROGRAM_NAME == __FILE__
  check = ARGV.delete("--check")
  manifest_path = ARGV.fetch(0, "docs/manual/manifest.yaml")
  sync = ManualDocs::NavigationMetadataSync.new(manifest_path: manifest_path)
  if check
    abort("Manual navigation metadata is out of date (#{sync.changes.length} files).") unless sync.current?
    puts "Manual navigation metadata is current."
  else
    sync.write
    puts "Updated #{sync.changes.length} manual files."
  end
end
