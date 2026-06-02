import cv2 as cv
import numpy as np


def generate_snapshot_heatmap(
    frame: np.ndarray,
    identities: list[dict],
    predictions: dict[int, dict]
) -> np.ndarray | None:
    """
    Generate attention map.
    """
    if frame is None or frame.size == 0:
        return None

    height, width = frame.shape[:2]

    canvas = np.full((height, width, 3), 255, dtype=np.uint8)

    cv.rectangle(canvas, (10, 10), (width - 10, height - 10), (80, 80, 80), 2)

    for subject in identities:

        subject_id = subject["id"]

        if subject_id not in predictions:
            continue

        centroid = next((c for c in reversed(subject["centroids"]) if c is not None), None)

        if centroid is None:
            continue

        x, y = map(int, centroid)

        attention = predictions[subject_id]["attention_label"]

        if attention == "low":
            color = (70, 70, 170)
        elif attention == "medium":
            color = (70, 140, 220)
        else:
            color = (80, 170, 100)

        cv.circle(canvas, (x, y),60, color, -1)

        cv.circle(canvas,(x, y),60,(50, 50, 50),2)

    return canvas