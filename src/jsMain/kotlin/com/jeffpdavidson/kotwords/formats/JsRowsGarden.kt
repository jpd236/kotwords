package com.jeffpdavidson.kotwords.formats

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

object JsRowsGarden {
    @JsName("parseRowsGarden")
    fun parseRowsGarden(rgz: ByteArray): Promise<RowsGarden> = GlobalScope.promise { RowsGarden.parse(rgz) }
}