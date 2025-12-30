package com.saip.lumio

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

class ObjectDetectorHelper(val context: Context) {
    private var objectDetector: ObjectDetector? = null

    init {
        try {
            // UPDATED: Using V2 for Maximum Accuracy
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("object_detector_v2.tflite")
                .setDelegate(Delegate.CPU)
                .build()

            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setScoreThreshold(0.3f) // Keep 30% for sensitivity
                .setMaxResults(3)
                .setRunningMode(RunningMode.IMAGE)
                .build()

            objectDetector = ObjectDetector.createFromOptions(context, options)
        } catch (e: Exception) {
            // Fallback to original if V2 is missing
            try {
                val baseOptions = BaseOptions.builder().setModelAssetPath("object_detector.tflite").setDelegate(Delegate.CPU).build()
                val options = ObjectDetector.ObjectDetectorOptions.builder().setBaseOptions(baseOptions).setScoreThreshold(0.3f).setRunningMode(RunningMode.IMAGE).build()
                objectDetector = ObjectDetector.createFromOptions(context, options)
            } catch (e2: Exception) { e2.printStackTrace() }
        }
    }

    fun detect(bitmap: Bitmap): ObjectDetectorResult? {
        if (objectDetector == null) return null
        return try {
            val mpImage = BitmapImageBuilder(bitmap.copy(Bitmap.Config.ARGB_8888, true)).build()
            objectDetector?.detect(mpImage)
        } catch (e: Exception) { null }
    }
}