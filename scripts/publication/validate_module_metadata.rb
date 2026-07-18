#!/usr/bin/env ruby

require_relative "publication_module_metadata_audit"

paths = if ARGV.empty?
          Dir.glob("**/build/publications/*/module.json")
            .reject { |path| path.start_with?(".worktrees/") }
        else
          ARGV
        end

result = Publication::ModuleMetadataAudit.new(paths).validate
unless result.errors.empty?
  warn(result.errors.join("\n"))
  abort(
    "publication-module-metadata: failures=#{result.errors.length} " \
    "files=#{result.file_count} variants=#{result.variant_count} dependencies=#{result.dependency_count}",
  )
end

puts(
  "publication-module-metadata: failures=0 files=#{result.file_count} " \
  "variants=#{result.variant_count} dependencies=#{result.dependency_count}",
)
