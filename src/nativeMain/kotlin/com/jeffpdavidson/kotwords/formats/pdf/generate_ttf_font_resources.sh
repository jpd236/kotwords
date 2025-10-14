#!/bin/bash
#
# Script to generate Kotlin-readable constants containing encoded TTF font data.
#
# Kotlin Native does not currently support embedding standard resource files (see
# https://youtrack.jetbrains.com/issue/KT-39194). As a workaround, we gzip-compress the files, encode them with base64,
# and make them string constants. Note that while Java class files have smaller string length limits, this does not
# impact Kotlin native compilation.
#
# Usage: ./generate_ttf_font_resources.sh src/commonTest/resources/pdf/fonts/*.ttf

if [ "$#" -eq 0 ]; then
    echo "Usage: $0 <ttf-files...>"
    exit 1
fi

SCRIPT_DIR="$( dirname -- "${BASH_SOURCE[0]}" )"
OUTPUT_FILE="${SCRIPT_DIR}/TtfFonts.kt"
PACKAGE_NAME="com.jeffpdavidson.kotwords.formats.pdf"

echo "Generating $OUTPUT_FILE..."

cat <<EOF > "$OUTPUT_FILE"
package $PACKAGE_NAME

/** Auto-generated TTF font resources, compressed with gzip and base64-encoded. */
internal object TtfFonts {
EOF

for font_file in "$@"; do
    echo "  Encoding $font_file..."
    filename=$(basename "$font_file")
    var_name=$(echo "$filename" | tr '[:lower:]' '[:upper:]' | sed 's/[-.]/_/g')
    b64_data=$(gzip -c -9 "$font_file" | base64 | tr -d '\n')
    echo "" >> $OUTPUT_FILE
    echo "    const val ${var_name}_BASE64 =" >> "$OUTPUT_FILE"
    echo "        \"$b64_data\"" >> $OUTPUT_FILE
done

echo "}" >> "$OUTPUT_FILE"
echo "Generated $OUTPUT_FILE successfully."