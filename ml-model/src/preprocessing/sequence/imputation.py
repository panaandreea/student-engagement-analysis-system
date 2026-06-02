import numpy as np


def impute_missing_frames(sequence: np.ndarray) -> np.ndarray:
    """
    Fill missing frames (all-zero or NaN) using temporal forward propagation.

    Args:
        sequence: (T, N, 3) array of landmarks.

    Returns:
        (T, N, 3) filled sequence.
    """

    sequence = np.asarray(sequence, dtype=np.float32).copy()

    def is_missing(frame: np.ndarray) -> bool:
        return np.all(frame == 0.0) or np.isnan(frame).any()

    if all(is_missing(frame) for frame in sequence):
        return sequence

    # Fill missing frames at the beginning
    for i in range(len(sequence)):
        if not is_missing(sequence[i]):
            sequence[:i] = sequence[i]
            break

    # Forward propagation
    for i in range(1, len(sequence)):
        if is_missing(sequence[i]):
            sequence[i] = sequence[i - 1]

    return sequence