package com.justnopoint.arcana.data

import okio.Source
import okio.buffer
import okio.use

class TblFile(
    source: Source
) {
    private val _sheetData = mutableListOf<Entry>()
    val sheetData: List<Entry>
        get() = _sheetData

    init {
        source.buffer().use { data ->
            while(data.readIntLe() != -1) {
                val entry = Entry(
                    sheet = data.readShortLe().toInt(),
                    axisX = data.readShortLe().toInt(),
                    axisY = data.readShortLe().toInt(),
                    width = data.readShortLe().toInt(),
                    height = data.readShortLe().toInt(),
                    colorGroup = data.readShortLe().toInt(),
                )
                _sheetData.add(entry)
            }
        }
    }

    data class Entry(
        val sheet: Int,
        val axisX: Int,
        val axisY: Int,
        val width: Int,
        val height: Int,
        val colorGroup: Int,
    )

    fun getSheetIndices(): List<Int> {
        return _sheetData.distinctBy { it.sheet }.map { it.sheet }
    }

    fun getEntry(index: Int): Entry? {
        return _sheetData.getOrNull(index-1)
    }

    fun getEntryCount(): Int {
        return _sheetData.size
    }
}