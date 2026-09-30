package com.justnopoint.arcana.data

import okio.Buffer
import kotlin.experimental.and

class AHFrame(
    data: Buffer
) {
    val activeSprites: List<Int> = (0 until 8).map { data.readShortLe().toInt() }
    val activeBoxes: List<Int> = (0 until 8).map { data.readShortLe().toInt() }
    val frameData = data.readByteArray(32)
    val frameData2 = data.readByteArray(28)

    fun getDuration(): Int {
        return frameData[10].toUByte().toInt() or (frameData[11].toUByte().toInt() shl 8)
    }

    fun getProjectileCoords(): String {
        val x = frameData[30].toUByte().toInt() or (frameData[31].toUByte().toInt() shl 8)
        val y = frameData2[0].toUByte().toInt() or (frameData[1].toUByte().toInt() shl 8)
        return "$x, $y"
    }

    fun getDamage(): Int {
        return frameData[12].toUByte().toInt() * 100
    }

    val throwBox: List<Int> by lazy {
        val x = frameData2.getShort(2)
        val y = frameData2.getShort(4)
        val w = frameData2.getShort(6)
        val h = frameData2.getShort(8)
        listOf(x,y,w,h)
    }

    val knownFlags: List<String> by lazy {
        val flags = mutableListOf<String>()
        if((frameData[1] and 0x01).toInt() == 0x01) {
            flags.add("Recovering")
        }
        if((frameData[1] and 0x08).toInt() == 0x08) {
            flags.add("Overhead Attack")
        }
        if((frameData[1] and 0x10).toInt() == 0x10) {
            flags.add("Low Attack")
        }
        if((frameData[1] and 0x20).toInt() == 0x20) {
            flags.add("Air Unblockable")
        }
        if((frameData[1].toInt() and 0x80) == 0x80) {
            flags.add("Special Cancel")
        }
        if((frameData[2] and 0x10).toInt() == 0x10) {
            flags.add("Counterhit")
        }
        if((frameData[3] and 0x01).toInt() == 0x01) {
            flags.add("Reset Hit")
        }
        if((frameData[3] and 0x02).toInt() == 0x02) {
            flags.add("Unclashable")
        }
        if((frameData[3] and 0x40).toInt() == 0x40) {
            flags.add("Grab")
        }
        if((frameData[3].toInt() and 0x80) == 0x80) {
            flags.add("No Burst")
        }
        if((frameData[16].toInt() and 0x02) == 0x02) {
            flags.add("Endurance")
        }
        flags
    }

    private fun ByteArray.getShort(index: Int): Int {
        return ((this[index].toUByte().toInt() or (this[index+1].toUByte().toInt() shl 8)) shl 16) shr 16
    }
}