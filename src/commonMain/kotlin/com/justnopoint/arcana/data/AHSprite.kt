package com.justnopoint.arcana.data

import com.justnopoint.arcana.RenderMode
import com.justnopoint.arcana.Transformation
import okio.Buffer
import kotlin.experimental.and

class AHSprite(
    data: Buffer
) {
    val flags = data.readByteArray(2)
    val sprNo = data.readShortLe()
    val axisX = data.readShortLe()
    val axisY = data.readShortLe()
    val multiplier = data.readIntLe()
    val unk1 = data.readByteArray(16)
    val angleDegrees = data.readShortLe().toInt()
    val unk2 = data.readByteArray(6)
    val scaleX = Float.fromBits(data.readIntLe())
    val scaleY = Float.fromBits(data.readIntLe())

    val renderMode : RenderMode by lazy {
        if((flags[0] and 0x02).toInt() == 0x02) {
            return@lazy RenderMode.Additive(multiplier)
        }
        if((flags[0] and 0x64).toInt() == 0x64) {
            return@lazy RenderMode.Subtractive
        }
        RenderMode.Normal
    }

    val transformations : List<Transformation> by lazy {
        val all = mutableListOf<Transformation>()
        when((flags[0] and 0x1C).toInt()) {
            0x04 -> {
                all.add(Transformation.Rotation(degrees = 90))
                all.add(Transformation.Flip(horizontal = false, vertical = true))
            }
            0x08 -> {
                all.add(Transformation.Flip(horizontal = false, vertical = true))
            }
            0x0C -> {
                all.add(Transformation.Rotation(degrees = 90))
            }
            0x10 -> {
                all.add(Transformation.Flip(horizontal = true, vertical = false))
            }
            0x14 -> {
                all.add(Transformation.Rotation(degrees = -90))
            }
            0x18 -> {
                all.add(Transformation.Flip(horizontal = true, vertical = true))
            }
            0x1C -> {
                all.add(Transformation.Rotation(degrees = 90))
                all.add(Transformation.Flip(horizontal = true, vertical = false))
            }
        }

        if(angleDegrees != 0) {
            all.add(Transformation.Rotation(degrees = angleDegrees))
        }

        all
    }
}