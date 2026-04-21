import numpy as np
import json

def load_landmarks(file_path: str) -> np.ndarray:
    """
    Load landmarks from a JSON file.

    Args:
        file_path (str): Path to JSON file.

    Returns:
        (T, 478, 3) array of landmarks.
    """
    with open(file_path, 'r') as f:
        data = json.load(f)

    return np.asarray(data['landmarks'], dtype=np.float32)