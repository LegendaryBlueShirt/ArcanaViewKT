package com.justnopoint.arcana.util

import com.justnopoint.arcana.data.SpriteSheet
import okio.BufferedSource
import okio.FileHandle
import okio.buffer

class HIPFile(dataFile: FileHandle, offset: Long, destination: SpriteSheet? = null, posX: Int = 0, posY: Int = 0) {
    companion object {
        val MAGIC = ByteArray(4) {
            when (it) {
                0 -> 0x48
                1 -> 0x49
                2 -> 0x50
                else -> {
                    0x00
                }
            }
        }.decodeToString()
    }

    var paldata: ByteArray? = null
    var sheet: SpriteSheet
    val texx: Int
    val texy: Int
    val bpp: Int

    init {
        val dataSource = dataFile.source(offset)
        val dataBuffer = dataSource.buffer()
        if(dataBuffer.readByteArray(4).decodeToString() != MAGIC) {
            throw IllegalArgumentException("Did not find HIP header!")
        }
        val endian = dataBuffer.readIntLe()
        if(endian != 293) {
            throw IllegalArgumentException("Image is probably big endian")
        }
        val filesize = dataBuffer.readIntLe()
        //println("HIP filesize - $filesize   Actual filesize - ${dataBuffer.buffer.size}")
        val palsize = dataBuffer.readIntLe()
        texx = dataBuffer.readIntLe()
        texy = dataBuffer.readIntLe()
        val flags = dataBuffer.readIntLe()
        dataBuffer.skip(4)

        when(flags and 0xFF) {
            1 -> {
                bpp = 8
                sheet = destination?: SpriteSheet(texx, texy, 1)
                load256(dataBuffer, palsize, sheet, posX, posY)
            }
            16 -> {
                bpp = 32
                sheet = destination?: SpriteSheet(texx, texy, 4)
                load32(dataBuffer, sheet, posX, posY)
            }
            64 -> {
                bpp = 32
                sheet = destination?: SpriteSheet(texx, texy, 4)
                load16(dataBuffer, sheet, posX, posY)
            }
            else -> throw IllegalArgumentException("Unknown image type with flags ${flags and 0xFF}")
        }
    }

    private fun load256(
        buffer: BufferedSource,
        palsize: Int,
        destination: SpriteSheet,
        posX: Int,
        posY: Int
    ) {
        paldata = buffer.readByteArray((palsize*4).toLong())
        var byte = 0
        var position: Int
        while(!buffer.exhausted()) {
            val data = buffer.readByte()
            val count = buffer.readByte().toUByte().toInt()
            (0 until count).forEach { _ ->
                position = (byte/texx + posY) * destination.width + (byte%texx) + posX
                destination.raster[position] = data
                byte++
            }
        }
    }

    private fun load32(
        buffer: BufferedSource,
        destination: SpriteSheet,
        posX: Int,
        posY: Int) {
        var byte = 0
        var position: Int
        while(!buffer.exhausted()) {
            val data = buffer.readByteArray(4)
            val temp = data[0]
            data[0] = data[2]
            data[2] = temp
            val count = buffer.readByte().toUByte().toInt()
            (0 until count).forEach { i ->
                position = (byte/texx + posY) * (4 * destination.width) + ((byte%texx) + posX) * 4
                data.forEach {
                    destination.raster[position++] = it
                }
                byte++
            }
        }
    }

    private fun load16(
        buffer: BufferedSource,
        destination: SpriteSheet,
        posX: Int,
        posY: Int
    ) {
        var byte = 0
        var position: Int
        while(!buffer.exhausted()) {
            val data = buffer.readShortLe().toUShort().toInt()
            /*var red = (data and 0xF800) ushr 11
            red = (red shl 3) or (red shr 2)
            var green = (data and 0x07E0) ushr 5
            green = (green shl 2) or (green shr 4)
            var blue = data and 0x001F
            blue = (blue shl 3) or (blue shr 2)*/
            var alpha = data and 0xF000 ushr 12
            alpha = alpha or alpha shl 4
            var red = data and 0x0F00 ushr 8
            red = red or red shl 4
            var green = data and 0x00F0 ushr 4
            green = green or green shl 4
            var blue = data and 0x000F
            blue = blue or blue shl 4
            val count = buffer.readByte().toUByte().toInt()
            (0 until count).forEach { _ ->
                position = (byte/texx + posY) * (4 * destination.width) + ((byte%texx) + posX) * 4
                destination.raster[position] = red.toByte()
                destination.raster[position+1] = green.toByte()
                destination.raster[position+2] = blue.toByte()
                destination.raster[position+3] = alpha.toByte()
                byte++
            }
        }
    }

    fun getBPP(): Int {
        return bpp
    }
}