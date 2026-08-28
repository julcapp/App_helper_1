package ru.apphelper.messages

import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONObject

class HttpMessageAnalysisProvider(
    private val baseUrl: String,
) : MessageAnalysisProvider {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun analyze(
        originalText: String,
        onSuccess: (MessageAnalysis) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (baseUrl.isBlank()) {
            onError("AI-анализ пока не настроен на этом устройстве.")
            return
        }
        if (originalText.isBlank()) {
            onError("В сообщении нет доступного текста для анализа.")
            return
        }

        executor.execute {
            try {
                val connection = (URL(baseUrl.trimEnd('/') + "/v1/message-analysis").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 30_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }

                val request = JSONObject().put("message", originalText).toString()
                connection.outputStream.use { it.write(request.toByteArray(Charsets.UTF_8)) }

                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

                if (status !in 200..299) {
                    throw IllegalStateException("AI gateway returned HTTP $status")
                }

                val json = JSONObject(body)
                val uncertainty = when (json.optString("uncertainty").lowercase()) {
                    "low" -> AnalysisUncertainty.LOW
                    "high" -> AnalysisUncertainty.HIGH
                    else -> AnalysisUncertainty.MEDIUM
                }
                val explicitRequest = if (json.isNull("explicitRequest")) null else json.optString("explicitRequest").takeIf { it.isNotBlank() }
                val analysis = MessageAnalysis(
                    summary = json.optString("summary"),
                    plainExplanation = json.optString("plainExplanation"),
                    explicitRequest = explicitRequest,
                    uncertainty = uncertainty,
                )
                mainHandler.post { onSuccess(analysis) }
            } catch (_: Exception) {
                mainHandler.post { onError("Не удалось выполнить AI-анализ сообщения. Попробуйте ещё раз позже.") }
            }
        }
    }
}
