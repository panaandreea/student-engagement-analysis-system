import onnxruntime as ort
import numpy as np
import cv2 as cv

from src.preprocessing.image.letterbox import letterbox
from src.config.settings import EMBEDDER_MODEL_PATH


# Load MobileFaceNet model
model = ort.InferenceSession(EMBEDDER_MODEL_PATH, providers=["CPUExecutionProvider"])


def compute_face_embedding(face: np.ndarray) -> np.ndarray | None:
    """
    Compute a 128D face embedding using MobileFaceNet.

    Args:
        face (np.ndarray): BGR face crop image.

    Returns:
        np.ndarray | None: L2-normalized 128D face embedding vector or None if input is invalid.
    """

    if face is None or face.size == 0 or len(face.shape) != 3 or face.shape[2] != 3:
        return None

    result = letterbox(face, (112, 112), pad_color=(128, 128, 128))

    if result is None:
        return None

    face, _, _, _ = result

    face = cv.cvtColor(face, cv.COLOR_BGR2RGB)
    face = face.astype(np.float32)
    face = (face - 127.5) / 128.0
    face = face.transpose(2, 0, 1)[None, ...]

    embedding = model.run(['output0'], {'input0': face})[0].flatten()

    embedding = embedding / (np.linalg.norm(embedding) + np.float32(1e-12))

    return embedding