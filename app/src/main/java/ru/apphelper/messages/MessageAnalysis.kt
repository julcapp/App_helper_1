package ru.apphelper.messages

data class MessageAnalysis(
    val summary: String,
    val plainExplanation: String,
    val explicitRequest: String?,
    val uncertainty: AnalysisUncertainty,
)

enum class AnalysisUncertainty {
    LOW,
    MEDIUM,
    HIGH,
}

interface MessageAnalysisProvider {
    fun analyze(
        originalText: String,
        onSuccess: (MessageAnalysis) -> Unit,
        onError: (String) -> Unit,
    )
}
