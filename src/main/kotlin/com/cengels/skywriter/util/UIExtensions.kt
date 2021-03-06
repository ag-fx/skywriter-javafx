package com.cengels.skywriter.util

import com.cengels.skywriter.enum.ImageSizingType
import com.cengels.skywriter.theming.ThemesManager
import javafx.animation.Animation
import javafx.animation.FadeTransition
import javafx.animation.Interpolator
import javafx.animation.Timeline
import javafx.beans.Observable
import javafx.beans.binding.Binding
import javafx.beans.property.Property
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.css.Styleable
import javafx.event.EventTarget
import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.geometry.Insets
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.IndexRange
import javafx.scene.image.Image
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.FileChooser
import javafx.stage.Screen
import javafx.stage.Window
import javafx.util.Duration
import tornadofx.*
import kotlin.math.min
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/** Creates a binding that automatically converts the [java.awt.Color] values into [javafx.scene.paint.Paint] values. */
 fun Color.toBackground(): Background {
     return Background(BackgroundFill(this, CornerRadii.EMPTY, Insets.EMPTY))
 }

fun Color.shift(hueBy: Double = 0.0, saturationBy: Double = 0.0, brightnessBy: Double = 0.0, opacityBy: Double = 0.0): Color {
    return this.deriveColor(
        hueBy,
        1 + saturationBy / this.saturation,
        1 + brightnessBy / this.brightness,
        1 + opacityBy / this.opacity
    )
}

/** Creates a binding that automatically converts the [javafx.scene.paint.Color] values into [javafx.scene.paint.Paint] values. */
fun Property<javafx.scene.paint.Color>.backgroundBinding(): Binding<Background> {
    return this.objectBinding {
        if (it == null) {
            return@objectBinding Background(BackgroundFill(javafx.scene.paint.Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY))
        }
        return@objectBinding Background(BackgroundFill(it, CornerRadii.EMPTY, Insets.EMPTY))
    } as Binding<Background>
}

val imageExtensionFilter: FileChooser.ExtensionFilter by lazy { FileChooser.ExtensionFilter("Images", "*.JPG", "*.BMP", "*.PNG", "*.GIF", "*.JPEG", "*.MPO") }

fun getBackgroundFor(color: Color, image: String? = null, imageSizingType: ImageSizingType = ImageSizingType.CONTAIN): Background {
    if (image.isNullOrBlank()) {
        return Background(BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY))
    }

    return Background(
        arrayOf(BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)),
        arrayOf(BackgroundImage(Image("file:///${image}"),
            if (imageSizingType == ImageSizingType.TILE) BackgroundRepeat.REPEAT else BackgroundRepeat.NO_REPEAT,
            if (imageSizingType == ImageSizingType.TILE) BackgroundRepeat.REPEAT else BackgroundRepeat.NO_REPEAT,
            if (imageSizingType != ImageSizingType.TILE) BackgroundPosition.CENTER else BackgroundPosition.DEFAULT,
            BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false,
                imageSizingType == ImageSizingType.CONTAIN,
                imageSizingType == ImageSizingType.COVER))))
}

/** Shifts the brightness of this color by the specified value. If the brightness is already at its bounds (meaning pure white or pure black), the shift is inverted instead. */
fun Color.shiftBy(value: Double): Color {
    val invert = this.brightness + value <= 0.0 || this.brightness + value >= 1.0
    return deriveColor(0.0, 1.0, 1.0 + (if (invert) -value else value), 1.0)
}

/** Adds a change listener to the selected Property<T> and calls it immediately. */
fun <T> ObservableValue<T>.onChangeAndNow(op: (it: T?) -> Unit) {
    this.onChange(op)
    op(this.value)
}

/**
 * Adds an instance of the specified stylesheet class to the scene and automatically updates it if the selected application theme changes.
 * The added stylesheet must have a public constructor with a first argument of type [Theme].
 **/
fun Scene.addThemedStylesheet(stylesheet: KClass<out Stylesheet>) {
    var currentStylesheet: Stylesheet? = null

    ThemesManager.selectedThemeProperty.onChangeAndNow {
        if (it != null) {
            currentStylesheet?.let { stylesheet -> this.stylesheets.remove(stylesheet.externalForm) }
            currentStylesheet = stylesheet.primaryConstructor?.call(it)?.also { stylesheet ->
                this.stylesheets.add(stylesheet.externalForm)
            } ?: throw IllegalArgumentException("The specified stylesheet does not have an accessible primary constructor.")
        }
    }
}

