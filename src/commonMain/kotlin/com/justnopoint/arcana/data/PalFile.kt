package com.justnopoint.arcana.data

import okio.Source
import okio.buffer

class PalFile(
    source: Source
) {
    val data = source.buffer().apply { skip(32) }.readByteArray(1024).toUByteArray()
}