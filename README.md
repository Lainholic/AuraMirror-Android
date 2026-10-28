# 🪞 AuraMirror Android — Skin AI & eCommerce Companion

> **Native Android (Jetpack Compose + CameraX + Health Connect) skincare diagnostic & eCommerce product discovery app powered by the official Perfect Corp. YouCam Skin AI API.**

[![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84.svg)](https://developer.android.com/jetpack/compose)
[![CameraX](https://img.shields.io/badge/CameraX-1%3A1%20ViewPort-0284C7.svg)](https://developer.android.com/training/camerax)
[![YouCam Skin AI](https://img.shields.io/badge/YouCam-Skin%20AI%20%26%20eCommerce-ff69b4.svg)](https://yce.perfectcorp.com/)
[![Demo Video](https://img.shields.io/badge/YouTube-Demo%20Video-FF0000.svg?logo=youtube)](https://www.youtube.com/watch?v=2mRsUcpAKxc)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

🎥 **Live Hardware Demo Video (Galaxy S24)**: [https://www.youtube.com/watch?v=2mRsUcpAKxc](https://www.youtube.com/watch?v=2mRsUcpAKxc)

---

## ✨ Key Highlights

1. **1:1 Square Viewfinder & Instant Frozen Capture UX (`CameraCaptureScreen.kt`)**:
   - Synchronizes CameraX `Preview` and `ImageCapture` via a shared `1:1 ViewPort` (`UseCaseGroup`) with `1.35x` center digital framing and horizontal mirror alignment so the live selfie preview and the `600×600` analysis frame match seamlessly while meeting YouCam's `>60%` face-width requirement.
   - Immediately freezes the snapped photo on screen so the user can lower their phone while the ~5–8 second cloud analysis completes.
2. **Real-Time YouCam 9-Vector Skin AI + 9 Interactive PNG Mask Overlays (`ResultScreen.kt`)**:
   - Evaluates **Oiliness, Dark Circles, Moisture, Redness, Pores, Acne, Wrinkles, Texture, and Age Spots** with interactive per-metric translucent mask overlays, a 6-axis **Skin Balance Radar Chart**, and local scan-to-scan delta tracking (`▲/▼`).
3. **Health Connect Wearable Sleep Fusion (`HealthConnectManager.kt`)**:
   - Reads the user's real last-night sleep duration from Android Health Connect to contextualize daily skin recovery.
4. **eCommerce Skincare Discovery & INCI Ingredient Safety Guard (O/X)**:
   - Maps measured skin concerns to targeted active ingredients (Salicylic Acid BHA, Caffeine + EGCG, 5D Hyaluronic Acid, Centella Asiatica, Niacinamide + Zinc) with one-tap **`🛍️ Shop / Explore`** links localized to the user's selected language and region (`EN` / `JA` / `ZH` / `KO`).
   - Includes an interactive **INCI Ingredient Safety Guard (O/X)** to cross-check any cosmetic product's ingredient list against the user's measured skin scores.
5. **Instant 4-Language Toggle (`🌐 EN` / `日本語` / `中文` / `한국어`) & Dual Clinical Themes**:
   - Instant zero-latency switching across English, Japanese, Simplified Chinese, and Korean, plus **Bright Clinical Pearl** (default) and **Luminous Aurora** themes persisted in `SharedPreferences`.

---

## 🛠️ Quick Start for Judges (3 Steps)

For security, **the YouCam API Secret Key is never embedded inside the Android APK**. Instead, the Android app communicates with the included local FastAPI proxy (`backend/server.py`).

### Step 1 — Set Your YouCam API Key & Start the Proxy Server (`backend/`)
```bash
cd backend
pip install fastapi uvicorn requests pillow pydantic

# Windows PowerShell
$env:YOUCAM_API_KEY="your_youcam_api_key_here"
python -m uvicorn server:app --host 0.0.0.0 --port 8000

# macOS / Linux
export YOUCAM_API_KEY="your_youcam_api_key_here"
python -m uvicorn server:app --host 0.0.0.0 --port 8000
```

### Step 2 — Connect Android Device (or Emulator) via ADB Reverse Tunnel
```bash
adb reverse tcp:8000 tcp:8000
```

### Step 3 — Build & Install the Android App (`youcam_android`)
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.synapse.auramirror/.MainActivity
```
- Tap **`📷 Scan My Face`** (`📷 내 얼굴 촬영`) to analyze your live face, or tap **`Demo Sample`** (`데모 샘플`) to test the full pipeline without a camera using the bundled sample portrait.

---

## 🔒 Privacy & Disclaimer
- Neither the Android app nor the local proxy writes facial photos to local disk. Frames are processed strictly in-memory and transmitted to YouCam Skin AI for analysis.
- AuraMirror provides cosmetic skincare wellness reference information only and does not provide medical diagnosis or treatment.
