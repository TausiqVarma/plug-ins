"""
_reader.py
==========
Core reader logic for the napari-evi-reader plugin.

This module exposes two functions that together satisfy the npe2 reader
contract:

1. ``get_reader(path)``  — the *reader contribution* callable.
   napari calls this first to ask: "can you handle this file?"
   Return ``reader_function`` if yes, or ``None`` if no.

2. ``reader_function(path)`` — the actual I/O routine.
   Opens the ``.evi`` file, deserializes the NumPy array, and returns
   it in the canonical ``LayerData`` format that napari expects.

npe2 contract reference
-----------------------
A reader contribution must return ``Optional[ReaderFunction]`` where
``ReaderFunction`` is ``Callable[[PathOrPaths], List[LayerData]]`` and
``LayerData = Tuple[Any, Dict, str]``  →  ``(data, kwargs, layer_type)``.
"""

from __future__ import annotations

import os
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Tuple, Union

import numpy as np

# ---------------------------------------------------------------------------
# Type aliases (matching napari / npe2 conventions)
# ---------------------------------------------------------------------------
PathLike = Union[str, os.PathLike]
PathOrPaths = Union[PathLike, Sequence[PathLike]]
LayerData = Tuple[Any, Dict[str, Any], str]
ReaderFunction = Any  # Callable[[PathOrPaths], List[LayerData]]


# ---------------------------------------------------------------------------
# Public API — wired from napari.yaml
# ---------------------------------------------------------------------------

def get_reader(path: PathOrPaths) -> Optional[ReaderFunction]:
    """Decide whether this plugin can read the given *path*.

    Parameters
    ----------
    path : str | os.PathLike | Sequence[str | os.PathLike]
        A single file path or a list of paths that napari wants to open.
        For multi-file formats you'd iterate over the list; for ``.evi``
        we only support single files.

    Returns
    -------
    reader_function : callable or None
        If the path ends with ``.evi``, return ``reader_function`` so
        napari knows to hand the path to us.  Otherwise return ``None``
        to let other plugins (or napari's built-in readers) try.
    """
    # npe2 may hand us a list of paths (e.g. drag-and-drop of multiple files).
    # Normalise to a single string so we can inspect the extension.
    if isinstance(path, (list, tuple)):
        # We only support single-file reads — reject multi-file drops.
        if len(path) != 1:
            return None
        path = path[0]

    # Gate on the file extension (case-insensitive).
    if str(path).lower().endswith(".evi"):
        return reader_function

    return None


def reader_function(path: PathOrPaths) -> List[LayerData]:
    """Read an ``.evi`` file and return napari-compatible layer data.

    The ``.evi`` format is a NumPy ``.npy`` binary blob saved with a
    custom extension.  We deserialize it with ``numpy.load`` /
    ``numpy.lib.format.read_array`` and wrap the result in the
    ``LayerData`` tuple that napari's viewer expects.

    Parameters
    ----------
    path : str | os.PathLike | Sequence[str | os.PathLike]
        Path to the ``.evi`` file.  If a sequence, the first element
        is used.

    Returns
    -------
    layer_data : list of LayerData
        A single-element list containing ``(array, metadata, "image")``.
    """
    # Normalise path — same logic as get_reader.
    if isinstance(path, (list, tuple)):
        path = path[0]

    path = Path(path)

    # ------------------------------------------------------------------
    # Load the array.
    # Because the file was written with ``np.lib.format.write_array``,
    # it uses the standard .npy binary layout — just with a .evi
    # extension.  ``np.lib.format.read_array`` can parse it directly.
    # ------------------------------------------------------------------
    with open(path, "rb") as fh:
        data: np.ndarray = np.lib.format.read_array(fh, allow_pickle=False)

    # ------------------------------------------------------------------
    # Build layer metadata.
    # The dict is forwarded as **kwargs to ``Viewer.add_image()``.
    # ------------------------------------------------------------------
    metadata: Dict[str, Any] = {
        "name": "EVI Image",  # layer name shown in napari's layer list
    }

    # Return the canonical LayerData list.
    # Each element is a 3-tuple: (data, add_kwargs, layer_type).
    return [(data, metadata, "image")]
