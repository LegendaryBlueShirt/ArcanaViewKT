package com.justnopoint.arcana.data

import okio.Buffer

class AHBox(
    data: Buffer
) {
    val data: ByteArray = data.readByteArray(6)
    val x = data.readShortLe().toInt()
    val y = data.readShortLe().toInt()
    val w = data.readShortLe().toInt()
    val h = data.readShortLe().toInt()

    fun getBoxType(): BoxType {
        return when(data[1].toUByte().toInt()) {
            0 -> BoxType.CLSN
            1, 2 -> BoxType.HURT
            6 -> BoxType.REFLECT
            8 -> BoxType.HIT
            4 -> BoxType.CLASH
            else -> BoxType.OTHER
        }
    }

    fun getBoxColor(): Long {
        return when(getBoxType()) {
            BoxType.HIT -> 0xC00000FF
            BoxType.HURT -> 0xC0FF0000
            BoxType.CLASH -> 0xC000FFFF
            BoxType.CLSN -> 0xC0FFFFFF
            BoxType.THROW -> 0xC0FF00B7
            BoxType.REFLECT -> 0xC0FFFF00
            BoxType.OTHER -> 0xC000FF00
        }
    }

    fun getExtraInformation(): String {
        return when((getBoxType())) {
            BoxType.HIT -> "${getDamage().toString()} Damage"
            BoxType.REFLECT -> "Reflect ${getReflectAngle().toString()}"
            else -> ""
        }
    }

    fun getDamage(): Int? {
        if (getBoxType() != BoxType.HIT) {
            return null
        }
        return data[2].toUByte().toInt()*100
    }

    fun getReflectAngle(): String? {
        if (getBoxType() != BoxType.REFLECT) {
            return null
        }
        return when(data[3].toUByte().toInt()) {
            1 -> "Down, Forward"
            2 -> "Down"
            3 -> "Down, Behind"
            4 -> "Behind"
            5 -> "Up, Behind"
            6 -> "Up"
            7 -> "Up, Forward"
            else -> "Forward"
        }
    }

    enum class BoxType {
        HIT,
        HURT,
        CLASH,
        CLSN,
        THROW,
        REFLECT,
        OTHER
    }
}