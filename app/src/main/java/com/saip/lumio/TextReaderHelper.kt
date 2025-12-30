package com.saip.lumio

import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextReaderHelper(val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun process(image: InputImage, onSuccess: (String) -> Unit, onError: () -> Unit) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (visionText.textBlocks.isEmpty()) {
                    onSuccess("No text found.")
                    return@addOnSuccessListener
                }

                val sb = StringBuilder()

                // Smart Parsing for Natural Reading
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        var lineText = line.text.trim()

                        // 1. Handle Hyphenation (Word wrap)
                        if (lineText.endsWith("-")) {
                            // Remove hyphen and join directly to next line (e.g. "Amaz- ing" -> "Amazing")
                            lineText = lineText.dropLast(1)
                            sb.append(lineText)
                        }
                        // 2. Handle Punctuation (Pause)
                        else if (lineText.endsWith(".") || lineText.endsWith("?") || lineText.endsWith("!")) {
                            sb.append(lineText).append(". ") // Add explicit pause
                        }
                        // 3. Standard line break (Join with space)
                        else {
                            sb.append(lineText).append(" ")
                        }
                    }
                    // Paragraph break (optional pause between blocks)
                    sb.append("  ")
                }

                onSuccess(sb.toString().trim())
            }
            .addOnFailureListener { onError() }
    }
}