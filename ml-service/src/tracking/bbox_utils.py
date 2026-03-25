import numpy as np

def bbox_centroid(bbox):
    """
    Compute the centroid of a bounding box

    Args:
        bbox (tuple): (x1, y1, x2, y2)

    Returns:
          np.array: (cx, cy)
    """
    x1, y1, x2, y2 = bbox

    return np.array([(x1 + x2) / 2, (y1 + y2) / 2], dtype=np.float32)


def bbox_distance(bbox1, bbox2):
    """
    Compute Euclidean distance between centroids of two bounding boxes
    """
    c1 = bbox_centroid(bbox1)
    c2 = bbox_centroid(bbox2)

    return np.linalg.norm(c1 - c2)


def bbox_similarity(bbox1, bbox2):
    """
    Convert distance to similarity score in range (0, 1].
    """
    dist = bbox_distance(bbox1, bbox2)

    return 1 / (1 + dist)