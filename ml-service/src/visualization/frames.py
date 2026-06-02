import numpy as np
import cv2 as cv
import logging

logger = logging.getLogger(__name__)


def display_frames(frames: list[np.ndarray | None]) -> None:
    """
    Display a sequence of frames for visualization and debugging.
    """
    if not frames:
        logger.info("No frames to display.")
        return

    logger.info("Displaying frames...")
    logger.info("Press ENTER for next frame or 'q' to quit.")

    total_frames = len(frames)

    window_name = "Frames"

    cv.namedWindow(window_name, cv.WINDOW_NORMAL)

    for index, frame in enumerate(frames, start=1):
        if frame is None or frame.size == 0:
            logger.info(f"Frame {index}/{total_frames} is invalid.")
            continue

        display_image = frame.copy()

        cv.putText(
            display_image,
            f"Frame {index}/{total_frames}",
            (10, 25),
            cv.FONT_HERSHEY_SIMPLEX,
            0.5,
            (255, 255, 255),
            1,
            cv.LINE_AA
        )

        cv.imshow(window_name, display_image)
        h, w = display_image.shape[:2]
        cv.resizeWindow(window_name, w, h)

        while True:
            # Enter key
            key = cv.waitKey(0)
            if key in (13, 10):
                break

            # Quit
            if key == ord('q'):
                cv.destroyAllWindows()
                return

    cv.destroyAllWindows()