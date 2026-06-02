import numpy as np
import logging

from typing import Iterator

logger = logging.getLogger(__name__)


def split_into_tiles(frame: np.ndarray, tile_size: int, overlap: float) -> Iterator[tuple[np.ndarray, int, int]]:
    """
    Split an image into overlapping tiles.

    Args:
        frame (np.ndarray): Input image.
        tile_size (int): Size of each tile.
        overlap (float): Overlap ratio between tiles (0 <= overlap < 1).

    Returns:
        Iterator[tuple[np.ndarray, int, int]]: (tile, x_offset, y_offset).
    """
    if frame is None or frame.size == 0:
        logger.warning("Invalid frame received in split_into_tiles()")
        return

    if tile_size <= 0:
        logger.error("tile_size must be > 0")
        yield frame, 0, 0
        return

    if not (0 <= overlap < 1):
        logger.error("overlap must be between 0 and 1")
        yield frame, 0, 0
        return

    height, width = frame.shape[:2]

    if width <= tile_size and height <= tile_size:
        yield frame, 0, 0
        return

    step = int(tile_size * (1 - overlap))
    step = max(1, step)

    processed_tiles = set()

    for y_offset in range(0, height, step):
        for x_offset in range(0, width, step):

            tile_x, tile_y = min(x_offset, width - tile_size), min(y_offset, height - tile_size)

            tile_x, tile_y = max(0, tile_x), max(0, tile_y)

            tile_coordinates = (tile_x, tile_y)

            if tile_coordinates in processed_tiles:
                continue

            processed_tiles.add(tile_coordinates)

            x_end, y_end = tile_x + tile_size, tile_y + tile_size

            tile = frame[tile_y:y_end, tile_x:x_end]

            yield tile, tile_x, tile_y