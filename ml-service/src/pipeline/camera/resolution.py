import cv2 as cv
import logging

logger = logging.getLogger(__name__)

WARMUP_FRAMES = 3

def get_max_camera_resolution(camera_index: int) -> tuple[int, int]:
    """
    Determine the maximum resolution for a camera.

    Args:
        camera_index (int): Index of the camera device.

    Returns:
        tuple[int, int]: Maximum (width, height) resolution.

    Raises:
        RuntimeError: If the camera cannot be opened.
    """
    resolutions = [
        (3840, 2160),
        (2560, 1440),
        (1920, 1080),
        (1280, 720),
        (640, 480),
    ]

    max_width, max_height = 0, 0

    cap = cv.VideoCapture(camera_index)

    if not cap.isOpened():
        logger.error(f"Failed to open camera {camera_index}")
        raise RuntimeError(f"Could not open camera {camera_index}")

    try:
        cap.set(cv.CAP_PROP_FOURCC, cv.VideoWriter.fourcc(*'MJPG'))

        logger.info(f"Probing resolutions for camera {camera_index}...")

        for width, height in resolutions:
            cap.set(cv.CAP_PROP_FRAME_WIDTH, width)
            cap.set(cv.CAP_PROP_FRAME_HEIGHT, height)

            for _ in range(WARMUP_FRAMES):
                ret, _ = cap.read()
                if not ret:
                    logger.warning(f"Failed warmup read at resolution {width}x{height}")
                    break

            actual_width = int(cap.get(cv.CAP_PROP_FRAME_WIDTH))
            actual_height = int(cap.get(cv.CAP_PROP_FRAME_HEIGHT))

            logger.info(
                f"Requested: {width}x{height} | Got: {actual_width}x{actual_height}"
            )

            if actual_width * actual_height > max_width * max_height:
                max_width, max_height = actual_width, actual_height

    finally:
        cap.release()

    logger.info(f"Camera {camera_index} max resolution detected: {max_width}x{max_height}")

    return max_width, max_height