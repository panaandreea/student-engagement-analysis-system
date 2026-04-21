import onnxruntime as ort
import numpy as np
import logging
import cv2 as cv

from concurrent.futures import ThreadPoolExecutor
from src.config.settings import FACE_DETECTOR_MODEL_PATH, MAX_WORKER_THREADS
from pipeline.detection.tiling import generate_tiles
from pipeline.detection.letterbox import resize_with_padding

logger = logging.getLogger(__name__)

# Load YOLO face detection model
model = ort.InferenceSession(FACE_DETECTOR_MODEL_PATH, providers=["CPUExecutionProvider"])

INPUT_SIZE = 640
CONF_THRESHOLD = 0.4
NMS_SCORE_THRESHOLD = 0.2
NMS_IOU_THRESHOLD = 0.40
BORDER_MARGIN_RATIO = 0.05

def _process_tile_detections(
        task: tuple[np.ndarray, int, int, int, int, int]
) -> list[tuple[int, int, int, int, float]]:
    """
    Process a single tile and return face detections mapped back to the original frame coordinates.

    Args:
        task: Tuple containing:
            - tile (np.ndarray)
            - offset_x (int)
            - offset_y (int)
            - frame_width (int)
            - frame_height (int)
            - region_offset_y (int)

    Returns:
        list[tuple[int, int, int, int, float]]: Detections as (x1, y1, x2, y2, confidence).
    """
    tile, offset_x, offset_y, frame_width, frame_height, region_offset_y = task

    if tile is None or tile.size == 0:
        return []

    offset_y += region_offset_y

    result = resize_with_padding(tile, (INPUT_SIZE, INPUT_SIZE), (128, 128, 128))
    if result is None:
        return []

    tile_resized, scale, pad_x, pad_y = result

    image = cv.cvtColor(tile_resized, cv.COLOR_BGR2RGB)
    image = image.astype(np.float32) / 255.0
    image = image.transpose(2, 0, 1)[None, ...]

    output = model.run(None, {"images": image})
    detections = output[0][0]

    tile_h, tile_w = tile.shape[:2]

    tile_detections = []

    # Filter detections too close to tile borders to reduce half-faces cut by tiling boundaries
    margin_x, margin_y = tile_w * BORDER_MARGIN_RATIO, tile_h * BORDER_MARGIN_RATIO

    for detection in detections:
        x1, y1, x2, y2, confidence, _ = detection

        if confidence < CONF_THRESHOLD:
            continue

        # Remove resize padding
        x1, y1 = x1 - pad_x, y1 - pad_y
        x2, y2 = x2 - pad_x, y2 - pad_y

        # Map back to tile coordinates
        x1, y1 = x1 / scale, y1 / scale
        x2, y2 = x2 / scale, y2 / scale

        box_w, box_h = x2 - x1, y2 - y1

        if box_w <= 0 or box_h <= 0:
            continue

        touches_tile_border = (x1 <= margin_x or y1 <= margin_y or x2 >= tile_w - margin_x or y2 >= tile_h - margin_y)

        if touches_tile_border:
            continue

        # Map to global frame coordinates
        x1, y1 = int(round(x1 + offset_x)), int(round(y1 + offset_y))
        x2, y2 = int(round(x2 + offset_x)), int(round(y2 + offset_y))

        # Clamp to frame
        x1, y1 = max(0, min(x1, frame_width - 1)), max(0, min(y1, frame_height - 1))
        x2, y2 = max(0, min(x2, frame_width)), max(0, min(y2, frame_height))

        if x2 <= x1 or y2 <= y1:
            continue

        tile_detections.append((x1, y1, x2, y2, float(confidence)))

    return tile_detections


FACE_SIZE = (224, 224)
MARGIN = 0.25

def detect_and_process_faces(
        frames: list[np.ndarray | None]
) -> list[list[dict]]:
    """
    Detect faces using adaptive multiscale tiling for improved small-face detection.

    Strategy:
        - Top region -> smaller tiles (zoom for distant faces)
        - Bottom region -> larger tiles (faces already big)

    Args:
        frames (list[np.ndarray | None]): Input frames.

    Returns:
        list[list[dict]]: Per-frame list of detected faces, where each face is a dict
        containing:
            - "bbox": (x1, y1, x2, y2)
            - "face": cropped face image
    """
    if not frames:
        return []

    results = []

    for frame_index, frame in enumerate(frames, start=1):
        if frame is None or frame.size == 0:
            results.append([])
            continue

        h, w = frame.shape[:2]

        regions = [
            (frame[0:int(h * 0.45), :], 0),
            (frame[int(h * 0.35):, :], int(h * 0.35)),
        ]

        tile_tasks = []

        for region, region_offset_y in regions:
            if region is None or region.size == 0:
                continue

            if region_offset_y == 0:
                tile_configs = [(320, 0.4)]
            else:
                tile_configs = [(640, 0.3)]

            for tile_size, overlap in tile_configs:
                tiles = generate_tiles(region, tile_size=tile_size, overlap=overlap)

                for tile, offset_x, offset_y in tiles:
                    tile_tasks.append((tile, offset_x, offset_y, w, h, region_offset_y))

        all_detections = []

        if tile_tasks:
            with ThreadPoolExecutor(max_workers=MAX_WORKER_THREADS) as executor:
                tile_results = executor.map(_process_tile_detections, tile_tasks)

                for detections in tile_results:
                    all_detections.extend(detections)

        # GLOBAL NMS
        if all_detections:
            boxes = [
                [d[0], d[1], d[2] - d[0], d[3] - d[1]]
                for d in all_detections
            ]
            confidences = [float(d[4]) for d in all_detections]

            indices = cv.dnn.NMSBoxes(boxes, confidences, NMS_SCORE_THRESHOLD, NMS_IOU_THRESHOLD)

            if len(indices) > 0:
                indices = np.array(indices).reshape(-1)
                final_detections = [all_detections[i] for i in indices]
            else:
                final_detections = []
        else:
            final_detections = []

        faces = []

        for x1, y1, x2, y2, confidence in final_detections:
            if x2 <= x1 or y2 <= y1:
                continue

            width = x2 - x1
            height = y2 - y1

            p_w, p_h = int(width * MARGIN), int(height * MARGIN)

            x1, y1 = max(0, x1 - p_w), max(0, y1 - p_h)
            x2, y2 = min(w, x2 + p_w), min(h, y2 + p_h)

            face_crop = frame[y1:y2, x1:x2]

            if face_crop.size == 0:
                continue

            result = resize_with_padding(face_crop, FACE_SIZE, (0, 0, 0))
            if result is None:
                continue

            face_crop, _, _, _ = result

            faces.append({
                "bbox": (x1, y1, x2, y2),
                "face": face_crop,
                "confidence": confidence,
            })

        logger.info(f"Frame {frame_index} | {len(final_detections)} detections")

        results.append(sorted(faces, key=lambda f: (f["bbox"][1], f["bbox"][0])))

    return results