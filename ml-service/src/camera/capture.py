import numpy as np
import cv2 as cv
import logging
import time

from src.config.settings import CAMERA_ID, FRAME_COUNT, CAPTURE_DURATION

logger = logging.getLogger(__name__)


def capture_frames(width: int, height: int) -> list[np.ndarray | None]:
    """
    Capture frames from a camera using time-based sampling.

    Args:
        width (int): Width of the captured frames.
        height (int): Height of the captured frames.

    Returns:
        list[np.ndarray | None]: List of captured frames.
    """
    cap = cv.VideoCapture(CAMERA_ID)

    cap.set(cv.CAP_PROP_FRAME_WIDTH, width)
    cap.set(cv.CAP_PROP_FRAME_HEIGHT, height)

    if not cap.isOpened():
        logger.error(f"Could not open camera {CAMERA_ID}!")
        return [None] * FRAME_COUNT

    for _ in range(10):
        cap.read()
        time.sleep(0.01)

    frames: list[np.ndarray | None] = []

    try:
        logger.info(f"Camera {CAMERA_ID} accessed successfully! Starting frame capture...")

        capture_interval = CAPTURE_DURATION / max(FRAME_COUNT - 1, 1)

        start_time = time.perf_counter()

        for frame_index in range(FRAME_COUNT):
            delay = start_time + frame_index * capture_interval - time.perf_counter()

            if delay > 0:
                time.sleep(delay)

            ret, frame = cap.read()

            if not ret or frame is None or frame.size == 0:
                logger.warning(f"Failed to capture frame {frame_index+1}")
                frame = None

            frames.append(frame)
    finally:
        cap.release()

    logger.info(f"Captured: {sum(f is not None for f in frames)}/{FRAME_COUNT} frames successfully")

    return frames