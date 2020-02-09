package com.cengels.skywriter

import com.cengels.skywriter.style.FormattingStylesheet
import com.cengels.skywriter.style.GeneralStylesheet
import com.cengels.skywriter.style.WriterStylesheet
import com.cengels.skywriter.writer.WriterView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.stage.Stage
import javafx.stage.WindowEvent
import tornadofx.*

class SkyWriterApp : App(WriterView::class, WriterStylesheet::class, FormattingStylesheet::class) {
    init {
        reloadStylesheetsOnFocus()
        FX.layoutDebuggerShortcut = KeyCodeCombination(KeyCode.J, KeyCombination.ALT_DOWN, KeyCombination.CONTROL_DOWN)
    }

    override fun start(stage: Stage) {
        stage.minWidth = 300.0
        stage.minHeight = 200.0

        restoreFromConfig(stage)

        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST) {
            config.set("windowMaximized" to stage.isMaximized)
            if (!stage.isMaximized) {
                config.set("windowHeight" to stage.height)
                config.set("windowWidth" to stage.width)
                config.set("windowX" to stage.x)
                config.set("windowY" to stage.y)
            }
            config.save()
        }

        super.start(stage)
    }

    /** Attempts to restore the window size and position from the config. */
    fun restoreFromConfig(stage: Stage) {
        stage.isMaximized = config.boolean("windowMaximized", false)
        stage.height = config.double("windowHeight", stage.height)
        stage.width = config.double("windowWidth", stage.width)
        stage.x = config.double("windowX", stage.x)
        stage.y = config.double("windowY", stage.y)
    }
}