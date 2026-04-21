import numpy as np

def _bbox_centroid(bbox: tuple[int, int, int, int]) -> np.ndarray:
    """
    Compute the centroid of a bounding box.

    Args:
        bbox (tuple): (x1, y1, x2, y2).

    Returns:
          np.ndarray: (cx, cy).
    """
    x1, y1, x2, y2 = bbox

    return np.array([(x1 + x2) / 2, (y1 + y2) / 2], dtype=np.float32)


def bbox_distance(bbox1: tuple[int, int, int, int], bbox2: tuple[int, int, int, int]) -> float:
    """
    Compute Euclidean distance between centroids of two bounding boxes.
    """
    c1 = _bbox_centroid(bbox1)
    c2 = _bbox_centroid(bbox2)

    return float(np.linalg.norm(c1 - c2))