package com.justnopoint.arcana

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import libpng.*
import platform.posix.fclose
import platform.posix.fflush
import platform.posix.fopen

actual fun writeImageToFile(raster: ByteArray, paldata: ByteArray?, width: Int, height: Int, outFile: String) {
    val fp = fopen(outFile, "wb+")
    val png_ptr = png_create_write_struct(PNG_LIBPNG_VER_STRING, null, null, null)
    if (png_ptr == null) {
        println("Unable to create the png (png_create_write_struct failure)")
        return
    }
    val info_ptr = png_create_info_struct(png_ptr)
    if (info_ptr == null) {
        println("Unable to create the png (png_create_info_struct failure")
        return
    }
    png_init_io(png_ptr, fp)
    val bit_depth = 8
    val (bytepixels, color_type) =
        if(paldata == null) {
            4 to PNG_COLOR_TYPE_RGBA
        } else {
            1 to PNG_COLOR_TYPE_PALETTE
        }

    png_set_IHDR(
        png_ptr, info_ptr, width.toUInt(), height.toUInt(),
        bit_depth, color_type, PNG_INTERLACE_NONE,
        PNG_COMPRESSION_TYPE_BASE, PNG_FILTER_TYPE_BASE
    )

    if(color_type == PNG_COLOR_TYPE_PALETTE) {
        val palBuffer = ByteArray(768)
        if(paldata?.size == 1024) {
            (0 until 256).forEach { index ->
                palBuffer[index * 3] = paldata[index * 4 + 2]
                palBuffer[index * 3 + 1] = paldata[index * 4 + 1]
                palBuffer[index * 3 + 2] = paldata[index * 4 + 0]
            }
        } else {
            paldata?.copyInto(palBuffer, 0, 0, 768)
        }
        val palette = palBuffer.asUByteArray().pin().addressOf(0) as png_colorp
        png_set_PLTE(png_ptr, info_ptr, palette, 256)
    }

    png_write_info(png_ptr, info_ptr)

    val rowBuffer = ByteArray(bytepixels * width)
    for (y in 0 until height) {
        raster.copyInto(rowBuffer, 0, y * bytepixels * width, (y + 1) * bytepixels * width)
        png_write_row(png_ptr, rowBuffer.asUByteArray().pin().addressOf(0))
    }
    png_write_end(png_ptr, null)
    fflush(fp)
    fclose(fp)
}