package com.example.myapp

import android.content.Context
import android.os.Environment
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

@ReactModule(name = LeodgeLoggerModule.NAME)
class LeodgeLoggerModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "LeodgeLoggerModule"
        private const val LOG_FILE_NAME = "leodge.log"
    }

    private fun getLogFile(): File {
        // Use public Downloads folder so user can access it
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadsDir, LOG_FILE_NAME)
    }

    @ReactMethod
    fun appendToLog(message: String, promise: Promise) {
        try {
            val logFile = getLogFile()
            
            // Append to file
            FileWriter(logFile, true).use { writer ->
                writer.append(message)
                writer.flush()
            }
            
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("LOG_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun getLogFilePath(promise: Promise) {
        try {
            val logFile = getLogFile()
            promise.resolve(logFile.absolutePath)
        } catch (e: Exception) {
            promise.reject("LOG_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun readLog(promise: Promise) {
        try {
            val logFile = getLogFile()
            if (logFile.exists()) {
                val content = logFile.readText()
                promise.resolve(content)
            } else {
                promise.resolve("")
            }
        } catch (e: Exception) {
            promise.reject("LOG_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun clearLog(promise: Promise) {
        try {
            val logFile = getLogFile()
            if (logFile.exists()) {
                logFile.writeText("")
            }
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("LOG_ERROR", e.message, e)
        }
    }

    override fun getName(): String = NAME
}
