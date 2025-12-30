package com.saip.lumio

import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class SceneDescriberHelper(val context: Context) {
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    fun process(image: InputImage, onSuccess: (String) -> Unit, onError: () -> Unit) {
        labeler.process(image)
            .addOnSuccessListener { labels ->
                if (labels.isEmpty()) {
                    onSuccess("Scene unclear.")
                } else {
                    // Take top 3 labels and join them naturally
                    val descriptions = labels.take(3).joinToString(", ") { it.text }
                    onSuccess("I see: $descriptions")
                }
            }
            .addOnFailureListener { onError() }
    }
}