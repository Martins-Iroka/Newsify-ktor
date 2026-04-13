package com.martdev.repository.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType


class CITextColumnType : TextColumnType() {
    override fun sqlType(): String = "CITEXT"
}

fun Table.citext(name: String) = registerColumn(name, CITextColumnType())