import cv2 as cv
import time

def capture_frames(camera_id, number_of_frames, duration, max_retries=3, skip_on_fail=True):
    """
    Capture a sequence of frames from a camera device using time-based sampling.

    Args:
        camera_id (int): Camera ID.
        number_of_frames (int): Number of frames to capture.
        duration (int): Total capture duration in seconds.
        max_retries (int): Maximum number of retries.
        skip_on_fail (bool): If True, failed frame captures are skipped.
                            If False, the capture loop stops.

    Returns:
        list: List of captured frames (None for skipped frames).
    """
    # Validate input parameters
    if duration <= 0 or number_of_frames <= 0:
        print("duration and number_of_frames must be greater than 0")
        return []

    # Open the camera device
    cap = cv.VideoCapture(camera_id, cv.CAP_DSHOW)
    cap.set(cv.CAP_PROP_BUFFERSIZE, 1)

    # Check if the camera was opened successfully
    if not cap.isOpened():
        print(f"Could not open the camera with ID: {camera_id}.")
        cap.release()
        return []
    else:
        for _ in range(10):
            cap.read()
        time.sleep(0.5)

        print("Camera accessed successfully! Starting frame capture...")

    frames = []

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
            print(f"Failed to capture frame {i+1}, retry {attempt+1}/{max_retries}.")
            time.sleep(0.05)

        # If the frame could not be captured after all retries
        if not ret or frame is None or frame.size == 0:
            if skip_on_fail:
                print(f"Frame {i+1} skipped after {max_retries} retries.")
                frames.append(None)
                continue
            else:
                print(f"Could not read frame {i+1} even after {max_retries} retries.")
                break

        # Store the successfully captured frame
        frames.append(frame)

    # Release the camera resource
    cap.release()

    valid_frames = sum(1 for f in frames if f is not None)
    print(f"Captured: {valid_frames}/{number_of_frames} frames successfully.")
    return frames
