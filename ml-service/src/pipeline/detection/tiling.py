import numpy as np
import logging

from typing import Iterator

logger = logging.getLogger(__name__)

def generate_tiles(
        image: np.ndarray,
        tile_size: int,
        overlap: float
) -> Iterator[tuple[np.ndarray, int, int]]:
    """
    Split an image into overlapping tiles.

    Args:
        image (np.ndarray): Input image.
        tile_size (int): Size of each tile.
        overlap (float): Overlap ratio between tiles (0 <= overlap < 1).

    Returns:
        Iterator[tuple[np.ndarray, int, int]]: (tile, x_offset, y_offset).
    """
    if image is None or image.size == 0:
        logger.warning("Invalid image received in generate_tiles()")
        return

    if tile_size <= 0:
        raise ValueError("tile_size must be > 0")

    if not (0 <= overlap < 1):
        raise ValueError("overlap must be between 0 and 1")

    height, width = image.shape[:2]

    if width <= tile_size and height <= tile_size:
        yield image, 0, 0
        return

    step = int(tile_size * (1 - overlap))
    step = max(1, step)

    visited = set()

    for y in range(0, height, step):
        for x in range(0, width, step):

            # Shift-back - ensure tile stays fully inside image
            x_start = min(x, width - tile_size)
            y_start = min(y, height - tile_size)

            # Clamp to valid range for small images
            x_start = max(0, x_start)
            y_start = max(0, y_start)

            key = (x_start, y_start)

            if key in visited:
                continue
            visited.add(key)

            x_end = x_start + tile_size
            y_end = y_start + tile_size

            tile = image[y_start:y_end, x_start:x_end]

            yield tile, x_start, y_start