/*
 * Copyright (c) 2019 Zen Liu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.jeffpdavidson.kotwords.formats

/**
 * Implementation of lz-string (https://github.com/pieroxy/lz-string) in Kotlin.
 *
 * Derived from https://github.com/ZenLiuCN/lz-string4k, which is licensed under the MIT License.
 *
 */
internal object LzString {
    fun decompress(compressed: String): String =
        if (compressed.isBlank()) "" else decompress(compressed.length, 32768) { compressed[it] }

    private fun decompress(length: Int, resetValue: Int, getNextValue: (idx: Int) -> Char): String {
        data class Data(
            var value: Char = '0',
            var position: Int = 0,
            var index: Int = 1
        )

        val builder = StringBuilder()
        val dictionary = mutableListOf(0.toChar().toString(), 1.toChar().toString(), 2.toChar().toString())
        var bits = 0
        var maxPower: Int
        var power: Int
        val data = Data(getNextValue(0), resetValue, 1)
        var resb: Int
        var c = ""
        var w: String
        var entry: String
        var numBits = 3
        var enlargeIn = 4
        var dictSize = 4
        var next: Int

        fun doPower(initBits: Int, initPower: Int, initMaxPowerFactor: Int, mode: Int = 0) {
            bits = initBits
            maxPower = 1 shl initMaxPowerFactor
            power = initPower
            while (power != maxPower) {
                resb = data.value.code and data.position
                data.position = data.position shr 1
                if (data.position == 0) {
                    data.position = resetValue
                    data.value = getNextValue(data.index++)
                }
                bits = bits or (if (resb > 0) 1 else 0) * power
                power = power shl 1
            }
            when (mode) {
                0 -> Unit
                1 -> c = bits.toChar().toString()
                2 -> {
                    dictionary.add(dictSize++, bits.toChar().toString())
                    next = (dictSize - 1)
                    enlargeIn--
                }
            }
        }

        fun checkEnlargeIn() {
            if (enlargeIn == 0) {
                enlargeIn = 1 shl numBits
                numBits++
            }
        }

        doPower(bits, 1, 2)
        next = bits
        when (next) {
            0 -> doPower(0, 1, 8, 1)
            1 -> doPower(0, 1, 16, 1)
            2 -> return ""
        }
        dictionary.add(3, c)
        w = c
        builder.append(w)
        while (true) {
            if (data.index > length) {
                return ""
            }
            doPower(0, 1, numBits)
            next = bits
            when (next) {
                0 -> doPower(0, 1, 8, 2)
                1 -> doPower(0, 1, 16, 2)
                2 -> return builder.toString()
            }
            checkEnlargeIn()
            entry = when {
                dictionary.size > next -> dictionary[next]
                next == dictSize -> w + w[0]
                else -> return ""
            }
            builder.append(entry)
            dictionary.add(dictSize++, w + entry[0])
            enlargeIn--
            w = entry
            checkEnlargeIn()
        }
    }
}