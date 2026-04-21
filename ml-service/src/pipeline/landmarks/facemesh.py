import mediapipe as mp
import numpy as np
import cv2 as cv
import logging

logger = logging.getLogger(__name__)

NUM_LANDMARKS = 478

mp_face_mesh = mp.solutions.face_mesh.FaceMesh(
    static_image_mode=True,
    max_num_faces=1,
    refine_landmarks=True,
)

def extract_landmarks(face):
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


def process_landmark_sequence(student_queues):
    student_landmarks = {}
    student_success_frames = {}

    for student_id, frames in student_queues.items():
        sequence = []

        for face_dict in frames:
            if face_dict is None:
                sequence.append(np.zeros((NUM_LANDMARKS, 3), dtype=np.float32))
                continue

            face = face_dict.get("face")

            if face is None:
                sequence.append(np.zeros((NUM_LANDMARKS, 3), dtype=np.float32))
                continue

            landmarks = extract_landmarks(face)

            face_dict["face"] = None

            if landmarks is None:
                sequence.append(np.zeros((NUM_LANDMARKS, 3), dtype=np.float32))
            else:
                sequence.append(landmarks)
                student_success_frames[student_id] = student_success_frames.get(student_id, 0) + 1

        student_landmarks[student_id] = np.array(sequence, dtype=np.float32)

    logger.info("=== Landmark Extraction Summary ===")

    total_students = len(student_queues)
    students_with_landmarks = len(student_success_frames)

    logger.info(f"Total students: {total_students}")
    logger.info(f"Students with landmarks: {students_with_landmarks}/{total_students}")

    return student_landmarks