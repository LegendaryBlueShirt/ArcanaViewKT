package com.justnopoint.arcana.util

import com.justnopoint.arcana.data.SpriteSheet
import okio.BufferedSource

const val fourcc_dxt_1 = "DXT1"
const val fourcc_dxt_3 = "DXT3"
const val fourcc_dxt_5 = "DXT5"

enum class DXT {
    DXT1, DXT3, DXT5
}

var blue: Int = 0
var green: Int = 0
var red: Int = 0

fun unpack565(packed: BufferedSource, color: ByteArray, cOffset: Int): Int {
    val value = packed.readShortLe().toUShort().toInt()

    blue = (value shr 11) and 0x1f
    green = (value shr 5) and 0x3f
    red = value and 0x1f

    color[cOffset] = ((red shl 3) or (red shr 2)).toUByte().toByte()
    color[cOffset + 1] = ((green shl 2) or (green shr 4)).toUByte().toByte()
    color[cOffset + 2] = ((blue shl 3) or (blue shr 2)).toUByte().toByte()
    color[cOffset + 3] = -1

    return value
}

val codes = ByteArray(16)

fun decompressAlphaDxt3(rgba: ByteArray, alphaData: ByteArray) {
    alphaData.map { it.toUByte() }.forEachIndexed { i, quant ->
        val lo = quant and 0xfu
        val hi = quant and 0xf0u

        rgba[8*i + 3] = (lo or ((lo * 0x10u).toUByte())).toByte()
        rgba[8*i + 7] = (hi or (hi * 0x10u).toUByte()).toByte()
    }
}

fun decompressAlphaDxt5(rgba: ByteArray, alphaData: ByteArray) {
    codes[0] = alphaData[0]
    codes[1] = alphaData[1]
    if(codes[0] <= codes[1]) {
        for(i in 1 until 5) {
            codes[i+1] = (((5 - i) * codes[0] + i * codes[1]) / 5).toUByte().toByte()
        }
        codes[6] = 0
        codes[7] = -1
    } else {
        for(i in 1 until 7) {
            codes[i+1] = (((7 - i) * codes[0] + i * codes[1]) / 7).toUByte().toByte()
        }
    }

    var src = 2
    var dest = 0
    for(i in 0 until 2) {
        var value = 0
        for(j in 0 until 3) {
            val b = alphaData[src++].toUByte().toInt()
            value = value or (b shl (8*j))
        }

        for(j in 0 until 8) {
            val index = (value shr (3*j)) and 0x7
            rgba[4*dest + 3] = codes[index]
            dest++
        }
    }
}

fun decompressColor(rgba: ByteArray, block: BufferedSource, isDxt1: Boolean) {
    val a = unpack565(block, codes, 0)
    val b = unpack565(block, codes, 4)

    if(isDxt1) {
        (0 until 3).forEach { i ->
            val c = codes[i].toUByte()
            val d = codes[i + 4].toUByte()

            if(a <= b) {
                codes[i + 8] = ((c + d) / 2u).toByte()
                codes[i + 12] = 0
            } else {
                codes[i + 8] = ((2u * c + d) / 3u).toByte()
                codes[i + 12] = ((c + 2u * d) / 3u).toByte()
            }
        }
    } else {
        (0 until 3).forEach { i ->
            val c = codes[i].toUByte()
            val d = codes[i + 4].toUByte()

            codes[i + 8] = ((2u * c + d) / 3u).toByte()
            codes[i + 12] = ((c + 2u * d) / 3u).toByte()
        }
    }


    codes[11] = -1
    codes[15] = if(isDxt1 && (a <= b)) 0 else -1

    var pos = 0
    do {
        val packed = block.readByte().toUByte().toInt()


        writeColor(rgba, packed and 0x3, pos++)
        writeColor(rgba, (packed shr 2) and 0x3, pos++)
        writeColor(rgba, (packed shr 4) and 0x3, pos++)
        writeColor(rgba, (packed shr 6) and 0x3, pos++)
    } while (pos < 16)
}

