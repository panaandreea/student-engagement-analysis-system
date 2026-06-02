import cv2 as cv
import logging

from src.config.settings import CAMERA_ID

logger = logging.getLogger(__name__)


def get_max_camera_resolution() -> tuple[int, int]:
    """
    Determine the maximum supported camera resolution.

    Returns:
        tuple[int, int]: Maximum (width, height) resolution.
    """
    resolutions = [
        (3840, 2160),
        (2560, 1440),
        (1920, 1080),
        (1280, 720),
        (640, 480),
    ]

    cap = cv.VideoCapture(CAMERA_ID)

    if not cap.isOpened():
        logger.error(f"Failed to open camera {CAMERA_ID}")
        return 0, 0

    try:
        logger.info(f"Probing resolutions for camera {CAMERA_ID}...")

        for width, height in resolutions:

            cap.set(cv.CAP_PROP_FRAME_WIDTH, width)
            cap.set(cv.CAP_PROP_FRAME_HEIGHT, height)

            actual_width = int(cap.get(cv.CAP_PROP_FRAME_WIDTH))
            actual_height = int(cap.get(cv.CAP_PROP_FRAME_HEIGHT))

            logger.info(
                f"Requested: {width}x{height} | Got: {actual_width}x{actual_height}"
            )

            if (actual_width, actual_height) == (width, height):
                return width, height
    finally:
        cap.release()

    return 0, 0