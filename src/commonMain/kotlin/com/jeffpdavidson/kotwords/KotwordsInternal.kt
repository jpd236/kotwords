package com.jeffpdavidson.kotwords

@RequiresOptIn(message = "Only intended for use within Kotwords. Do not use externally.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
internal annotation class KotwordsInternal