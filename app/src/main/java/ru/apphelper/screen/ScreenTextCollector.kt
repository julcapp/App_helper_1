package ru.apphelper.screen

data class ScreenTextPart(val text: String, val visible: Boolean = true, val password: Boolean = false)
data class ScreenTextResult(val text: String, val truncated: Boolean)

/** Bounded local processing; input is a lazy traversal, never a retained screen snapshot. */
object ScreenTextCollector {
    const val MAX_CHARS = 12000
    const val MAX_NODES = 1000

    fun collect(parts: Sequence<ScreenTextPart>): ScreenTextResult {
        val output = StringBuilder()
        var previous = ""
        var count = 0
        for (part in parts) {
            if (++count > MAX_NODES) return ScreenTextResult(output.toString(), true)
            if (part.password || !part.visible) continue
            val text = part.text.trim()
            if (text.isBlank() || text == previous) continue
            previous = text
            val value = (if (output.isEmpty()) "" else "\n") + text
            val remaining = MAX_CHARS - output.length
            output.append(value.take(remaining))
            if (value.length > remaining) return ScreenTextResult(output.toString(), true)
        }
        return ScreenTextResult(output.toString(), false)
    }
}
