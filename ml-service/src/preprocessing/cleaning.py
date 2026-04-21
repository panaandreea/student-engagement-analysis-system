import numpy as np

def fill_missing_frames(sequence: np.ndarray) -> np.ndarray:
    """
    Fill missing frames using forward and backward propagation.

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

    # Beginning
    for i in range(len(sequence)):
        if not is_missing(sequence[i]):
            sequence[:i] = sequence[i]
            break

    # Forward fill
    for i in range(1, len(sequence)):
        if is_missing(sequence[i]):
            sequence[i] = sequence[i - 1]

    # Backward fill
    for i in range(len(sequence) - 2, -1, -1):
        if is_missing(sequence[i]):
            sequence[i] = sequence[i + 1]

    return sequence