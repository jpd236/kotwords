#!/usr/bin/env perl
#
# Script to generate the xNN.kt data files and output the array for Unidecode.kt.
# The files will be generated in the same folder as this script.
#
# To run against a specific locally-extracted version of Text::Unidecode, run:
# $ perl -I/path/to/unidecode generate.pl

use strict;
use warnings;

use Encode;
use File::Basename;
use File::Spec::Functions 'catfile';
use Text::Unidecode;

# Custom mappings to be applied on top of the default set from unidecode.
my %custom_mappings = (
    0x2190 => "<-",  # ←
    0x2191 => "^",   # ↑
    0x2192 => "->",  # →
    0x2193 => "v",   # ↓
    0x2605 => "*",   # ★
    0x266d => "b",   # ♭
    0x266f => "#",   # ♯
);

print "private val data = mapOf(\n";

my $output_dir = dirname(__FILE__);
# Skip 0x00, since we support ISO-8859-1.
for (my $high = 0x01; $high <= 0xFF; $high++) {
    print sprintf("    0x%02x to x%02x,\n", $high, $high);
    open(HANDLE, '>', catfile($output_dir, sprintf("x%02x.kt", $high)));
    print HANDLE "package com.jeffpdavidson.kotwords.formats.unidecode\n\n";
    print HANDLE sprintf("internal val x%02x = arrayOf(", $high);
    for (my $low = 0x00; $low <= 0xFF; $low++) {
        my $code = ($high << 8) + $low;
        my $decoded = exists($custom_mappings{$code}) ? $custom_mappings{$code} : unidecode(chr($code));
        print HANDLE sprintf("\n    // 0x%04x: %s => %s", $code, encode("utf-8", chr($code)), $decoded);
        print HANDLE "\n    \"";
        foreach my $char (split('', $decoded)) {
          print HANDLE sprintf("\\u%04x", ord($char));
        }
        print HANDLE "\"";
        print HANDLE ",";
    }
    print HANDLE "\n)\n";
    close(HANDLE);
}
print ")\n";