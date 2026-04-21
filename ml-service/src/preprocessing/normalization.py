import numpy as np

from src.preprocessing.cleaning import fill_missing_frames

def _normalize_frame_landmarks(landmarks: np.ndarray) -> np.ndarray:
    """
    Normalize FaceMesh landmarks for a single frame.

    - Removes translation.
    - Removes scale.
    - Preserves head pose.

    Args:
        landmarks: (N, 3) array.

    Returns:
        (N, 3) normalized landmarks.
    """
    landmarks = np.asarray(landmarks, dtype = np.float32)

    center = landmarks[4]
    centered = landmarks - center

    scale = np.max(np.linalg.norm(centered, axis = 1))
    scale = max(scale, 1e-6)

    return centered / scale


def _normalize_sequence_landmarks(sequence: np.ndarray) -> np.ndarray:
    """
    Normalize a sequence of FaceMesh frames.

    Args:
        sequence: (T, N, 3) array of face landmarks.

    Returns:
        (T, N * 3) normalized sequence.
    """

    sequence = np.asarray([
        _normalize_frame_landmarks(frame) for frame in sequence
    ], dtype = np.float32)

    return sequence.reshape(sequence.shape[0], -1)


def normalize_student_landmarks(student_landmarks: dict[int, np.ndarray]) -> dict[int, np.ndarray]:
    """
    Normalize all student landmarks..

    Args:
        student_landmarks: student_id -> (T, N, 3).

    Returns:
        student_id -> (T, N * 3)
    """
    normalized = {}

    for student_id, sequence in student_landmarks.items():
        if sequence is None or len(sequence) == 0:
            continue

        sequence = fill_missing_frames(sequence)

        normalized_sequence = _normalize_sequence_landmarks(sequence)

        normalized[student_id] = normalized_sequence.astype(np.float32)

    return normalized