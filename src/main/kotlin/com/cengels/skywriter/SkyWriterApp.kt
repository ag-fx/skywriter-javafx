package com.cengels.skywriter

import com.cengels.skywriter.persistence.AppConfig
import com.cengels.skywriter.style.*
import com.cengels.skywriter.theming.ThemesManager
import com.cengels.skywriter.writer.WriterView
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.stage.Stage
import javafx.stage.WindowEvent
import sun.awt.OSInfo
import tornadofx.*
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.filechooser.FileSystemView

class SkyWriterApp : App(WriterView::class, GeneralStylesheet::class) {
    companion object {
        private val isWindows = OSInfo.getOSType() == OSInfo.OSType.WINDOWS
        /** A [Path] corresponding to the user's home directory. On Windows, this will generally be `%USER%/AppData/Roaming`. On all other operating systems, the property `user.home` is used. */
        val homeDirectory: Path = Paths.get(if (isWindows) System.getenv("APPDATA") else System.getProperty("user.home"))
        /** Skywriter's application directory within the user's home directory. This is where configuration files and historical data will be stored. */
        val applicationDirectory: Path = homeDirectory.resolve(if (isWindows) "Skywriter" else "skywriter")

        @JvmStatic
        fun main(args: Array<String>) {
            launch<SkyWriterApp>(args)
        }
    }

    init {
        FX.layoutDebuggerShortcut = KeyCodeCombination(KeyCode.J, KeyCombination.ALT_DOWN, KeyCombination.CONTROL_DOWN)
    }

    override fun start(stage: Stage) {
        stage.minWidth = 300.0
        stage.minHeight = 200.0

        applicationDirectory.toFile().apply {
            if (!this.exists()) {
                if (!this.mkdir()) {
                    throw IOException("Could not create user directory.")
                }
            }
        }

        AppConfig.restoreStage(stage)

        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST) {
            AppConfig.storeStage(stage)
        }

        ThemesManager.load()
        ThemesManager.selectedTheme = ThemesManager.themes.find { it.name == AppConfig.activeTheme } ?: ThemesManager.DEFAULT

        super.start(stage)

        stage.icons.add(Image(this::class.java.getResourceAsStream("air.png")))
    }
}