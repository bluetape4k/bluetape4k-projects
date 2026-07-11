#!/usr/bin/env ruby

require "json"
require_relative "manual_contract"

inventory_path = ARGV.fetch(0, "build/manual/module-inventory.json")
manifest_path = ARGV.fetch(1, "docs/manual/manifest.yaml")

begin
  inventory = JSON.parse(File.read(inventory_path))
rescue Errno::ENOENT
  abort("module inventory not found: #{inventory_path}")
rescue JSON::ParserError => error
  abort("module inventory JSON is invalid: #{error.message}")
end

errors = ManualDocs::Validator.new(
  inventory: inventory,
  manifest_path: manifest_path,
  repository_root: Dir.pwd,
).errors

abort(errors.join("\n")) unless errors.empty?
puts "Manuals are aligned."
