package com.justnopoint.arcana.util

import okio.BufferedSink
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

object Logger {
    private var verbose = false
    var internalLog = emptyList<String>()
    private lateinit var logFileHandle: BufferedSink

    fun init(verbose: Boolean = false) {
        logFileHandle = FileSystem.SYSTEM.appendingSink("log.txt".toPath()).buffer()
        this.verbose = verbose
    }

    fun clear() {
        internalLog = emptyList()
    }

    fun logVerbose(message: String) {
        if(verbose) {
            log(message)
        }
    }

    fun log(message: String) {
        internalLog += message
        println(message)
        logFileHandle.writeUtf8(message)
        logFileHandle.writeUtf8("\n")
        logFileHandle.flush()
    }

    fun close() {
        logFileHandle.close()
    }
}