package com.justnopoint.arcana

import com.justnopoint.arcana.data.AHBox

data class TextureInstance(
    val texture: Int
)

enum class Keys {
    UP, DOWN, LEFT, RIGHT, SPACEBAR
}

sealed class Transformation {
    data class Rotation(
        val degrees: Int
    ) : Transformation()

    data class Flip(
        val horizontal: Boolean,
        val vertical: Boolean
    ) : Transformation()
}

sealed class RenderMode {
    data object Normal : RenderMode()
    data class Additive(
        val multiplier: Int
    ) : RenderMode()
    data object Subtractive : RenderMode()
}

interface EngineContext {
    fun isExiting(): Boolean
    fun setKeyCallback(callback: (Keys) -> Unit)
    fun folderSelected(): String?
    fun characterSelected(): Int?
    fun animationSelected(): Int?
    fun boxtypeSelected(): AHBox.BoxType?
    fun currentFrame(): Int
    fun loadRgbaImage(raster: ByteArray, width: Int, height: Int): TextureInstance
    fun loadIndexedImage(raster: ByteArray, palette: UByteArray, width: Int, height: Int): TextureInstance
    fun clearTexture(handle: TextureInstance)
    fun processInput()
    fun <T> performRender(renderFunction: RenderContext.() -> T)
}

interface RenderContext {
    //fun showImage(textureHandle: TextureInstance, posX: Int, posY: Int, scale: Int = 2)
    //fun showImage(textureHandle: TextureInstance, posX: Int, posY: Int, width: Int, height: Int)
    fun showImage(
        textureHandle: TextureInstance,
        posX: Int,
        posY: Int,
        srcX: Int,
        srcY: Int,
        width: Int,
        height: Int,
        transformations: List<Transformation> = emptyList(),
        renderMode: RenderMode)
    fun drawBox(color: Long, left: Int, top: Int, width: Int, height: Int)
    fun showText(text: String, posX: Int, posY: Int)
}

expect inline fun <T> engineContext(title: String, contextFunction: EngineContext.() -> T)