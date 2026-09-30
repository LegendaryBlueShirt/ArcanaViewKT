package com.justnopoint.arcana.util

import okio.*
import okio.Path.Companion.toPath

private const val COPY_BUF_SIZE = 0x1000

object UnPk3 {
    fun decompress(input: Source, output: Sink) {
        input.buffer().use { inputBuffer ->
            output.buffer().use { outputBuffer ->
                val copyBuffer = IntArray(COPY_BUF_SIZE)
                var position = 0

                do {
                    try {
                        var control = inputBuffer.readShortLe().toUShort().toInt()
                        for (n in 0 until 16) {
                            val code: Int = inputBuffer.readShortLe().toUShort().toInt()

                            if ((control and 0x8000) == 0) {
                                outputBuffer.writeShortLe(code)
                                copyBuffer[(position++)%COPY_BUF_SIZE] = code
                            } else {
                                val loop = (code ushr 12) + 2
                                var offset = position - ((code and 0xfff) + 1)

                                for(m in 0 until loop) {
                                    val value = copyBuffer[offset++%COPY_BUF_SIZE]
                                    outputBuffer.writeShortLe(value)
                                    copyBuffer[(position++)%COPY_BUF_SIZE] = value
                                }
                            }
                            control = control shl 1
                        }
                    } catch (e: EOFException) {
                        break
                    }
                } while (true)
            }
        }
    }
}

fun pk3util() {
    println("UnPk3Util")
    val path = "C:\\Users\\Francisco\\Downloads\\acheartf_filesystem\\files\\ACT\\00\\ACT_00.PK3".toPath()
    val outBuffer = Buffer()
    FileSystem.SYSTEM.openReadOnly(path).use { handle ->
        handle.source().buffer().use {
            UnPk3.decompress(it, outBuffer)
        }
    }
    FileSystem.SYSTEM.openReadWrite("output.bin".toPath()).use { handle ->
        handle.sink().write(outBuffer, outBuffer.size)
        handle.flush()
    }
}