/** Checks if this element has any children that are currently focused. */
fun Node.isChildFocused(): Boolean {
    return this.scene.focusOwner?.findParent { it === this } != null
}

/** Finds the parent matching the specified predicate. Returns null if no parent matching the predicate is found. */
fun Node.findParent(predicate: (node: Node) -> Boolean): Node? {
    if (parent == null) {
        return null
    }

    if (predicate.invoke(parent)) {
        return parent
    }

    return parent.findParent(predicate)
}

/** Listens to a variable number of observables and executes the given function if any of the observables change as well as executing it once during the initial call. */
fun executeAndListen(vararg dependencies: Observable, op: () -> Unit) {
    listen(true, *dependencies, op = op)
}

/** Listens to a variable number of observables and executes the given function if any of the observables change. */
fun listen(vararg dependencies: Observable, op: () -> Unit) {
    listen(false, *dependencies, op = op)
}

private fun listen(executeNow: Boolean, vararg dependencies: Observable, op: () -> Unit) {
    if (dependencies.isEmpty()) {
        throw IllegalArgumentException("Must specify at least one dependency.")
    }

    dependencies.forEach { it.addListener { op() } }

    if (executeNow) op()
}

/** Animates this property whenever the given observable value changes. */
fun <T> WritableValue<T>.animate(value: ObservableValue<T>, duration: Duration, interpolator: Interpolator? = null, op: Timeline.() -> Unit = {}) {
    value.addListener { observable, oldValue, newValue ->
        this.animate(newValue, duration, interpolator, op)
    }
}

/** Plays the method in reverse, starting from the end and transitioning to the beginning. */
fun Animation.playInReverse() {
    rate = -this.rate
    jumpTo(this.totalDuration)
    play()
}

/** Adds a new [FadeTransition] to this [Node] and automatically plays it whenever the given [ObservableBooleanValue] changes. If the value changes to true, the node is faded in. If the value changes to false, the node is faded out. */
fun Node.fadeWhen(playWhen: ObservableBooleanValue, durationMs: Number = 200): FadeTransition {
    opacity = 0.0
    return FadeTransition(durationMs.millis, this).apply {
        fromValue = 0.0
        toValue = 1.0
        interpolator = Interpolator.EASE_BOTH

        playWhen.addListener { _, _, newValue ->
            if (newValue) {
                playFromStart()
            } else {
                playInReverse()
            }
        }
    }
}

fun <T> DataGrid<T>.bindSelectedItem(property: ObservableValue<T?>) {
    // skin is null during initialization, which causes an exception
    this.skinProperty().onChangeOnce { skin ->
        val value = property.value
        if (skin != null && value != null) {
            this.selectionModel.select(value)
        }
    }

    property.onChange { newValue ->
        if (newValue == null) this.selectionModel.clearSelection() else this.selectionModel.select(newValue)
    }

    if (property is Property<*>) {
        selectionModel.selectedItemProperty().onChange {
            property.value = it
        }
    }
}

/** Sets a clip with the specified background radius to this node that resizes according to the node's own size. */
fun Region.setRadiusClip(radius: Number) {
    clipProperty().bind(heightProperty().objectBinding(widthProperty()) {
        Rectangle(widthProperty().value, heightProperty().value).apply {
            arcWidth = radius.toDouble() * 2
            arcHeight = radius.toDouble() * 2
        }
    })
}

/** Gets the boundaries of the screen the window is placed on. If the window is placed on multiple screens, gets its combined bounding box. */
val Window.screenBounds: Rectangle2D
    get() {
        var minX = 0.0
        var minY = 0.0
        var width = 0.0
        var height = 0.0

        Screen.getScreensForRectangle(this.x, this.y, this.width, this.height).forEach {
            it.visualBounds.apply {
                minX = min(this.minX, minX)
                minY = min(this.minY, minY)
                width = min(this.width, width)
                height = min(this.height, height)
            }
        }

        return Rectangle2D(minX, minY, width, height)
    }

operator fun IndexRange.contains(index: Int): Boolean {
    return this.start <= index && index < this.end
}