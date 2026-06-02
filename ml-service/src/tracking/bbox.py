import numpy as np


def bbox_centroid(bbox: tuple[int, int, int, int]) -> tuple[float, float]:
    """
    Compute the centroid of a bounding box.
    """
    x1, y1, x2, y2 = bbox

    return (
        (x1 + x2) / 2,
        (y1 + y2) / 2,
    )


def centroid_distance(c1: tuple[float, float], c2: tuple[float, float]) -> float:
    """
    Compute Euclidean distance between two centroids.
    """
    return float(np.linalg.norm(np.subtract(c1, c2)))