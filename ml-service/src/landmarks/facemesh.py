import mediapipe as mp
import numpy as np
import cv2 as cv
import logging

from src.preprocessing.sequence.normalization import normalize_landmark_sequence
from src.preprocessing.sequence.imputation import impute_missing_frames

logger = logging.getLogger(__name__)


mp_face_mesh = mp.solutions.face_mesh.FaceMesh(
    static_image_mode=True,
    max_num_faces=1,
    refine_landmarks=True,
)


def _extract_landmarks(face: np.ndarray) -> np.ndarray | None:
    """
    Extract 3D facial landmarks from a face crop using MediaPipe FaceMesh.
    """
    if face is None or face.size == 0:
        return None

    face = cv.cvtColor(face, cv.COLOR_BGR2RGB)
    results = mp_face_mesh.process(face)

    if not results.multi_face_landmarks:
        return None

    landmarks = results.multi_face_landmarks[0].landmark

    landmarks = np.array([
        [lm.x, lm.y, lm.z] for lm in landmarks
    ], dtype=np.float32)

    return landmarks


def process_landmark_sequence(identities: list[dict]) -> dict[int, np.ndarray]:
    """
    Generate normalized landmark sequence for each tracked student.
    """
    student_landmarks = {}
    student_success_frames = {}

    for person in identities:
        student_id = person["id"]
        faces = person["faces"]

        sequence = []

        for face in faces:
            if face is None:
                sequence.append(np.zeros((478, 3), dtype=np.float32))
                continue

            landmarks = _extract_landmarks(face)

            if landmarks is None:
                sequence.append(np.zeros((478, 3), dtype=np.float32))
            else:
                sequence.append(landmarks)

                student_success_frames[student_id] = (student_success_frames.get(student_id, 0) + 1)

        if len(sequence) < 20:
            padding = [
                np.zeros((478, 3), dtype=np.float32)
            ] * (20 - len(sequence))

            sequence = padding + sequence

        elif len(sequence) > 20:
            sequence = sequence[-20:]

        sequence = np.array(sequence, dtype=np.float32)

        sequence = impute_missing_frames(sequence)

        sequence = normalize_landmark_sequence(sequence)

        student_landmarks[student_id] = np.array(sequence, dtype=np.float32)

    logger.info("=== Landmark Extraction Summary ===")

    total_students = len(identities)
    students_with_landmarks = len(student_success_frames)

    logger.info(f"Total students: {total_students}")
    logger.info(
        f"Students with landmarks: "
        f"{students_with_landmarks}/{total_students}"
    )

    return student_landmarks
