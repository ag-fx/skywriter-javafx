package com.cengels.skywriter.util

import com.cengels.skywriter.enum.ImageSizingType
import com.cengels.skywriter.util.convert.ColorConverter
import javafx.beans.binding.Binding
import javafx.beans.property.Property
import javafx.geometry.Insets
import javafx.geometry.Side
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout.*
import javafx.scene.paint.Paint
import javafx.stage.FileChooser
import tornadofx.*
import java.awt.Color

/** Creates a binding that automatically converts the [java.awt.Color] values into [javafx.scene.paint.Paint] values. */
 fun Color.toBackground(): Background {
     return Background(BackgroundFill(ColorConverter.convert(this), CornerRadii.EMPTY, Insets.EMPTY))
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

fun getBackgroundFor(color: javafx.scene.paint.Color, image: String? = null, imageSizingType: ImageSizingType = ImageSizingType.CONTAIN): Background {
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

fun getBackgroundFor(color: Color, image: String? = null, imageSizingType: ImageSizingType = ImageSizingType.CONTAIN): Background {
    return getBackgroundFor(ColorConverter.convert(color), image, imageSizingType)
}

/** Adds a change listener to the selected Property<T> and calls it immediately. */
fun <T> Property<T>.onChangeAndNow(op: (it: T?) -> Unit) {
    this.onChange(op)
    op(this.value)
}