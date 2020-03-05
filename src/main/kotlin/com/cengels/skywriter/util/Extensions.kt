package com.cengels.skywriter.util

import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.max

private val WORD_REGEX = Regex("\\b\\S+\\b")

/** Counts the number of words in the [String]. */
fun String.splitWords(): List<String> {
    return this.split(WORD_REGEX)
}

/** Counts the words by applying the regex `\b\S+\b`. */
fun String.countWords(): Int {
    return this.splitWords().size - 1
}

/** Surrounds the [String] with the specified text. */
fun String.surround(with: String): String = "$with$this$with"

/** Removes the specified substrings from the [String]. */
fun String.remove(vararg strings: String): String {
    return strings.fold(this) { acc, string -> acc.replace(string, "") }
}

/** Removes the specified substrings from the [String] only if they are not escaped (preceded by a backslash). If they are escaped, removes the escaping. */
fun String.removeUnescaped(vararg strings: String): String {
    return strings.fold(this) { acc, string ->
        var index = acc.indexOf(string)
        var newString = acc

        while (index != -1) {
            var endIndex = index + string.length - 1
            if (newString.getOrNull(index - 1)?.equals('\\') != true) {
                // unescaped
                newString = newString.removeRange(index..endIndex)
                endIndex = -1
            } else {
                newString = newString.removeRange(index - 1 until index)
                endIndex -= 1
            }

            index = newString.indexOf(string, endIndex + 1)
        }

        newString
    }
}

/** Splits the string at the specified exclusive positions. */
fun String.split(vararg at: Int): Collection<String> {
    if (at.isEmpty()) {
        return mutableListOf(this)
    }

    val splitString: MutableCollection<String> = mutableListOf(this.slice(0 until at.first()))

    if (at.size > 1) {
        (0 until at.size - 1).mapTo(splitString) { this.slice(at[it]..at[it + 1]) }
    }

    splitString.add(this.slice(at.last() until this.length))

    return splitString
}

/** Finds the word boundaries at the specified index. */
fun String.findWordBoundaries(at: Int): IntRange {
    val splitString = this.split(at)
    val first = splitString.first().indexOfLast { c -> c == ' ' || c == '\n' || c == '\r' }
    val second = splitString.last().indexOfFirst { c -> c == ' ' || c == '\n' || c == '\r' }

    return max(first + 1, 0)..if (second == -1) this.length else second + at
}

/** Checks if the [LocalDateTime] lies between now and now - duration. */
fun LocalDateTime.isWithin(duration: Duration): Boolean {
    return this.isAfter(LocalDateTime.now().minusSeconds(duration.seconds))
}

/** Checks if the iterable contains any of the given elements. */
fun <T> Iterable<T>.containsAny(otherCollection: Iterable<T>): Boolean {
    return this.any { otherCollection.contains(it) }
}

/** Checks if the iterable contains any of the given elements. */
fun <T> Iterable<T>.containsAny(vararg elements: T): Boolean {
    return this.any { elements.contains(it) }
}
