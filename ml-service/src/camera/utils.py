from  src.config.settings import MAX_CAMERAS_CHECKED
import cv2 as cv

def get_available_cameras():
    """
    Scans camera device indices in the range [0, MAX_CAMERAS_CHECKED)
    and returns indices of devices that can be successfully opened and
    capture at least one valid video frame.

    Returns:
        list[int]: List of available camera indices.
    """
    available_cameras = []

    for camera_index in range(MAX_CAMERAS_CHECKED):
        cap = cv.VideoCapture(camera_index)

        # Skip device if it cannot be opened
        if not cap.isOpened():
            cap.release()
            continue

        # Attempt to read a frame to validate the device
        ret, frame = cap.read()
        cap.release()

        # Accept camera only if a valid frame is returned
        if ret and frame is not None and frame.size != 0:
            available_cameras.append(camera_index)

    return available_cameras
