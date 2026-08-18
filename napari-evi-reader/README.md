# napari-evi-reader

A napari reader plugin for the custom `.evi` volumetric image format.

## Installation

```bash
pip install -e .
```

## Usage

```python
import napari

viewer = napari.Viewer()
viewer.open("sample.evi")
napari.run()
```
