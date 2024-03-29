package com.jeffpdavidson.kotwords.formats.unidecode

/**
 * Provider of ASCII transliterations of Unicode text.
 *
 * This package is based on [Text::Unidecode](https://metacpan.org/pod/Text::Unidecode) for generating ASCII
 * transliterations of Unicode strings, which is licensed under the
 * [Perl Artistic License](https://metacpan.org/pod/perlartistic). The `xNN.kt` data files were generated from
 * Text::Unidecode 1.30 with `generate.pl`. For each unicode character with code `0xWXYZ`, the corresponding string can
 * be found in the `0xYZ` element of the array in `xWX.kt`.
 */
internal object Unidecode {

    fun unidecode(string: String): String {
        if (string.none { it.code >= 256 }) {
            // Can be encoded as an ISO-8859-1 string, so return the string as is.
            return string
        }

        return string.map { ch ->
            if (ch.code < 256) {
                "$ch"
            } else if (ch.code > 0xFFFF) {
                "[?]"
            } else {
                val highBits = ch.code ushr 8
                val lowBits = ch.code and 0xFF
                data[highBits]!![lowBits]
            }
        }.joinToString("")
    }

    private val data = mapOf(
        0x01 to x01,
        0x02 to x02,
        0x03 to x03,
        0x04 to x04,
        0x05 to x05,
        0x06 to x06,
        0x07 to x07,
        0x08 to x08,
        0x09 to x09,
        0x0a to x0a,
        0x0b to x0b,
        0x0c to x0c,
        0x0d to x0d,
        0x0e to x0e,
        0x0f to x0f,
        0x10 to x10,
        0x11 to x11,
        0x12 to x12,
        0x13 to x13,
        0x14 to x14,
        0x15 to x15,
        0x16 to x16,
        0x17 to x17,
        0x18 to x18,
        0x19 to x19,
        0x1a to x1a,
        0x1b to x1b,
        0x1c to x1c,
        0x1d to x1d,
        0x1e to x1e,
        0x1f to x1f,
        0x20 to x20,
        0x21 to x21,
        0x22 to x22,
        0x23 to x23,
        0x24 to x24,
        0x25 to x25,
        0x26 to x26,
        0x27 to x27,
        0x28 to x28,
        0x29 to x29,
        0x2a to x2a,
        0x2b to x2b,
        0x2c to x2c,
        0x2d to x2d,
        0x2e to x2e,
        0x2f to x2f,
        0x30 to x30,
        0x31 to x31,
        0x32 to x32,
        0x33 to x33,
        0x34 to x34,
        0x35 to x35,
        0x36 to x36,
        0x37 to x37,
        0x38 to x38,
        0x39 to x39,
        0x3a to x3a,
        0x3b to x3b,
        0x3c to x3c,
        0x3d to x3d,
        0x3e to x3e,
        0x3f to x3f,
        0x40 to x40,
        0x41 to x41,
        0x42 to x42,
        0x43 to x43,
        0x44 to x44,
        0x45 to x45,
        0x46 to x46,
        0x47 to x47,
        0x48 to x48,
        0x49 to x49,
        0x4a to x4a,
        0x4b to x4b,
        0x4c to x4c,
        0x4d to x4d,
        0x4e to x4e,
        0x4f to x4f,
        0x50 to x50,
        0x51 to x51,
        0x52 to x52,
        0x53 to x53,
        0x54 to x54,
        0x55 to x55,
        0x56 to x56,
        0x57 to x57,
        0x58 to x58,
        0x59 to x59,
        0x5a to x5a,
        0x5b to x5b,
        0x5c to x5c,
        0x5d to x5d,
        0x5e to x5e,
        0x5f to x5f,
        0x60 to x60,
        0x61 to x61,
        0x62 to x62,
        0x63 to x63,
        0x64 to x64,
        0x65 to x65,
        0x66 to x66,
        0x67 to x67,
        0x68 to x68,
        0x69 to x69,
        0x6a to x6a,
        0x6b to x6b,
        0x6c to x6c,
        0x6d to x6d,
        0x6e to x6e,
        0x6f to x6f,
        0x70 to x70,
        0x71 to x71,
        0x72 to x72,
        0x73 to x73,
        0x74 to x74,
        0x75 to x75,
        0x76 to x76,
        0x77 to x77,
        0x78 to x78,
        0x79 to x79,
        0x7a to x7a,
        0x7b to x7b,
        0x7c to x7c,
        0x7d to x7d,
        0x7e to x7e,
        0x7f to x7f,
        0x80 to x80,
        0x81 to x81,
        0x82 to x82,
        0x83 to x83,
        0x84 to x84,
        0x85 to x85,
        0x86 to x86,
        0x87 to x87,
        0x88 to x88,
        0x89 to x89,
        0x8a to x8a,
        0x8b to x8b,
        0x8c to x8c,
        0x8d to x8d,
        0x8e to x8e,
        0x8f to x8f,
        0x90 to x90,
        0x91 to x91,
        0x92 to x92,
        0x93 to x93,
        0x94 to x94,
        0x95 to x95,
        0x96 to x96,
        0x97 to x97,
        0x98 to x98,
        0x99 to x99,
        0x9a to x9a,
        0x9b to x9b,
        0x9c to x9c,
        0x9d to x9d,
        0x9e to x9e,
        0x9f to x9f,
        0xa0 to xa0,
        0xa1 to xa1,
        0xa2 to xa2,
        0xa3 to xa3,
        0xa4 to xa4,
        0xa5 to xa5,
        0xa6 to xa6,
        0xa7 to xa7,
        0xa8 to xa8,
        0xa9 to xa9,
        0xaa to xaa,
        0xab to xab,
        0xac to xac,
        0xad to xad,
        0xae to xae,
        0xaf to xaf,
        0xb0 to xb0,
        0xb1 to xb1,
        0xb2 to xb2,
        0xb3 to xb3,
        0xb4 to xb4,
        0xb5 to xb5,
        0xb6 to xb6,
        0xb7 to xb7,
        0xb8 to xb8,
        0xb9 to xb9,
        0xba to xba,
        0xbb to xbb,
        0xbc to xbc,
        0xbd to xbd,
        0xbe to xbe,
        0xbf to xbf,
        0xc0 to xc0,
        0xc1 to xc1,
        0xc2 to xc2,
        0xc3 to xc3,
        0xc4 to xc4,
        0xc5 to xc5,
        0xc6 to xc6,
        0xc7 to xc7,
        0xc8 to xc8,
        0xc9 to xc9,
        0xca to xca,
        0xcb to xcb,
        0xcc to xcc,
        0xcd to xcd,
        0xce to xce,
        0xcf to xcf,
        0xd0 to xd0,
        0xd1 to xd1,
        0xd2 to xd2,
        0xd3 to xd3,
        0xd4 to xd4,
        0xd5 to xd5,
        0xd6 to xd6,
        0xd7 to xd7,
        0xd8 to xd8,
        0xd9 to xd9,
        0xda to xda,
        0xdb to xdb,
        0xdc to xdc,
        0xdd to xdd,
        0xde to xde,
        0xdf to xdf,
        0xe0 to xe0,
        0xe1 to xe1,
        0xe2 to xe2,
        0xe3 to xe3,
        0xe4 to xe4,
        0xe5 to xe5,
        0xe6 to xe6,
        0xe7 to xe7,
        0xe8 to xe8,
        0xe9 to xe9,
        0xea to xea,
        0xeb to xeb,
        0xec to xec,
        0xed to xed,
        0xee to xee,
        0xef to xef,
        0xf0 to xf0,
        0xf1 to xf1,
        0xf2 to xf2,
        0xf3 to xf3,
        0xf4 to xf4,
        0xf5 to xf5,
        0xf6 to xf6,
        0xf7 to xf7,
        0xf8 to xf8,
        0xf9 to xf9,
        0xfa to xfa,
        0xfb to xfb,
        0xfc to xfc,
        0xfd to xfd,
        0xfe to xfe,
        0xff to xff,
    )
}