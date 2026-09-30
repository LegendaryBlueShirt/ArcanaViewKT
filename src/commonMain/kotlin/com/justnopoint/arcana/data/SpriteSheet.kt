package com.justnopoint.arcana.data

class SpriteSheet(
    val width: Int,
    val height: Int,
    val bytesPerPixel: Int = 1
    ) {
    val raster = ByteArray(width * height * bytesPerPixel)
}