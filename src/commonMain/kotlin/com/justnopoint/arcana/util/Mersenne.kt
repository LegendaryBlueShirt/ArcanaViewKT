package com.justnopoint.arcana.util

import okio.*

class Mersenne(filename: String) {
//    var prev = 0x43415046u
//
//    fun encrypt(input: Int): Int {
//        val dec = input.toUInt()
//        val rand = genRandom()
//        val enc = dec xor (prev xor rand)
//        prev = dec
//        return enc.toInt()
//    }

    companion object {
        private const val SZ = 624

        fun decrypt(filename: String, encrypted: Source): ByteArray {
            val mersenne = Mersenne(filename)
            val output = Buffer()

            var outVal = 0x43415046u
            val input = encrypted.buffer()
            var enc: UInt
            var rand: UInt
            while(!input.exhausted()) {
                enc = input.readUIntLe()
                rand = mersenne.genRandom()
                outVal = outVal xor (enc xor rand)
                output.writeUIntLe(outVal)
            }
            input.close()

            return output.readByteArray()
        }

        fun getEncrypter(filename: String): Mersenne {
            return Mersenne(filename)
        }

//        fun encrypt(filename: String, input: BufferedSource): Buffer {
//            val mersenne = Mersenne(filename)
//            val output = Buffer()
//
//            var prev = 0x43415046u
//            while (!input.exhausted()) {
//                val dec = input.readIntLe().toUInt()
//                val rand = mersenne.genRandom()
//                val enc = dec xor (prev xor rand)
//                output.writeIntLe(enc.toInt())
//                prev = dec
//            }
//
//            return output
//        }
    }

    val values = UIntArray(SZ)
    var index = 0
    var prev: UInt = 0u

    init {
        values[0] = generateSeed(filename)

        (1 until SZ).forEach {
            prev = values[it-1]
            values[it] = (0x6c078965u * (prev xor (prev shr 30)) + it.toUInt())
        }
        index = SZ
    }

    private fun generateSeed(filename: String): UInt {
        var seed = 0u
        filename.forEach {
            seed *= 137u
            seed += it.code.toUInt()
        }
        return seed
    }

    private fun twist() {
        (0 until SZ).forEach {
            val value = (values[it] and 0x80000000u) or (values[(it+1)%SZ] and 0x7FFFFFFFu)
            values[it] = values[(it+397)%SZ] xor (value shr 1)
            if((value and 1u) != 0u) {
                values[it] = values[it] xor 0x9908b0dfu
            }
        }
        index = 0
    }

    fun genRandom(): UInt {
        if(index >= SZ) {
            twist()
        }

        var value = values[index++]
        value = value xor (value shr 11)
        value = value xor ((value shl 7) and 0x9d2c5680u)
        value = value xor ((value shl 15) and 0xefc60000u)
        value = value xor (value shr 18)

        return value
    }
}