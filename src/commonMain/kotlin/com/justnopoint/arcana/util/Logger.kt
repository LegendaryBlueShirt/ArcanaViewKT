package com.justnopoint.arcana.util

import okio.BufferedSink
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

object Logger {
    private var verbose = false
    private val _internalLog = mutableListOf<String>()
    val internalLog: List<String>
        get() = _internalLog.toList()
    private lateinit var logFileHandle: BufferedSink

    fun init(verbose: Boolean = false) {
        logFileHandle = FileSystem.SYSTEM.appendingSink("log.txt".toPath()).buffer()
        this.verbose = verbose
    }

    fun clear() {
        _internalLog.clear()
    }

    fun logVerbose(message: String) {
        if(verbose) {
            log(message)
        }
    }

    fun log(message: String) {
        _internalLog.add(message)
        logFileHandle.writeUtf8(message)
        logFileHandle.writeUtf8("\n")
        logFileHandle.flush()
    }

    fun close() {
        logFileHandle.close()
    }
}