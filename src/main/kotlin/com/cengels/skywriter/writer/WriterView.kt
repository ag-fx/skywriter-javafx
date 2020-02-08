package com.cengels.skywriter.writer

import com.cengels.skywriter.enum.Heading
import com.cengels.skywriter.persistence.MarkdownParser
import com.sun.org.apache.xml.internal.serialize.LineSeparator
import javafx.scene.control.ButtonType
import javafx.stage.FileChooser
import javafx.stage.WindowEvent
import tornadofx.*
import java.io.File


class WriterView: View("Skywriter") {
    val model = WriterViewModel()
    val textArea = WriterTextArea().also {
        it.insertText(0, "This is a thing. This is another thing.")
        it.plainTextChanges().subscribe { change ->
            model.dirty = true
        }

        it.isWrapText = true

        contextmenu {
            item("Cut").action { it.cut() }
            item("Copy").action { it.copy() }
            item("Paste").action { it.paste() }
            item("Delete").action { it.deleteText(it.selection) }
        }

        shortcut("Shift+Enter") {
            it.insertText(it.caretPosition, LineSeparator.Windows)
        }
    }

    init {
        this.updateTitle()
        model.fileProperty.onChange { this.updateTitle() }
        model.dirtyProperty.onChange { this.updateTitle() }
    }

    override fun onDock() {
        currentWindow!!.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST) {
            warnOnUnsavedChanges { it.consume() }
        }
    }

    override val root = vbox {
        setPrefSize(800.0, 600.0)
        borderpane {
            top {
                menubar {
                    menu("File") {
                        item("New", "Ctrl+N").action {
                            warnOnUnsavedChanges { return@action }

                            textArea.replaceText("")
                            model.file = null
                            model.dirty = false
                        }
                        item("Open...", "Ctrl+O").action {
                            openLoadDialog()
                        }
                        separator()
                        item("Save", "Ctrl+S") {
                            enableWhen(model.dirtyProperty)
                            action { save() }
                        }
                        item("Save As...", "Ctrl+Shift+S").action {
                            openSaveDialog()
                        }
                        item("Rename...", "Ctrl+R") {
                            enableWhen(model.fileExistsProperty)
                            action { rename() }
                        }
                        separator()
                        item("Preferences...", "Ctrl+P")
                        item("Quit", "Ctrl+Alt+F4").action {
                            close()
                        }
                    }

                    menu("Edit") {
                        item("Undo", "Ctrl+Z").action { textArea.undo() }
                        item("Redo", "Ctrl+Y").action { textArea.redo() }
                        separator()
                        item("Cut", "Ctrl+X").action { textArea.cut() }
                        item("Copy", "Ctrl+C").action { textArea.copy() }
                        item("Paste", "Ctrl+V").action { textArea.paste() }
                        item("Paste Unformatted", "Ctrl+Shift+V")
                        separator()
                        item("Select Word", "Ctrl+W").action { textArea.selectWord() }
                        item("Select Sentence")
                        item("Select Paragraph", "Ctrl+Shift+W").action { textArea.selectParagraph() }
                        item("Select All", "Ctrl+A").action { textArea.selectAll() }
                    }

                    menu("Formatting") {
                        item("Bold", "Ctrl+B").action { textArea.updateSelection("bold") }
                        item("Italic", "Ctrl+I").action { textArea.updateSelection("italic") }
                        separator()
                        item("No Heading").action { textArea.setHeading(null) }
                        item("Heading 1").action { textArea.setHeading(Heading.H1) }
                        item("Heading 2").action { textArea.setHeading(Heading.H2) }
                        item("Heading 3").action { textArea.setHeading(Heading.H3) }
                        item("Heading 4").action { textArea.setHeading(Heading.H4) }
                        item("Heading 5").action { textArea.setHeading(Heading.H5) }
                        item("Heading 6").action { textArea.setHeading(Heading.H6) }
                    }
                }
            }

            center {
                this += textArea
            }

            bottom {
            }
        }
    }

    private fun updateTitle() {
        this.title = if (model.file != null) "Skywriter • ${model.file!!.name}" else "Skywriter • Untitled"

        if (model.dirty) {
            this.title += " *"
        }
    }

    private fun save() {
        if (model.file == null) {
            return openSaveDialog()
        }

        MarkdownParser(textArea.document).save(model.file!!)
        model.dirty = false
    }

    private fun openSaveDialog() {
        val initialDir = if (model.file != null) model.file!!.parent else System.getProperty("user.dir")

        chooseFile(
            "Save As...",
            arrayOf(FileChooser.ExtensionFilter("Markdown", "*.md")),
            File(initialDir),
            FileChooserMode.Save).apply {
            if (this.isNotEmpty()) {
                model.file = this.single()
                MarkdownParser(textArea.document).save(this.single())
                model.dirty = false
            }
        }
    }

    private fun openLoadDialog() {
        val initialDir = if (model.file != null) model.file!!.parent else System.getProperty("user.dir")

        chooseFile(
            "Open...",
            arrayOf(FileChooser.ExtensionFilter("Markdown", "*.md")),
            File(initialDir),
            FileChooserMode.Single).apply {
            if (this.isNotEmpty()) {
                warnOnUnsavedChanges { return@apply }

                model.file = this.single()
                MarkdownParser(textArea.document).load(this.single(), textArea.segOps).also {
                    textArea.replace(it)
                    model.dirty = false
                }
            }
        }
    }

    private fun rename() {
        chooseFile(
            "Rename...",
            arrayOf(FileChooser.ExtensionFilter("Markdown", "*.md")),
            File(model.file!!.parent),
            FileChooserMode.Save).apply {
            if (this.isNotEmpty()) {
                if (model.dirty) {
                    save()
                }

                val newFile: File = this.single()
                if (newFile.exists()) {
                    newFile.delete()
                }

                model.file!!.renameTo(newFile)
                updateTitle()
            }
        }
    }

    /** Warns the user of unsaved changes and prompts them to save. */
    private inline fun warnOnUnsavedChanges(onCancel: () -> Unit) {
        if (model.dirty) {
            warning(
                "Warning",
                "You have unsaved changes. Would you like to save them?",
                ButtonType.YES,
                ButtonType.NO,
                ButtonType.CANCEL
            ) {
                when (this.result) {
                    ButtonType.YES -> save()
                    ButtonType.CANCEL -> onCancel()
                }
            }
        }
    }
}