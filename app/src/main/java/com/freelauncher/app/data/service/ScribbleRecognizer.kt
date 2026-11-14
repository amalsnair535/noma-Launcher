package com.freelauncher.app.data.service

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionResult
import com.freelauncher.app.ui.util.ScribbleStroke
import kotlinx.coroutines.tasks.await

class ScribbleRecognizer(private val context: Context) {

    private var recognizer: DigitalInkRecognizer? = null
    private var model: DigitalInkRecognitionModel? = null
    private var isDownloadingModel = false
    private var isModelReady = false

    init {
        initializeModel()
    }

    private fun initializeModel() {
        try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
            if (modelIdentifier != null) {
                model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
                recognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(model!!).build()
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
     * Converts UI strokes to an ML Kit Ink object and performs asynchronous handwriting recognition.
     */
    suspend fun recognizeStrokes(strokes: List<ScribbleStroke>): List<String> {
        val activeRecognizer = recognizer
        if (activeRecognizer == null || strokes.isEmpty()) {
            return emptyList()
        }

        return try {
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
            if (ink.strokes.isEmpty()) return emptyList()

            val result: RecognitionResult = activeRecognizer.recognize(ink).await()
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
}
