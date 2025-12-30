package com.saip.lumio

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult

class FaceDetectorHelper(val context: Context) {
    private var faceDetector: FaceDetector? = null

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_detector.tflite")
                .setDelegate(Delegate.CPU)
                .build()

            val options = FaceDetector.FaceDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinDetectionConfidence(0.5f)
                .setRunningMode(RunningMode.IMAGE)
                .build()

            faceDetector = FaceDetector.createFromOptions(context, options)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun detect(bitmap: Bitmap): FaceDetectorResult? {
        if (faceDetector == null) return null
        return try {
            val mpImage = BitmapImageBuilder(bitmap.copy(Bitmap.Config.ARGB_8888, true)).build()
            faceDetector?.detect(mpImage)
        } catch (e: Exception) { null }
    }
}