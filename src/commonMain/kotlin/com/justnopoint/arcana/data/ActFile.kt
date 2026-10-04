package com.justnopoint.arcana.data

import com.justnopoint.arcana.util.UnPk3
import okio.Buffer
import okio.Source
import okio.use

private const val NANIMS = 2816L
private const val ANIMSIZE = 256L

class ActFile(
    source: Source
) {
    private var animDefs: List<List<Int>> = emptyList()
    private var frameDefs: List<AHFrame> = emptyList()
    private var boxDefs: List<AHBox> = emptyList()
    private var sprDefs: List<AHSprite> = emptyList()

    init {
        Buffer().use { dataBuffer ->
            UnPk3.decompress(source, dataBuffer)
            val animDefDataRaw = (0 until NANIMS).map {
                val buffer = Buffer()
                dataBuffer.read(buffer, ANIMSIZE)
                buffer
            }
            val lengths = dataBuffer.readByteArray(NANIMS)
            val nFrames = dataBuffer.readIntLe()
            val nBoxes = dataBuffer.readIntLe()
            val nSprites = dataBuffer.readIntLe()

            frameDefs = (0 until nFrames).map {
                AHFrame(dataBuffer)
            }
            boxDefs = (0 until nBoxes).map {
                AHBox(dataBuffer)
            }
            sprDefs = (0 until nSprites).map {
                AHSprite(dataBuffer)
            }
            animDefs = animDefDataRaw.mapIndexed { index, data ->
                data.use {
                    (0 until lengths[index].toUByte().toInt()).map {
                        data.readShortLe().toUShort().toInt()
                    }
                }
            }
        }
    }

    fun getValidAnims(): List<Int> {
        return animDefs.indices.filter { animDefs[it].isNotEmpty() }
    }

    fun getAnimDef(index: Int): List<Int> {
        return animDefs[index]
    }

    fun getFrameDef(index: Int): AHFrame {
        return frameDefs[index-1]
    }

    fun getSprDef(index: Int): AHSprite {
        return sprDefs[index-1]
    }

    fun getBoxDef(index: Int): AHBox {
        return boxDefs[index-1]
    }

    fun getSprCount(): Int {
        return sprDefs.size
    }

    fun getSequenceDurationTotal(anim: List<Int>): Int {
        return anim.map { getFrameDef(it) }.sumOf { it.getDuration() }
    }

    fun getTimeForFrame(anim: List<Int>, currentFrame: Int): Int {
        return (0 until currentFrame)
            .map { getFrameDef(anim[it]) }
            .sumOf { it.getDuration() }
    }

    fun getFrameForTime(anim: List<Int>, time: Int): Int {
        val duration = getSequenceDurationTotal(anim)
        if (duration == 0) return -1
        var currentTime = time%duration
        var framenum = -1
        anim.forEach { frameIndex ->
            val frame = getFrameDef(frameIndex)
            framenum++
            currentTime -= frame.getDuration()
            if(currentTime < 0)
                return framenum
        }
        return framenum
    }
}
