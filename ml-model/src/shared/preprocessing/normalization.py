import numpy as np

def _normalize_landmarks(landmarks: np.ndarray) -> np.ndarray:
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


def normalize_landmark_sequence(sequence: np.ndarray) -> np.ndarray:
    """
    Normalize a sequence of FaceMesh frames.

    Args:
        sequence: (T, N, 3) array of face landmarks.

    Returns:
        (T, N, 3) normalized sequence.
    """

    sequence = np.asarray([
        _normalize_landmarks(frame) for frame in sequence
    ], dtype = np.float32)

    return sequence.reshape(sequence.shape[0], -1)