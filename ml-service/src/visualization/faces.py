import cv2 as cv
import logging

logger = logging.getLogger(__name__)


def display_faces(identities: list[dict]) -> None:
    """
    Display tracked face crops grouped by student identity.
    """
    if not identities:
        logger.info("No tracked identities found.")
        return

    logger.info("Displaying tracked faces...")
    logger.info("Press ENTER for next face or 'q' to quit.")

    window_name = "Tracked Faces"

    cv.namedWindow(window_name, cv.WINDOW_NORMAL)

    for person in identities:
        student_id = person["id"]
        faces = person["faces"]

        logger.info(f"Displaying faces for student {student_id}")

        for frame_index, face in enumerate(faces, start=1):
            if face is None or face.size == 0:
                continue

            display_image = face.copy()

            cv.putText(
                display_image,
                f"Student {student_id} | Frame {frame_index}",
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
                key = cv.waitKey(0)

                # Enter key
                if key in (13, 10):
                    break

                # Quit
                if key == ord('q'):
                    cv.destroyAllWindows()
                    return

    cv.destroyAllWindows()