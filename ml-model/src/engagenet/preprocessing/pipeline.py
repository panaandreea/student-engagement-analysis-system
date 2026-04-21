import numpy as np

from engagenet.dataset.loader import load_landmarks
from shared.preprocessing.cleaning import fill_missing_frames
from shared.preprocessing.normalization import normalize_landmark_sequence

def preprocess_sequence(file_path: str) -> np.ndarray:
    """
    Full preprocessing pipeline: JSON -> landmarks -> cleaning -> normalization.

    Args:
        file_path: (str) path to file.

    Returns:
        (T, 1434) array of normalized landmarks.
    """
    landmarks_sequence = load_landmarks(file_path)

    landmarks_sequence = fill_missing_frames(landmarks_sequence)

    return normalize_landmark_sequence(landmarks_sequence)