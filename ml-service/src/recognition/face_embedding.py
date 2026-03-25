from src.config.settings import MOBILE_FACE_NET_PATH
from src.utils.image import resize_with_padding

import onnxruntime as ort
import numpy as np
import cv2 as cv

# Load MobileFaceNet model once
model = ort.InferenceSession(MOBILE_FACE_NET_PATH, providers=["CPUExecutionProvider"])

def get_face_embedding(face):
    """
    Compute a 128D face embedding  using MobileFaceNet.

    Args:
        face (np.ndarray): BGR face crop image.

    Returns:
        np.ndarray | None: L2-normalized 128D face embedding vector or None if input is invalid.
    """
    if face is None or face.size == 0:
        print("Invalid face image (None or empty)!")
        return None

    if len(face.shape) != 3 or face.shape[2] != 3:
        print("Input face must be a 3-channel BGR image!")
        return None

    # Preprocess face for MobileFaceNet recognition
    face = resize_with_padding(face, (112, 112), pad_color=(128, 128, 128))

    if face is None:
        return None

    face = cv.cvtColor(face, cv.COLOR_BGR2RGB)
    face = face.astype(np.float32)
    face = (face - 127.5) / 128.0   # normalization for MobileFaceNet
    face = face.transpose(2, 0, 1)[None, ...]

    outputs = model.run(['output0'], {'input0': face})
    embedding = outputs[0].flatten()

    # L2-normalize embedding for cosine similarity
    embedding = embedding / (np.linalg.norm(embedding) + np.float32(1e-12))

    return embedding
