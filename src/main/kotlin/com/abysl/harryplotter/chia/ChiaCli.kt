/*
 *     Copyright (c) 2021 Andrew Bueide
 *
 *     This file is part of Harry Plotter.
 *
 *     Harry Plotter is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Harry Plotter is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Harry Plotter.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.abysl.harryplotter.chia

import com.abysl.harryplotter.config.Prefs
import com.abysl.harryplotter.model.records.ChiaKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import java.time.LocalDateTime

class ChiaCli(val exe: File = File(Prefs.exePath), val config: File = File(Prefs.configPath)) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.JavaFx

    val chiaHome = config.parentFile.parentFile

    fun readKeys(): List<ChiaKey> {
        val keyInput = runCommand("keys", "show")
        val keys = mutableListOf<ChiaKey>()
        for (line in keyInput) {
            if (line.contains("Fingerprint")) {
                keys.add(ChiaKey())
            }
            if (keys.isNotEmpty()) {
                keys[keys.size - 1] = keys[keys.size - 1].parseLine(line)
            }
        }
        return keys
    }

    /**
     * Usage: runCommand("keys", "show") -> chia.exe keys show
     * @param args
     * @return
     */

    fun runCommand(vararg commandArgs: String, timeout: Long = 100): List<String> {
        val command: List<String> = listOf(exe.path) + commandArgs.toList()
        val proc: Process = ProcessBuilder(command)
            .start()
        proc.waitFor(timeout, TimeUnit.SECONDS)
        val input: InputStream = proc.inputStream
        val err: InputStream = proc.errorStream
        return input.reader().readLines()
    }

    fun runCommandAsync(
        ioDelay: Long = 10,
        outputCallback: (String) -> Unit,
        completedCallback: (Double) -> Unit,
        vararg commandArgs: String,
    ): Process {
        val command: List<String> = listOf(exe.path) + commandArgs.toList()
        val plotterLogDir = File(System.getProperty("user.home") + "/.harryplotter/plotLogs")
        val logFile = File(plotterLogDir.path + "/" + LocalDateTime.now() + ".log")
        if (!plotterLogDir.exists()) { 
            plotterLogDir.mkdirs()
        }
        val proc: Process = ProcessBuilder(command)
            .start()
        val input = BufferedReader(InputStreamReader(proc.inputStream))
        val err = BufferedReader(InputStreamReader(proc.errorStream))
        CoroutineScope(Dispatchers.IO).launch {
            while (proc.isAlive) {
                input.lines().forEach {
                    logFile.writeText(it)
                    outputCallback(it)
                }
                err.lines().forEach {
                    logFile.writeText(it)
                    outputCallback(it)
                }
                delay(ioDelay)
            }
            input.close()
            err.close()
            val time = proc.info()
                ?.startInstant()
                ?.map { Duration.between(it, Instant.now()) }
                ?.orElse(null)
                ?.seconds?.toDouble() ?: 0.0

            completedCallback(time)
        }
        return proc
    }
}
