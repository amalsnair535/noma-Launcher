package com.freelauncher.app.data.service

import android.content.Context
import com.freelauncher.app.ui.util.ScribbleStroke
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionContext
import com.google.mlkit.vision.digitalink.RecognitionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * On-device ML Kit Digital Ink Recognition service.
 * Performs asynchronous handwriting recognition off the UI thread,
 * supporting multi-stroke handwriting and language models.
 */
class ScribbleRecognizer(
    private val context: Context,
    languageTag: String = "en-US"
) {

    private var recognizer: DigitalInkRecognizer? = null
    private var model: DigitalInkRecognitionModel? = null
    @Volatile private var isDownloadingModel = false
    @Volatile private var isModelReady = false

    init {
        initializeModel(languageTag)
    }

    private fun initializeModel(languageTag: String) {
        try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
                ?: DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
            
            if (modelIdentifier != null) {
                val recognitionModel = DigitalInkRecognitionModel.builder(modelIdentifier).build()
                model = recognitionModel
                recognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(recognitionModel).build()
                )
                checkOrDownloadModel()
            }
        } catch (e: Exception) {
            android.util.Log.e("ScribbleRecognizer", "Failed to initialize ML Kit Digital Ink Recognition", e)
        }
    }

    private fun checkOrDownloadModel() {
        val targetModel = model ?: return
        val remoteModelManager = RemoteModelManager.getInstance()

        remoteModelManager.isModelDownloaded(targetModel)
            .addOnSuccessListener { downloaded ->
                if (downloaded) {
                    isModelReady = true
                } else if (!isDownloadingModel) {
                    isDownloadingModel = true
                    remoteModelManager.download(targetModel, DownloadConditions.Builder().build())
                        .addOnSuccessListener {
                            isModelReady = true
                            isDownloadingModel = false
                            android.util.Log.d("ScribbleRecognizer", "Digital Ink Model downloaded successfully")
                        }
                        .addOnFailureListener { e ->
                            isDownloadingModel = false
                            android.util.Log.e("ScribbleRecognizer", "Failed to download Digital Ink Model", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ScribbleRecognizer", "Failed to check model download status", e)
            }
    }

    /**
     * Converts UI strokes to an ML Kit Ink object and performs asynchronous handwriting recognition
     * on the Default dispatcher away from the main thread.
     */
    suspend fun recognizeStrokes(
        strokes: List<ScribbleStroke>,
        preContext: String = ""
    ): List<String> = withContext(Dispatchers.Default) {
        val activeRecognizer = recognizer
        if (activeRecognizer == null || strokes.isEmpty()) {
            return@withContext emptyList()
        }

        try {
            val inkBuilder = Ink.builder()
            for (stroke in strokes) {
                if (stroke.points.isEmpty()) continue
                val mlStrokeBuilder = Ink.Stroke.builder()
                for (pt in stroke.points) {
                    mlStrokeBuilder.addPoint(Ink.Point.create(pt.x, pt.y, pt.timestamp))
                }
                inkBuilder.addStroke(mlStrokeBuilder.build())
            }

            val ink = inkBuilder.build()
            if (ink.strokes.isEmpty()) return@withContext emptyList()

            val result: RecognitionResult = if (preContext.isNotBlank()) {
                val contextBuilder = RecognitionContext.builder()
                    .setPreContext(preContext)
                    .build()
                activeRecognizer.recognize(ink, contextBuilder).await()
            } else {
                activeRecognizer.recognize(ink).await()
            }

            result.candidates.map { it.text }
        } catch (e: Exception) {
            android.util.Log.e("ScribbleRecognizer", "Error recognizing handwriting ink", e)
            emptyList()
        }
    }

    fun close() {
        try {
            recognizer?.close()
        } catch (_: Exception) {}
    }

    companion object {
        /**
         * Selects and disambiguates the best candidate from handwriting recognition results.
         * For multi-stroke shapes (strokes.size >= 2), prioritizes letter candidates (such as 'X', 'H', 'T', 'K')
         * over single vertical bar candidates ('I', '1') or math/slash symbols ('+', '*', '/').
         */
        fun resolveCandidate(candidates: List<String>, isMultiStroke: Boolean): String {
            if (candidates.isEmpty()) return ""

            val cleaned = candidates.map { it.trim() }.filter { it.isNotEmpty() }
            if (cleaned.isEmpty()) return ""

            val top = cleaned.first()

            if (isMultiStroke) {
                // If 2+ strokes were drawn, look for an explicit single letter/digit that is not 'I', '1', or '|'
                val letterCand = cleaned.firstOrNull { cand ->
                    cand.length == 1 && cand.first().isLetterOrDigit() &&
                        !cand.equals("i", ignoreCase = true) && cand != "1" && cand != "|"
                }
                if (letterCand != null) {
                    return letterCand.uppercase()
                }

                // Fallback to any single letter/digit candidate
                val anyLetter = cleaned.firstOrNull { cand -> cand.length == 1 && cand.first().isLetterOrDigit() }
                if (anyLetter != null) {
                    return anyLetter.uppercase()
                }
            }

            return when {
                top.equals("i", ignoreCase = true) || top == "1" || top == "|" -> "I"
                top.equals("l", ignoreCase = true) -> {
                    if (cleaned.take(3).any { c -> c.equals("i", ignoreCase = true) || c == "1" || c == "|" }) "I" else "L"
                }
                else -> top.uppercase()
            }
        }
    }
}
