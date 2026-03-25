import cv2 as cv

def display_faces(students):
    """
    Display face crops grouped by student id.

    Args:
        students (dict[int, list[dict]]): Faces grouped by student id.
    """
    if not students:
        print("No students found.")
        return

    print("Displaying faces...")
    print("Press ENTER for next face or 'q' to quit.")

    window_name = "Students"
    cv.namedWindow(window_name, cv.WINDOW_NORMAL)

    for student_id, faces in students.items():
        print(f"Displaying faces for student {student_id + 1}")

        for frame_index, face_dict in enumerate(faces, start=1):
            if not face_dict:
                continue

            face = face_dict.get("face")

            if face is None or face.size == 0:
                continue

            display_image = face.copy()

            cv.putText(display_image,
                       f"Student {student_id + 1} | Frame {frame_index}",
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
