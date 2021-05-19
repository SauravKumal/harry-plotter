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

import com.abysl.harryplotter.model.PlotJob
import com.abysl.harryplotter.util.invoke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class JobStatusViewModel {
    var logsScope = CoroutineScope(Dispatchers.IO)
    var shownLogs: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    var shownJob: MutableStateFlow<PlotJob?> = MutableStateFlow(null)

    private var lastLogsSize = 0

    fun loadJob(job: PlotJob) {
        clearJob()
        shownJob.value = job
        logsScope = CoroutineScope(Dispatchers.IO)
        job.state.logs
            .onEach { logsList ->
                lastLogsSize = shownLogs().size
                shownLogs.value = logsList
            }
            .launchIn(logsScope)
    }

    fun clearJob() {
        logsScope.cancel()
        shownJob.value = null
        shownLogs.value = listOf()
    }

    fun shouldAppend(): Boolean {
        return shownLogs().size == lastLogsSize + 1
    }
}
