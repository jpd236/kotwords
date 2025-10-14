#!/bin/bash
#
# Script to generate Kotlin-readable tables of font metrics for the PDF built-in fonts from the core
# 14 AFM files originally distributed by Adobe. The original Adobe links are broken, but the files
# are mirrored elsewhere, e.g. https://github.com/tecnickcom/tc-font-core14-afms.
#
# Usage: ./generate_metrics.sh Times-Roman.afm Times-Bold.afm ...
#
# These AFM files were originally distributed under the following license:
#
# This file and the 14 PostScript(R) AFM files it accompanies may be used, copied,
# and distributed for any purpose and without charge, with or without modification,
# provided that all copyright notices are retained;
# that the AFM files are not distributed without this file;
# that all modifications to this file or any of the AFM files are prominently noted in the modified file(s);
# and that this paragraph is not modified.
# Adobe Systems has no responsibility or obligation to support the use of the AFM files.

if [ "$#" -eq 0 ]; then
    echo "Usage: $0 <afm-files...>"
    exit 1
fi

SCRIPT_DIR="$( dirname -- "${BASH_SOURCE[0]}" )"
OUTPUT_FILE="${SCRIPT_DIR}/BuiltInFontMetrics.kt"
PACKAGE_NAME="com.jeffpdavidson.kotwords.formats.pdf"

echo "Generating $OUTPUT_FILE..."

cat <<EOF > "$OUTPUT_FILE"
package $PACKAGE_NAME

/** Auto-generated font metrics from Adobe AFM files. */
internal object BuiltInFontMetrics {
EOF

for afm_file in "$@"; do
    echo "  Processing $afm_file..."

    awk '
    BEGIN {
        # Map Adobe Glyph Names to Unicode code points for CP1252 range 0x80-0x9F
        # Reference: https://adobe-type-tools.github.io/adobe-latin-charsets/adobe-latin-1.html
        nameToUnicode["Euro"] = 0x20AC;
        nameToUnicode["quotesinglbase"] = 0x201A;
        nameToUnicode["florin"] = 0x0192;
        nameToUnicode["quotedblbase"] = 0x201E;
        nameToUnicode["ellipsis"] = 0x2026;
        nameToUnicode["dagger"] = 0x2020;
        nameToUnicode["daggerdbl"] = 0x2021;
        nameToUnicode["circumflex"] = 0x02C6;
        nameToUnicode["perthousand"] = 0x2030;
        nameToUnicode["Scaron"] = 0x0160;
        nameToUnicode["guilsinglleft"] = 0x2039;
        nameToUnicode["OE"] = 0x0152;
        nameToUnicode["Zcaron"] = 0x017D;
        nameToUnicode["quoteleft"] = 0x2018;
        nameToUnicode["quoteright"] = 0x2019;
        nameToUnicode["quotedblleft"] = 0x201C;
        nameToUnicode["quotedblright"] = 0x201D;
        nameToUnicode["bullet"] = 0x2022;
        nameToUnicode["endash"] = 0x2013;
        nameToUnicode["emdash"] = 0x2014;
        nameToUnicode["tilde"] = 0x02DC;
        nameToUnicode["trademark"] = 0x2122;
        nameToUnicode["scaron"] = 0x0161;
        nameToUnicode["guilsinglright"] = 0x203A;
        nameToUnicode["oe"] = 0x0153;
        nameToUnicode["zcaron"] = 0x017E;
        nameToUnicode["Ydieresis"] = 0x0178;
    }
    /^FontName / {
        fontName = $2;
        gsub(/-/, "", fontName);
        print "\n    val " fontName "Widths = mapOf<Char, Int>(";
    }
    /^C / {
        # Replace semicolons with spaces to make standard field splitting easier
        gsub(/;/, " ");

        charNum = -1;
        glyphName = "";
        width = 0;

        # Parse fields: C (Code), WX (Width), N (Name)
        for (i = 1; i <= NF; i++) {
            if ($i == "C") charNum = $(i+1);
            if ($i == "WX") width = $(i+1);
            if ($i == "N") glyphName = $(i+1);
        }

        finalUnicode = -1;

        if (glyphName in nameToUnicode) {
            finalUnicode = nameToUnicode[glyphName];
        } else if (charNum >= 32 && charNum <= 126 || charNum >= 160 && charNum <= 255) {
            finalUnicode = charNum;
        }

        if (finalUnicode != -1) {
            printf "        0x%04X.toChar() to %d,\n", finalUnicode, width;
        }
    }
    END {
        print "    )"
    }
    ' "$afm_file" >> "$OUTPUT_FILE"
done

echo "}" >> "$OUTPUT_FILE"
echo "Generated $OUTPUT_FILE successfully."