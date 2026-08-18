# EVI Reader Plugins

This repository contains custom IO plugins for reading `.evi` files in both **napari** (Python) and **Fiji / ImageJ** (Java).

## 📥 Ready to Install

If you just want to use the plugins, you do not need to build them from source. Download the pre-compiled files from the `ready-to-install/` folder:

### For napari
1. Download `napari_evi_reader.whl` from the `ready-to-install/` folder.
2. Open your terminal/command prompt and run:
   ```bash
   pip install napari_evi_reader.whl
   ```
3. Open napari and drag-and-drop your `.evi` file into the window!

### For Fiji / ImageJ
1. Download `Open_EVI.jar` from the `ready-to-install/` folder.
2. Place the `.jar` file into your Fiji `plugins/` directory (e.g., `Fiji.app/plugins/`).
3. Restart Fiji.
4. Click **Plugins > Open EVI** in the top menu and select your file.

---

## 💻 Source Code (For Developers)

- **`napari-evi-reader/`**: Contains the Python source code using the modern `npe2` manifest system.
- **`fiji-evi-reader/`**: Contains the Java source code for the legacy ImageJ 1.x `PlugIn` architecture.
