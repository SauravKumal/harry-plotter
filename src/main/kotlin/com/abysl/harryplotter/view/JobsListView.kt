package com.abysl.harryplotter.view

import com.abysl.harryplotter.config.Config
import com.abysl.harryplotter.model.PlotJob
import com.abysl.harryplotter.util.invoke
import com.abysl.harryplotter.viewmodel.JobsListViewModel
import com.abysl.harryplotter.windows.SimpleDialogs.showConfirmation
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.ContextMenu
import javafx.scene.control.ListView
import javafx.scene.control.MenuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class JobsListView {

    @FXML
    lateinit var jobsView: ListView<PlotJob>

    lateinit var viewModel: JobsListViewModel

    var stateRefreshScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    fun initialized(jobsListViewModel: JobsListViewModel) {
        this.viewModel = jobsListViewModel.also { it.refreshCallback = jobsView::refresh }

        jobsListViewModel.plotJobs.onEach { jobList ->
            stateRefreshScope.cancel()
            stateRefreshScope = CoroutineScope(Dispatchers.IO)
            jobList.onEach {
                it.stateFlow.onEach { jobsView.refresh() }.launchIn(stateRefreshScope)
                it.statsFlow.onEach { jobsView.refresh() }.launchIn(stateRefreshScope)
            }
            Platform.runLater {
                jobsView.items.setAll(jobList)
                jobsView.selectionModel.select(viewModel.selectedPlotJob())
            }
        }.launchIn(CoroutineScope(Dispatchers.IO))
        jobsListViewModel.selectedPlotJob.onEach {
            if (it != null) {
                jobsView.selectionModel.select(it)
            } else {
                jobsView.selectionModel.clearSelection()
            }
        }.launchIn(CoroutineScope(Dispatchers.IO))
        jobsView.selectionModel.selectedItemProperty().addListener { obs, old, new ->
            viewModel.selectedPlotJob.value = new
        }
        jobsView.contextMenu = jobsMenu
    }

    fun onStartAll() {
        viewModel.onStartAll()
    }

    fun onStopAll() {
        if (showConfirmation("Stop Processes", "Are you sure you want to stop all plots?")) {
            viewModel.onStopAll()
        }
    }

    fun onClear() {
        viewModel.onClear()
    }

    val jobsMenu = ContextMenu()

    val duplicate = MenuItem("Duplicate").also {
        it.setOnAction {
            val job = viewModel.selectedPlotJob() ?: return@setOnAction
            viewModel.plotJobs.value += PlotJob(job.description)
            Config.savePlotJobs(viewModel.plotJobs.value)
        }
        jobsMenu.items.add(it)
    }

    val delete = MenuItem("Delete").also {
        it.setOnAction {
            val job = viewModel.selectedPlotJob() ?: return@setOnAction
            if (showConfirmation("Delete Job?", "Are you sure you want to delete ${job.description}")) {
                viewModel.plotJobs.value -= job
                Config.savePlotJobs(viewModel.plotJobs.value)
            }
        }
        jobsMenu.items.add(it)
    }
}
