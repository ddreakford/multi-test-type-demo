#!/usr/bin/env bash
#
# convert-to-docx.sh — Convert the Markdown setup guide to .docx format
#
# Usage:
#   ./scripts/convert-to-docx.sh                  # converts the setup guide
#   ./scripts/convert-to-docx.sh path/to/file.md  # converts a specific file
#
# Requires: pandoc (brew install pandoc)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

DEFAULT_INPUT="$PROJECT_ROOT/docs/TestAutomationDemo_SetupGuide.md"
INPUT="${1:-$DEFAULT_INPUT}"

if [[ ! -f "$INPUT" ]]; then
  echo "Error: Input file not found: $INPUT" >&2
  exit 1
fi

if ! command -v pandoc &>/dev/null; then
  echo "Error: pandoc is not installed. Install with: brew install pandoc" >&2
  exit 1
fi

OUTPUT="${INPUT%.md}.docx"

pandoc "$INPUT" \
  -f gfm \
  -t docx \
  --resource-path="$(dirname "$INPUT")" \
  -o "$OUTPUT"

echo "Created: $OUTPUT"
