package com.justnopoint.arcana.util

import okio.*
import okio.Path.Companion.toPath
import kotlin.time.Clock

class PacFileSystem(private val path: Path): FileSystem() {
    companion object {
        const val MAGIC = "FPAC"
        const val DELIM = "/"
        const val SUFFIX = ".pac"
    }

    private val files = mutableListOf<PacFile>()
    private var data: ByteArray

    init {
        var currentInstant = Clock.System.now()
        SYSTEM.openReadOnly(path).use { handle ->
            val dataBuffer = if (checkEncrypted(handle.source())) {
                Mersenne.decrypt(path.name.uppercase(), handle.source())
            } else {
                Buffer().also {
                    handle.source().buffer().readAll(it)
                }
            }
            var instant = Clock.System.now()
            Logger.logVerbose("Decrypted in ${(instant - currentInstant).inWholeMilliseconds} ms")
            currentInstant = instant
            data = dataBuffer.peek().readByteArray(dataBuffer.size)
            instant = Clock.System.now()
            Logger.logVerbose("Read in ${(instant - currentInstant).inWholeMilliseconds} ms")
            currentInstant = instant
            files.addAll(readFileList(dataBuffer.peek(), DELIM, 0L, dataBuffer.size))
            instant = Clock.System.now()
            Logger.logVerbose("Parsed in ${(instant - currentInstant).inWholeMilliseconds} ms")
            currentInstant = instant
            var archive = files.firstOrNull { it.name.endsWith(SUFFIX) }
            while (archive != null) {
                val base = archive.name.substring(0, archive.name.length - SUFFIX.length)
                val contents = readFileList(dataBuffer.peek().apply { skip(archive.offset) }, base+DELIM, archive.offset, archive.size)
                files.remove(archive)
                if(contents.isNotEmpty()) {
                    files.addAll(contents)
                } else {
                    println("Add empty folder $base")
                    files.add(PacFile(base, -1, -2, -1))
                }
                archive = files.firstOrNull { it.name.endsWith(SUFFIX) }
            }
            instant = Clock.System.now()
            Logger.logVerbose("Recursive archive parsed in ${(instant - currentInstant).inWholeMilliseconds} ms")
            currentInstant = instant
        }
    }

    private fun checkEncrypted(source: Source): Boolean {
        source.buffer().use {
            if (it.readByteArray(4).decodeToString() != MAGIC) {
                return true
            }
        }
        return false
    }

    private fun readFileList(source: Source,
                             pathPrefix: String,
                             fOffset: Long = 0L,
                             fSize: Long): List<PacFile> {
        source.buffer().use { buffer ->
            val header = buffer.readByteArray(4).decodeToString()
            if (header != MAGIC) {
                throw IllegalArgumentException("Unexpected header! $header")
            }
            val startOffset = buffer.readIntLe().toLong()
            val size = buffer.readIntLe().toLong()
            if (size != fSize) {
                throw IllegalArgumentException("Unexpected size value, possibly wrong endian?")
            }
            val nFiles = buffer.readIntLe()
            val unk = buffer.readIntLe()
            val nSize = buffer.readIntLe().toLong()
            buffer.skip(8)
            val fillerSize = 16 - ((nSize + 12) % 16)

            return (0 until nFiles).map {
                val name = pathPrefix + buffer.readByteArray(nSize).decodeToString().trim{it <= ' '}
                val filenum = buffer.readIntLe()
                val offset = buffer.readIntLe() + startOffset + fOffset
                val fileSize = buffer.readIntLe().toLong()
                buffer.skip(fillerSize)
                PacFile(name = name, filenum = filenum, offset = offset, size = fileSize)
            }
        }
    }

    data class PacFile(val name: String, val filenum: Int, val offset: Long, val size: Long)

    override fun appendingSink(file: Path, mustExist: Boolean): Sink {
        TODO("Not yet implemented")
    }

    override fun atomicMove(source: Path, target: Path) {
        TODO("Not yet implemented")
    }

    override fun canonicalize(path: Path): Path {
        TODO("Not yet implemented")
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean) {
        TODO("Not yet implemented")
    }

    override fun createSymlink(source: Path, target: Path) {
        TODO("Not yet implemented")
    }

    override fun delete(path: Path, mustExist: Boolean) {
        TODO("Not yet implemented")
    }

    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle {
        TODO("Not yet implemented")
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
        TODO("Not yet implemented")
    }

    override fun list(dir: Path): List<Path> {
        Logger.logVerbose("Get list for $dir")
        val segments = dir.segments
        val list = files.map { it.name }.asSequence().filter { it.startsWith(dir.toString()) }
            .map { it.toPath() }
            //.map { it.split(DELIM) }
            .filter { it.segments.size > segments.size }
            .map { it.segments[segments.size] }
            .distinct()
            .map { dir.div(it) }
            .toList()
        Logger.logVerbose(list.joinToString())
        return list
    }

    override fun listOrNull(dir: Path): List<Path>? {
        return list(dir).ifEmpty { null }
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        val fileMatch = files.firstOrNull { it.name == path.toString() }
        return if(fileMatch != null) {
            when (fileMatch.offset) {
                -2L -> {
                    FileMetadata(
                        isDirectory = true
                    )
                }
                -1L -> {
                    FileMetadata(
                        isRegularFile = false,
                        isDirectory = false
                    )
                }
                else -> {
                    FileMetadata(
                        isRegularFile = true,
                        size = fileMatch.size
                    )
                }
            }
        } else if(list(path).isNotEmpty()) {
            FileMetadata(
                isDirectory = true
            )
        } else {
            null
        }
    }

    override fun openReadOnly(file: Path): FileHandle {
        val mem = files.firstOrNull { it.name == file.toString() } ?: throw FileNotFoundException(file.toString())
        return object: FileHandle(false) {
            override fun protectedClose() {
            }

            override fun protectedFlush() {
            }

            override fun protectedRead(fileOffset: Long, array: ByteArray, arrayOffset: Int, byteCount: Int): Int {
                if(fileOffset == mem.size)
                    return -1
                var bytesToRead = byteCount
                if((mem.size - fileOffset) < byteCount) {
                    bytesToRead = (mem.size - fileOffset).toInt()
                }

                data.copyInto(
                    destination = array,
                    destinationOffset = arrayOffset,
                    startIndex = (mem.offset + fileOffset).toInt(),
                    endIndex = (mem.offset + fileOffset + bytesToRead).toInt()
                )
                return bytesToRead
            }

            override fun protectedResize(size: Long) {
                TODO("Not yet implemented")
            }

            override fun protectedSize() = mem.size

            override fun protectedWrite(fileOffset: Long, array: ByteArray, arrayOffset: Int, byteCount: Int) {
                TODO("Not yet implemented")
            }

        }
    }

    override fun source(file: Path): Source {
        val mem = files.firstOrNull { it.name == file.toString() }!!
        val outBuffer = Buffer()
        outBuffer.write(data, mem.offset.toInt(), mem.size.toInt())
        outBuffer.flush()
        //data.copyTo(outBuffer, mem.offset, mem.size)
        return outBuffer.peek()
    }
}