"""Quick verification that the napari-evi-reader plugin works.

Uses plugin=None so napari auto-discovers our reader via npe2,
rather than the default plugin='napari' which only tries the built-in reader.
"""
import napari

viewer = napari.Viewer()

# plugin=None triggers the auto-discovery path:
#   napari finds all compatible readers for *.evi → ['napari-evi-reader']
#   since there's exactly one, it auto-selects it
viewer.open(r"c:\OME-Zarr research\plug-in\sample.evi", plugin=None)

napari.run()
