package com.saip.lumio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import com.saip.lumio.databinding.ActivityMainBinding
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class, androidx.camera.core.ExperimentalLensFacing::class)
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, LumioGestures.GestureListener {

    private lateinit var binding: ActivityMainBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var textToSpeech: TextToSpeech? = null
    private var speechRate = 1.0f

    // AI Helpers (Cleaned up)
    private lateinit var objectHelper: ObjectDetectorHelper
    private lateinit var faceDetector: FaceDetectorHelper
    private lateinit var faceRecognizer: FaceRecognitionHelper
    private lateinit var handHelper: HandLandmarkerHelper
    private lateinit var textReader: TextReaderHelper // NEW
    private lateinit var sceneDescriber: SceneDescriberHelper // NEW

    // State
    private var currentTrigger: String = ""
    private var isProcessing = false
    private var currentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // Add Person Workflow State
    private var isAddPersonMode = false
    private var tempDetectedName: String? = null // Holds the name "John" before saving
    private var pendingFaceEmbedding: FloatArray? = null // Holds the math data

    // Face Database
    private var knownFaces = mutableMapOf<String, FloatArray>()
    private val gson = Gson()
    private lateinit var nameInputRef: EditText

    // Settings Flags
    private var featText=true; private var featScene=true; private var featObject=true; private var featPerson=true; private var featTouchRead=true

    private val mainHandler = Handler(Looper.getMainLooper())

    // Voice Input Result
    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val res = result.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!res.isNullOrEmpty()) {
                val spokenName = res[0]
                binding.etPersonName.setText(spokenName)

                // NEW: Store name and ask for Triple Tap confirmation
                tempDetectedName = spokenName
                speak("Heard $spokenName. Triple tap to save.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        textToSpeech = TextToSpeech(this, this)

        // Init Helpers
        objectHelper = ObjectDetectorHelper(this)
        faceDetector = FaceDetectorHelper(this)
        faceRecognizer = FaceRecognitionHelper(this)
        handHelper = HandLandmarkerHelper(this)
        textReader = TextReaderHelper(this) // NEW
        sceneDescriber = SceneDescriberHelper(this) // NEW

        loadFaceDatabase()

        // Gestures
        val gestureHandler = LumioGestures(this, this)
        binding.viewFinder.setOnTouchListener(gestureHandler)
        binding.layoutAddPerson.setOnTouchListener(gestureHandler) // Overlay Gestures

        // UI
        binding.tvOverlay.setOnClickListener { binding.tvOverlay.visibility = View.GONE; speak("Lumio Ready.") }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.btnSwitchCamera.setOnClickListener { toggleCamera() }

        // Manual Add Person UI (Fallback for sighted users)
        binding.btnSavePerson.setOnClickListener {
            val name = binding.etPersonName.text.toString()
            if (name.isNotBlank()) confirmSave(name)
        }
        binding.btnCancelAdd.setOnClickListener { closeAddPersonMode() }

        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        isProcessing = false
        currentTrigger = ""
        val prefs = getSharedPreferences("LumioPrefs", Context.MODE_PRIVATE)
        featText = prefs.getBoolean("feat_text", true); featScene = prefs.getBoolean("feat_scene", true)
        featObject = prefs.getBoolean("feat_object", true); featPerson = prefs.getBoolean("feat_person", true)
        featTouchRead = prefs.getBoolean("feat_touch_read", true); speechRate = prefs.getFloat("speech_rate", 1.0f)
        textToSpeech?.setSpeechRate(speechRate)
    }

    // --- GESTURES LOGIC ---

    override fun onDoubleTap() {
        if (!isAddPersonMode && featText) triggerAnalysis("TEXT")
    }

    override fun onTripleTap() {
        // SPECIAL CASE: Saving a person
        if (isAddPersonMode && tempDetectedName != null && pendingFaceEmbedding != null) {
            confirmSave(tempDetectedName!!)
            return
        }

        // Normal Case: Scene
        if (!isAddPersonMode && featScene) triggerAnalysis("SCENE")
    }

    override fun onTwoFingerSwipe() {
        if (!isAddPersonMode && featObject) triggerAnalysis("OBJECT")
    }

    override fun onLongPress() {
        if (!isAddPersonMode && featTouchRead) { speak("Touch Reader."); triggerAnalysis("TOUCH_READ") }
    }

    override fun onTwoFingerTap() {
        if (isAddPersonMode) {
            // MODE: Add Person -> Trigger Voice Input
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            speechLauncher.launch(intent)
        } else if (featPerson) {
            // MODE: Normal -> Identify Person
            triggerAnalysis("PERSON")
        }
    }

    private fun triggerAnalysis(type: String) {
        if (isProcessing) return
        if (type != "TOUCH_READ") speak("Scanning...")
        currentTrigger = type
        isProcessing = true
        mainHandler.postDelayed({ isProcessing = false }, 5000)
    }

    // --- CAMERA PIPELINE ---
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            val analyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                .also { it.setAnalyzer(cameraExecutor) { proxy -> processImage(proxy) } }
            try { provider.unbindAll(); provider.bindToLifecycle(this, currentCameraSelector, preview, analyzer) }
            catch (e: Exception) { speak("Camera error.") }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleCamera() {
        currentCameraSelector = if(currentCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        startCamera()
    }

    private fun processImage(imageProxy: ImageProxy) {
        if (currentTrigger == "") { imageProxy.close(); return }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val mlKitImage = com.google.mlkit.vision.common.InputImage.fromMediaImage(mediaImage, rotation)
            val bitmap = imageProxy.toBitmap()
            val rotatedBitmap = rotateBitmap(bitmap, rotation.toFloat())

            val mode = currentTrigger
            currentTrigger = ""

            when (mode) {
                // NEW: Uses Helper Class
                "TEXT" -> textReader.process(mlKitImage,
                    onSuccess = { speak(it); imageProxy.close(); isProcessing = false },
                    onError = { speak("Text Error"); imageProxy.close(); isProcessing = false }
                )

                // NEW: Uses Helper Class
                "SCENE" -> sceneDescriber.process(mlKitImage,
                    onSuccess = { speak(it); imageProxy.close(); isProcessing = false },
                    onError = { speak("Scene Error"); imageProxy.close(); isProcessing = false }
                )

                "OBJECT" -> {
                    val result = objectHelper.detect(rotatedBitmap)
                    if (result != null && result.detections().isNotEmpty()) {
                        val cat = result.detections()[0].categories()[0]
                        speak("This is a ${cat.categoryName()}")
                    } else speak("No object found.")
                    imageProxy.close(); isProcessing = false
                }

                "PERSON" -> {
                    val result = faceDetector.detect(rotatedBitmap)
                    if (result != null && result.detections().isNotEmpty()) {
                        val face = result.detections()[0]
                        val box = face.boundingBox()
                        val w = rotatedBitmap.width; val h = rotatedBitmap.height

                        // Valid Crop Check
                        if (box.left >= 0 && box.top >= 0 && box.right <= w && box.bottom <= h && box.width() > 0 && box.height() > 0) {
                            val faceBitmap = Bitmap.createBitmap(rotatedBitmap, box.left.toInt(), box.top.toInt(), box.width().toInt(), box.height().toInt())
                            val embedding = faceRecognizer.getFaceEmbedding(faceBitmap)
                            identifyFace(embedding)
                        } else speak("Face not clear.")
                    } else speak("No person detected.")
                    imageProxy.close(); isProcessing = false
                }

                "TOUCH_READ" -> runTouchReader(rotatedBitmap, mlKitImage, imageProxy)
                else -> { imageProxy.close(); isProcessing = false }
            }
        } else { imageProxy.close(); isProcessing = false }
    }

    // --- PERSON ID & DB LOGIC ---
    private fun identifyFace(currentEmbedding: FloatArray) {
        var bestName: String? = null
        var bestDist = 0.8f

        for ((name, savedEmbedding) in knownFaces) {
            val dist = faceRecognizer.calculateDistance(currentEmbedding, savedEmbedding)
            if (dist < bestDist) { bestDist = dist; bestName = name }
        }

        if (bestName != null) {
            speak("$bestName is here.")
        } else {
            speak("Unknown person. Double tap with 2 fingers to speak name.")
            pendingFaceEmbedding = currentEmbedding
            tempDetectedName = null // Reset
            runOnUiThread { openAddPersonMode() }
        }
    }

    private fun openAddPersonMode() {
        isAddPersonMode = true
        binding.etPersonName.setText("")
        binding.layoutAddPerson.visibility = View.VISIBLE
    }

    private fun confirmSave(name: String) {
        if (pendingFaceEmbedding != null) {
            knownFaces[name] = pendingFaceEmbedding!!
            val json = gson.toJson(knownFaces)
            getSharedPreferences("LumioFaces", Context.MODE_PRIVATE).edit().putString("db_embeddings", json).apply()
            speak("Saved $name.")
            closeAddPersonMode()
        }
    }

    private fun closeAddPersonMode() {
        isAddPersonMode = false
        binding.layoutAddPerson.visibility = View.GONE
        pendingFaceEmbedding = null
        tempDetectedName = null
    }

    private fun loadFaceDatabase() {
        val json = getSharedPreferences("LumioFaces", Context.MODE_PRIVATE).getString("db_embeddings", null)
        if (json != null) {
            val type = object : TypeToken<MutableMap<String, FloatArray>>() {}.type
            knownFaces = gson.fromJson(json, type)
        }
    }

    // --- TOUCH READER (PINCH) ---
    private fun runTouchReader(bitmap: Bitmap, mlKitImage: com.google.mlkit.vision.common.InputImage, proxy: ImageProxy) {
        val handResult = handHelper.detect(bitmap)
        if (handResult == null || handResult.landmarks().isEmpty()) {
            speak("No hand detected."); proxy.close(); isProcessing = false; return
        }
        val lm = handResult.landmarks()[0]
        val thumb = lm[4]; val index = lm[8]
        val w = bitmap.width; val h = bitmap.height
        val left = min(thumb.x(), index.x()) * w; val right = max(thumb.x(), index.x()) * w
        val top = min(thumb.y(), index.y()) * h; val bottom = max(thumb.y(), index.y()) * h
        val box = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

        // Use text helper logic inside pinch?
        // For simplicity, we keep ML Kit direct call here as it needs 'box' intersection logic
        val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(mlKitImage).addOnSuccessListener { vt ->
            var found = false; val sb = StringBuilder()
            for (b in vt.textBlocks) {
                if (b.boundingBox != null && Rect.intersects(b.boundingBox!!, box)) {
                    // Use simple text for touch reader, or can refine later
                    sb.append(b.text).append(" "); found = true
                }
            }
            if (found) speak(sb.toString()) else speak("No text in pinch.")
        }.addOnCompleteListener { proxy.close(); isProcessing = false }
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix(); matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
    private fun speak(text: String) { textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) { textToSpeech?.language = Locale.US; startCamera() } }
    private fun checkPermissions() { if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10) } }
}