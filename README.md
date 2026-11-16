# 🖱️ 3D Mouse — Open Source CAD Viewport Controller for Android & PC

![License: MIT](https://img.shields.io/badge/License-MIT-orange.svg)
![Android: 8.0+](https://img.shields.io/badge/Android-8.0%2B-brightgreen.svg)
![Blender | Fusion 360 | SolidWorks](https://img.shields.io/badge/CAD-Blender%20%7C%20Fusion%20360%20%7C%20SolidWorks-blue.svg)

> **3D Mouse** turns your Android smartphone into a high-precision, low-latency 3D viewport navigation controller for **Blender**, **Autodesk Fusion 360**, **SolidWorks**, and **FreeCAD**.
> 
> **100% Free & Open Source | Zero Ads | Zero Tracking | Pure AMOLED Design**

---

## ✨ Features

- 🌀 **Smooth 3D Orbit, Pan & Zoom**: High-frequency rate-limiting (up to 165 FPS) with exponential low-pass filtering.
- 🧠 **Smart AI Gesture Engine**:
  - **AI Non-Linear Velocity Scaling**: Micro-drags automatically trigger sub-millimeter precision (`0.35x`), fast swipes scale up to `1.65x`.
  - **AI Axis Snapping**: Snaps near-horizontal or vertical orbits to pure X/Z axes for turntable rotation.
  - **Multi-Touch Trackpad Gestures**:
    - **1-Finger Double-Tap**: Toggle `0.25x` Precision Mode.
    - **2-Finger Double-Tap**: Reset Home View / Fit to Screen.
    - **3-Finger Tap**: Undo (`Ctrl + Z`).
    - **4-Finger Tap**: Redo (`Ctrl + Y` / `Ctrl + Shift + Z`).
    - **3-Finger Swipes**: Instant View Plane Snapping (Top, Front, Side views).
- 📳 **Tactile Haptic Vibration Feedback**: Mechanical ticks on zoom notches and button clicks with 1x-10x strength slider.
- 🎨 **AMOLED Customization**: Pure `#000000` AMOLED surface with 10 Neon edge border colors + 🌈 RGB Rainbow animated loop mode.
- 🔒 **App Exit Lock Protection**: Intercepts Android Back gestures to prevent accidental app minimization while modeling.
- 🌐 **Wi-Fi Router & Windows Mobile Hotspot Gateway Auto-Detection**: Dual IP auto-discovery with persistent memory.

---

## 🛠️ Quick Setup Guide

### 1. PC Receiver Setup (Windows)
1. Download `start_pc_receiver.bat` and `space_mouse_receiver.py` from the [Releases](../../releases) section or directly from the app.
2. Double-click `start_pc_receiver.bat` on your PC.
3. The script automatically configures Windows Firewall and launches the Python receiver server with a Cyberpunk Matrix animation.

### 2. Android App Connection
1. Install `3D_Mouse.apk` on your phone.
2. Connect both phone and PC to the same Wi-Fi network **OR** connect your phone directly to your PC's **Mobile Hotspot**.
3. Launch the app. The green status circle will light up when connected!

---

## 💻 Supported 3D Software Presets

| Preset | Orbit | Pan | Zoom | Home View | Undo / Redo |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Fusion 360** | Shift + MMB | MMB | Wheel | `F6` | `Ctrl+Z` / `Ctrl+Y` |
| **Blender** | MMB | Shift + MMB | Wheel | `Home` / `Numpad .` | `Ctrl+Z` / `Ctrl+Shift+Z` |
| **SolidWorks** | MMB | Ctrl + MMB | Wheel | `F` | `Ctrl+Z` / `Ctrl+Y` |

---

## 📜 License & Credits

Distributed under the **MIT License**. See `LICENSE` for more information.

Created by **instagram m4ximys**
