import cv2 as cv

def display_frames(frames):
    """
    Display a sequence of frames for visualization and debugging.

    Args:
        frames (list): List of frames to display.
    """
    if not frames:
        print("No frames to display.")
        return

    print("Displaying frames...")
    print("Press ENTER for next frame or 'q' to quit.")

    total_frames = len(frames)

    window_name = "Frames"
    cv.namedWindow(window_name, cv.WINDOW_NORMAL)

    for index, frame in enumerate(frames, start=1):
        # Skip invalid or missing frames
        if frame is None or frame.size == 0:
            print(f"Frame {index}/{total_frames} is invalid.")
            continue

        display_image = frame.copy()

        cv.putText(display_image,
                   f"Frame {index}/{total_frames}",
                   (10, 25),
                   cv.FONT_HERSHEY_SIMPLEX,
                   0.5,
                   (255, 255, 255),
                   1,
                   cv.LINE_AA)

        cv.imshow(window_name, display_image)
        h, w = display_image.shape[:2]
        cv.resizeWindow(window_name, w, h)

        while True:
            key = cv.waitKey(0)

            # Enter key
            if key in (13, 10):
                break

            # Quit
            if key == ord('q'):
                cv.destroyAllWindows()
                return

    cv.destroyAllWindows()
