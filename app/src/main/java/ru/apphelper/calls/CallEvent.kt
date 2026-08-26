package ru.apphelper.calls

data class CallEvent(
    val phoneNumber: String,
    val displayName: String? = null,
    val incoming: Boolean = true,
    val timestampMillis: Long = System.currentTimeMillis(),
)

object CallEventStore {
    private val listeners = mutableSetOf<(CallEvent) -> Unit>()

    fun publish(event: CallEvent) {
        listeners.toList().forEach { it(event) }
    }

    fun addListener(listener: (CallEvent) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (CallEvent) -> Unit) {
        listeners -= listener
    }
}
