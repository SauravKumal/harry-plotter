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

package com.abysl.harryplotter.controller

import com.abysl.harryplotter.chia.ChiaCli
import com.abysl.harryplotter.chia.ChiaLocator
import com.abysl.harryplotter.config.Config
import com.abysl.harryplotter.config.Prefs
import com.abysl.harryplotter.data.JobProcess
import com.abysl.harryplotter.model.DataModel
import com.abysl.harryplotter.model.DataModel.chia
import com.abysl.harryplotter.model.DataModel.jobs
import com.abysl.harryplotter.model.DataModel.keys
import com.abysl.harryplotter.model.DataModel.selectedKey
import com.abysl.harryplotter.windows.VersionPromptWindow
import javafx.application.HostServices
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.layout.VBox
import javafx.stage.Stage

class MainController{
    // UI Components ---------------------------------------------------------------------------------------------------
    @FXML
    private lateinit var mainBox: VBox

    @FXML
    private lateinit var jobsListController: JobsListController

    @FXML
    private lateinit var jobEditorController: JobEditorController

    @FXML
    private lateinit var jobStatusViewController: JobStatusController

    lateinit var hostServices: HostServices
    lateinit var toggleTheme: () -> Unit

    // Calls after the JavaFX vars are populated so they aren't null
    fun initialized() {
        val chiaLocator = ChiaLocator(mainBox)
        val exePath = chiaLocator.getExePath()
        Prefs.exePath = exePath.path
        chia = ChiaCli(exePath, chiaLocator.getConfigFile())
        jobsListController.initialized()
        jobEditorController.initialized()
        jobStatusViewController.initialized()
        keys += chia.readKeys()
        jobs += Config.getPlotJobs().map { JobProcess(chia, it) }
        selectedKey = keys.first()
    }

    fun onAbout() {
        VersionPromptWindow.show()
    }

    fun onBugReport() {
        hostServices.showDocument("https://github.com/abueide/harry-plotter/issues/new")
    }

    fun onToggleTheme() {
        toggleTheme()
    }

    fun onExit() {
        if (jobs.any { it.state.running }) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Let plot jobs finish?"
            alert.headerText = "Let plot jobs finish?"
            alert.contentText = "Would you like to let plot jobs finish or close them?"
            (alert.dialogPane.lookupButton(ButtonType.OK) as Button).text = "Let them finish"
            (alert.dialogPane.lookupButton(ButtonType.CANCEL) as Button).text = "Close them"
            val answer = alert.showAndWait()
            if (answer.get() != ButtonType.OK) {
                jobs.forEach {
                    it.stop(true)
                }
            }
        }
        (mainBox.scene.window as Stage).close()
    }
}
