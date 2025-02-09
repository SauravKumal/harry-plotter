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

package com.abysl.harryplotter.viewmodel

import com.abysl.harryplotter.config.Config
import com.abysl.harryplotter.config.Prefs
import com.abysl.harryplotter.model.PlotJob
import com.abysl.harryplotter.model.records.JobDescription
import com.abysl.harryplotter.util.IOUtil
import com.abysl.harryplotter.util.invoke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class JobsListViewModel {
    val plotJobs: MutableStateFlow<List<PlotJob>> = MutableStateFlow(listOf())
    val selectedPlotJob: MutableStateFlow<PlotJob?> = MutableStateFlow(null)
    var staggerScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    var runStagger: Boolean = false

    lateinit var refreshCallback: () -> Unit

    fun onStartAll(delay: Long = 1000) {
        cancelStagger()
        runStagger = true
        var first = true
        plotJobs.value.forEach { it.tempDone = 0 }
        staggerScope.launch {
            while (runStagger) {
                var staticTimer = if (first) Prefs.staticStagger * MILLIS_PER_MINUTE else 0L
                while (staticTimer < Prefs.staticStagger * MILLIS_PER_MINUTE || checkPhaseBlocked()) {
                    delay(delay)
                    staticTimer += delay
                }
                plotJobs.value.firstOrNull {
                    it.isReady() && !it.manageSelf &&
                        (it.tempDone < it.description.plotsToFinish || it.description.plotsToFinish == 0)
                }?.start()
                first = false
            }
        }
    }

    fun onStopAll() {
        cancelStagger()
        plotJobs.value.forEach(PlotJob::stop)
    }

    fun onClear() {
        val plotIds = plotJobs.value.filter { it.state.plotId.isNotBlank() }.map { it.state.plotId }
        val dirs = plotJobs.value.map { it.description.tempDir }
        dirs.forEach { dir ->
            val files = dir.listFiles() ?: return@forEach
            files.filter { file ->
                file.extension == "tmp" &&
                    plotIds.none {
                        file.name.contains(it)
                    }
            }.forEach(IOUtil::deleteFile)
        }
    }

    fun saveJob(description: JobDescription) {
        val selectedJob = selectedPlotJob()
        if (selectedJob == null) {
            val newJob = PlotJob(description)
            plotJobs.value += PlotJob(description)
            selectedPlotJob.value = newJob
        } else {
            selectedJob.description = description
        }
        Config.savePlotJobs(plotJobs())
        refreshCallback()
    }

    fun cancelStagger() {
        staggerScope.cancel()
        staggerScope = CoroutineScope(Dispatchers.IO)
        runStagger = false
    }

    fun checkPhaseBlocked(): Boolean {
        return checkPhaseOneBlocked() || checkOtherBlocked()
    }

    fun checkPhaseOneBlocked(): Boolean {
        val phaseOneStagger = Prefs.firstStagger
        if (phaseOneStagger == 0) return false
        return plotJobs.value.filter { it.state.phase == 1 && it.state.running }.size >= phaseOneStagger
    }

    fun checkOtherBlocked(): Boolean {
        val otherStagger = Prefs.otherStagger
        if (otherStagger == 0) return false
        return plotJobs.value.filter { it.state.phase != 1 && it.state.running }.size >= otherStagger
    }

    fun clearSelected() {
        selectedPlotJob.value = null
    }

    companion object {
        private const val MILLIS_PER_MINUTE = 60000L
    }
}
