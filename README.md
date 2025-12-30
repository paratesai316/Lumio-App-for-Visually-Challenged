# Lumio: AI-Powered Vision for the Visually Impaired

<p align="left">
  <img src="logo.svg"
     height="120"
     style="float:left; margin:0 16px 16px 0; object-fit: contain;" />

  Lumio is an intelligent Android application designed to serve as a digital eye for blind and visually impaired individuals. By leveraging advanced on-device Machine Learning, Lumio instantly translates the visual world—text, objects, scenes, and people—into clear, audible descriptions.
</p>
<br clear="left"/>

## 🚀 Primary Objective

To empower independence by providing a completely non-visual, gesture-based interface that allows users to navigate complex environments without internet connectivity or expensive proprietary hardware.

## 🌟 Key Features

1. **📄 Smart Text Reader**

    Function: Instantly reads signs, menus, and documents.

    Intelligence: Detects columns in newspapers/menus to read content in the correct order. Pauses for headers and distinct sections.

    Gesture: Single-finger double tap.

2. **☕ Object Identifier**

    Function: Identifies 300+ distinct items (e.g., Laptop, Coffee Cup, Keys).

    Tech: EfficientDet-Lite2 via MediaPipe for high-accuracy detection.

    Gesture: Two-finger swipe.

3. **🏙️ Scene Describer**

    Function: Provides a contextual summary of the environment
    (e.g., “A living room with a couch and TV”).

    Tech: Google ML Kit Image Labeling.

    Gesture: Single-finger triple tap.

4. **👤 Person Recognition (Biometric ID)**

    Function: Detects people and identifies them by name if they exist in the user’s database.

    Workflow: Unknown people can be added by speaking their name via a simple voice prompt.

    Tech: MediaPipe Face Detection + FaceNet Embeddings (TensorFlow Lite).

    Gesture: Two-finger tap.

5. **🤏 Touch-to-Read (Tactile Mode)**

    Function: Allows users to pinch their fingers around a specific paragraph on a physical document to read only that text.

    Tech: MediaPipe Hand Tracking + OCR.

    Gesture: Long press.

6. **👓 Wearable Mode**

    Function: Supports external USB cameras (UVC).
    Users can clip a small camera (e.g., Waveshare OV5640) to glasses, connect via USB-OTG, and use the phone as a processing unit in their pocket.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Camera**: Android CameraX (Back, Front, and USB/External support)
- **ML Engines**

  - Google MediaPipe: Object Detection, Face Detection, Hand Landmarks
  - Google ML Kit: Text Recognition, Image Labeling
  - TensorFlow Lite: FaceNet (Face Embeddings)
  - Audio: Android TextToSpeech (TTS)

## 📱 Gesture Controls (Accessibility Guide)

Lumio uses a full-screen invisible gesture overlay, so users never need to locate physical or on-screen buttons.

| Action          | Gesture               | Description                                                |
|-----------------|-----------------------|------------------------------------------------------------|
| Read Text       | 1-Finger Double Tap   | Scans and reads visible text.                              |
| Describe Scene  | 1-Finger Triple Tap   | Describes the general environment.                         |
| Identify Object | 2-Finger Swipe        | Detects the dominant object in front.                      |
| Identify Person | 2-Finger Tap          | Identifies the person. If unknown, prompts to add.         |
| Touch Reader    | Long Press            | Activates hand tracking to read text between fingers.      |
| Add Person      | 2-Finger Double Tap   | (When prompted) Activates microphone to save a name.       |

## ⚙️ Installation & Setup

1. **Clone the Repository**

    ` git clone https://github.com/yourusername/lumio.git `

2. **Add ML Models**

    Download the following files and place them in:

    ` app/src/main/assets/ `

    - ` object_detector_v2.tflite ` (EfficientDet-Lite2)

    - ` face_detector.tflite ` (MediaPipe Face Detection)

    - ` facenet.tflite ` (FaceNet Mobile)

    - ` hand_landmarker.task ` (MediaPipe Hand Solutions)

3. **Build in Android Studio

    Ensure minSdkVersion is 24 or higher

    Sync Gradle files

4. **Permissions**

    The app requires CAMERA permission to function.

## 🧪 Hardware Prototype (Optional)

Lumio is designed to function as a wearable prototype.

- **Camera**: Waveshare OV5640 (5MP USB Camera – Auto Focus)

- **Connection**: USB-C to USB-A OTG adapter

- *Usage**:

  - Connect the camera
  - Open Lumio
  - Tap Switch Camera (top right) until the voice announces
    “Goggles Connected”

## 🔮 Future Roadmap

- 🌍 Multi-Language Support: Automatic translation of foreign text

- 🧭 GPS Navigation: Walking directions via Google Maps

- 💡 Light Detector: Audio cues for light sources (windows, lamps)

- 💵 Currency Reader: Banknote identification

##

#### Developed with ❤️ for Accessibility
