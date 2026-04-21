import numpy as np
import cv2 as cv
import logging
import time

logger = logging.getLogger(__name__)

WARMUP_FRAMES = 10
WARMUP_DELAY = 0.3
RETRY_DELAY = 0.05

def capture_frames(
        camera_id: int,
        width: int,
        height: int,
        number_of_frames: int,
        duration: float,
        max_retries: int = 3,
        skip_on_fail: bool = True
) -> list[np.ndarray | None]:
    """
    Capture frames from a camera using time-based sampling.

    Args:
        camera_id (int): Camera ID.
        width (int): Width of the captured frames.
        height (int): Height of the captured frames.
        number_of_frames (int): Number of frames to capture.
        duration (float): Total capture duration in seconds.
        max_retries (int): Maximum number of retries.
        skip_on_fail (bool): If True, failed frame captures are skipped.
                            If False, the capture loop stops.

    Returns:
        list[np.ndarray | None]: List of captured frames (None for skipped frames).
    """
    if duration <= 0 or number_of_frames <= 0:
        raise ValueError("duration and number_of_frames must be > 0")

    cap = cv.VideoCapture(camera_id)

    cap.set(cv.CAP_PROP_FOURCC, cv.VideoWriter.fourcc(*'MJPG'))
    cap.set(cv.CAP_PROP_BUFFERSIZE, 1)

    cap.set(cv.CAP_PROP_FRAME_WIDTH, width)
    cap.set(cv.CAP_PROP_FRAME_HEIGHT, height)

    # Check if the camera was opened successfully
    if not cap.isOpened():
        logger.error(f"Could not open camera {camera_id}")
        raise RuntimeError(f"Could not open camera {camera_id}")

    frames: list[np.ndarray | None] = []

    try:
        for _ in range(WARMUP_FRAMES):
            cap.read()
        time.sleep(WARMUP_DELAY)

        logger.info(f"Camera {camera_id} accessed successfully! Starting frame capture...")

        # Time interval between consecutive frame captures
        if number_of_frames > 1:
            capture_interval = duration / (number_of_frames - 1)
        else:
            capture_interval = 0

        start_time = time.perf_counter()

        # Capture the requested number of frames
        for i in range(number_of_frames):
            # Calculate when the current frame should be captured
            planned_capture_time = start_time + (i * capture_interval)
            now = time.perf_counter()

            # Wait until the scheduled capture time
            if now < planned_capture_time:
                time.sleep(planned_capture_time - now)

            ret = False
            frame = None

            # Try to read a frame with retries
            for attempt in range(max_retries):
                ret, frame = cap.read()

                # Check if the frame is valid
                if ret and frame is not None and frame.size != 0:
                    break
                logger.warning(f"Failed to capture frame {i+1}, retry {attempt+1}/{max_retries}")
                time.sleep(RETRY_DELAY)

            # If the frame could not be captured after all retries
            if not ret or frame is None or frame.size == 0:
                if skip_on_fail:
                    logger.warning(f"Frame {i+1} skipped after {max_retries} retries")
                    frames.append(None)
                    continue
                else:
                    logger.error(f"Could not read frame {i+1} even after {max_retries} retries")
                    break

            # Store the successfully captured frame
            frames.append(frame)

    finally:
        cap.release()

    valid_frames = sum(1 for f in frames if f is not None)
    logger.info(f"Captured: {valid_frames}/{number_of_frames} frames successfully")

    return frames