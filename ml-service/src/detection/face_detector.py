from concurrent.futures import ThreadPoolExecutor

import onnxruntime as ort
import numpy as np
import cv2 as cv

from src.recognition.face_embedding import get_face_embedding
from src.config.settings import MAX_WORKER_THREADS
from src.utils.image import resize_with_padding
from src.config.settings import YOLO_PATH


# Load YOLO face detection model once
model = ort.InferenceSession(YOLO_PATH, providers=["CPUExecutionProvider"])


def build_face_representation(face_data):
    """
    Build face representation (embedding + bbox) from a face crop.
    """
    face_image, bbox = face_data

    embedding = get_face_embedding(face_image)

    return {
        "bbox": bbox,
        "face": face_image,
        "embedding": embedding
    }


def detect_and_process_faces(frames):
    """
    Run face detection and embedding extraction on a list of frames.

    Args:
        frames (list[np.array]): list of BGR frames.

    Returns:
        list[list[dict]]:
            Outer list -> corresponds to input frames.
            Inner list -> contains detected faces for that frame, each with:
                - bbox (tuple): (x1, y1, x2, y2).
                - face (np.array): cropped face image.
                - embedding (np.array): face embedding vector.
    """
    if not frames:
        return []

    original_height, original_width = frames[0].shape[:2]

    # Compute scale and padding for letterbox resizing
    scale = min(640 / original_width, 640 / original_height)
    new_width, new_height = int(original_width * scale), int(original_height * scale)
    pad_x = (640 - new_width) // 2
    pad_y = (640 - new_height) // 2

    results = []

    for frame_index, frame in enumerate(frames, start=1):
        if frame is None or frame.size == 0:
            print(f"Frame {frame_index}: skipped (invalid frame).")
            results.append([])
            continue

        faces_detected = 0
        embeddings_extracted = 0

        # Preprocess frame for YOLO
        resized_frame = resize_with_padding(frame, (640, 640), (128, 128, 128))
        resized_frame = cv.cvtColor(resized_frame, cv.COLOR_BGR2RGB)
        resized_frame = resized_frame.astype(np.float32) / 255.0
        resized_frame = resized_frame.transpose(2, 0, 1)[None, ...]

        output = model.run(None, {"images": resized_frame})
        detections = output[0][0]

        face_data_list = []

        for detection in detections:
            x1, y1, x2, y2, confidence, _ = detection

            # Skip low confidence detections
            if confidence < 0.2:
                continue

            faces_detected += 1

            # Rescale bounding box back to original image size
            x1 = x1 - pad_x
            y1 = y1 - pad_y
            x2 = x2 - pad_x
            y2 = y2 - pad_y

            x1 = int(x1 / scale)
            y1 = int(y1 / scale)
            x2 = int(x2 / scale)
            y2 = int(y2 / scale)

            width = x2 - x1
            height = y2 - y1

            p_w = int(width * 0.1)
            p_h = int(height * 0.1)

            x1 = max(0, x1 - p_w)
            y1 = max(0, y1 - p_h)
            x2 = min(original_width, x2 + p_w)
            y2 = min(original_height, y2 + p_h)

            # Validate bounding box after padding
            if x2 <= x1 or y2 <= y1:
                continue

            # Crop face region for embedding extraction
            face = frame[y1:y2, x1:x2]

            if face.size == 0:
                continue

            bbox = (x1, y1, x2, y2)

            face_data_list.append((face, bbox))

        if not face_data_list:
            results.append([])
            continue

        with ThreadPoolExecutor(max_workers=MAX_WORKER_THREADS) as executor:
            processed_faces = list(executor.map(build_face_representation, face_data_list))

        for f in processed_faces:
            if f["embedding"] is not None:
                embeddings_extracted += 1

        print(
            f"Frame {frame_index}: "
            f"{faces_detected} faces detected, "
            f"{embeddings_extracted} embeddings extracted"
        )

        results.append(sorted(processed_faces, key=lambda ff: ff["bbox"][0]))

    return results
