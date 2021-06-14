package com.jeffpdavidson.kotwords.js

internal enum class MeasurementUnit(val jsValue: String) {
    POINTS("pt")
}

internal enum class Format(val jsValue: String) {
    LETTER("letter")
}

internal fun newJsPdfOptions(
    unit: MeasurementUnit = MeasurementUnit.POINTS,
    format: Format = Format.LETTER,
): JsPdfOptions {
    val options = js("{}").unsafeCast<JsPdfOptions>()
    options.unit = unit.jsValue
    options.format = format.jsValue
    return options
}