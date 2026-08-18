"""
generate_colored_evi.py
========================
Generates a visually striking 3D `.evi` file with a bright sphere
in the center surrounded by darkness. When opened in Fiji or napari,
you will immediately see a glowing ball — unmistakable proof the
plugin is working.
"""

import numpy as np
from pathlib import Path


def generate_sphere_volume(
    depth: int = 64,
    height: int = 128,
    width: int = 128,
) -> np.ndarray:
    """Create a 3D volume with a bright sphere in the center."""

    # Create coordinate grids centered at the middle of the volume
    z, y, x = np.mgrid[0:depth, 0:height, 0:width]

    # Center of the volume
    cz, cy, cx = depth / 2, height / 2, width / 2

    # Distance from center for every voxel
    distance = np.sqrt((z - cz) ** 2 + (y - cy) ** 2 + (x - cx) ** 2)

    # Create a sphere: bright inside radius 30, dark outside
    sphere_radius = 30.0
    volume = np.zeros((depth, height, width), dtype=np.float32)

    # Bright core sphere (intensity 255)
    volume[distance < sphere_radius] = 255.0

    # Smooth glowing edge (fade from 255 to 0 over 10 voxels)
    fade_zone = (distance >= sphere_radius) & (distance < sphere_radius + 10)
    fade_values = 255.0 * (1.0 - (distance[fade_zone] - sphere_radius) / 10.0)
    volume[fade_zone] = fade_values

    # Add a smaller bright ring/torus in the middle slice for extra visual interest
    mid_z = depth // 2
    for z_slice in range(mid_z - 2, mid_z + 3):
        ring_dist = np.sqrt((y[z_slice] - cy) ** 2 + (x[z_slice] - cx) ** 2)
        ring_mask = (ring_dist > 40) & (ring_dist < 50)
        volume[z_slice][ring_mask] = 200.0

    return volume


if __name__ == "__main__":
    volume = generate_sphere_volume()
    out_path = Path(__file__).parent / "sample.evi"

    # Write using NumPy's .npy binary format but with .evi extension
    with open(out_path, "wb") as f:
        np.lib.format.write_array(f, volume, allow_pickle=False)

    print(f"Generated EVI file: {out_path}")
    print(f"  Shape : {volume.shape}")
    print(f"  Dtype : {volume.dtype}")
    print(f"  Min   : {volume.min():.1f}")
    print(f"  Max   : {volume.max():.1f}")
    print(f"  Size  : {out_path.stat().st_size / 1024:.1f} KB")
    print()
    print("You should see a bright glowing sphere when opened in Fiji or napari!")