private fun writeColor(rgba: ByteArray, index: Int, position: Int) {
    codes.copyInto(
        destination = rgba,
        destinationOffset = position * 4,
        startIndex = 4 * index,
        endIndex = 4 * index + 4
    )
    //rgba[position * 4 + 0] = codes[4 * index + 0]
    //rgba[position * 4 + 1] = codes[4 * index + 1]
    //rgba[position * 4 + 2] = codes[4 * index + 2]
    //rgba[position * 4 + 3] = codes[4 * index + 3]
}

data class DDSHeader(
    val magic: String,
    val size: Int,
    val flags: Int,
    val height: Int,
    val width: Int,
    val pitchOrLinearSize: Int,
    val depth: Int,
    val mipMapCount: Int,
    val reserved: ByteArray,
    val pixelformat: PixelFormat,
    val caps: Int,
    val caps2: Int,
    val caps3: Int,
    val caps4: Int,
    val reserved2: ByteArray
)

data class PixelFormat(
    val size: Int,
    val flags: Int,
    val fourCC: String,
    val rgbBitCount: Int,
    val rBitMask: Int,
    val gBitMask: Int,
    val bBitMask: Int,
    val aBitMask: Int
)

fun readHeader(source: BufferedSource): DDSHeader {
    return DDSHeader(
        source.readByteString(4).utf8(),
        source.readIntLe(),
        source.readIntLe(),
        source.readIntLe(),
        source.readIntLe(),
        source.readIntLe(),
        source.readIntLe(),
        source.readIntLe(),
        source.readByteArray(44),
        readPixelFormat(source),
        source.readIntLe(),
        source.readIntLe(),
        source.readIntLe(),
        source.readIntLe(),
        source.readByteArray(4)
    )
}

fun readPixelFormat(source: BufferedSource): PixelFormat {
    return PixelFormat(
        source.readIntLe(),
        source.readIntLe(),
        source.readByteString(4).utf8(),
        source.readIntLe(),
        source.readIntLe(),
        source.readIntLe(),
        source.readIntLe(),
        source.readIntLe()
    )
}

val targetRgba = ByteArray(64)
fun decompressBlock(source: BufferedSource, method: DXT): ByteArray {
    when(method) {
        DXT.DXT1 -> {
            decompressColor(targetRgba, source, true)
        }
        DXT.DXT3 -> {
            val alphaData = source.readByteArray(8)
            decompressColor(targetRgba, source, false)
            decompressAlphaDxt3(targetRgba, alphaData)
        }
        DXT.DXT5 -> {
            val alphaData = source.readByteArray(8)
            decompressColor(targetRgba, source, false)
            decompressAlphaDxt5(targetRgba, alphaData)
        }
    }

    return targetRgba
}

fun DecompressDDS(source: BufferedSource): SpriteSheet {
    val header = readHeader(source)
    if(header.magic != "DDS ") {
        error("Bad DDS Magic Value! ${header.magic}")
    }
    val method = when(header.pixelformat.fourCC) {
        fourcc_dxt_1 -> DXT.DXT1
        fourcc_dxt_3 -> DXT.DXT3
        fourcc_dxt_5 -> DXT.DXT5
        else -> error("Unsupported DDS type ${header.pixelformat.fourCC}")
    }

    val sheet = SpriteSheet(header.width, header.height, 4)

    for(y in 0 until header.height step 4) {
        for(x in 0 until header.width step 4) {
            decompressBlock(source, method)

            var sourcePixel = 0
            var sx: Int
            var sy: Int
            for(n in 0 until 16) {
                sx = x + n%4
                sy = y + n/4
                if(sx < header.width && sy < header.height) {
                    for(i in 0 until 4) {
                        sheet.raster[(header.width * sy + sx)*4 + i] = targetRgba[sourcePixel++]
                    }
                } else {
                    sourcePixel += 4
                }
            }
        }
    }

    return sheet
}