package com.saip.lumio

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import kotlin.math.sqrt

class FaceRecognitionHelper(context: Context) {
    private val interpreter: Interpreter
    private var imageProcessor: ImageProcessor
    private var inputSize = 160
    private var outputSize = 128

    init {
        // Load facenet.tflite
        val model = FileUtil.loadMappedFile(context, "facenet.tflite")
        val options = Interpreter.Options().apply { setNumThreads(4) }
        interpreter = Interpreter(model, options)

        // Auto-detect model shape to prevent crashes
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()
        inputSize = inputShape[1]
        outputSize = outputShape[1]

        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(127.5f, 127.5f))
            .build()
    }

    fun getFaceEmbedding(bitmap: Bitmap): FloatArray {
        val tensorImage = TensorImage.fromBitmap(bitmap)
        val processed = imageProcessor.process(tensorImage)
        val output = Array(1) { FloatArray(outputSize) }
        interpreter.run(processed.buffer, output)
        return l2Normalize(output[0])
    }

    private fun l2Normalize(embedding: FloatArray): FloatArray {
        var sum = 0.0
        for (value in embedding) { sum += value * value }
        val norm = sqrt(sum).toFloat()
        return FloatArray(embedding.size) { i -> embedding[i] / norm }
    }

    fun calculateDistance(e1: FloatArray, e2: FloatArray): Float {
        if (e1.size != e2.size) return 100f
        var sum = 0.0
        for (i in e1.indices) {
            val diff = e1[i] - e2[i]
            sum += diff * diff
        }
        return sqrt(sum).toFloat()
    }
}