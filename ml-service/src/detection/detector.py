import onnxruntime as ort
import numpy as np
import logging
import cv2 as cv

from concurrent.futures import ThreadPoolExecutor
from typing import Any

from src.config.settings import FACE_DETECTOR_MODEL_PATH, MAX_WORKERS
from src.recognition.embedding import compute_face_embedding
from src.preprocessing.image.tiling import split_into_tiles
from src.preprocessing.image.letterbox import letterbox

logger = logging.getLogger(__name__)


# Load YOLO face detection model
model = ort.InferenceSession(FACE_DETECTOR_MODEL_PATH, providers=["CPUExecutionProvider"])


def _process_tile(tile: tuple[np.ndarray, int, int, int, int, int]) -> list[dict[str, Any]]:

    tile, offset_x, offset_y, frame_width, frame_height, region_offset_y = tile

    if tile is None or tile.size == 0:
        return []

    offset_y += region_offset_y

    result = letterbox(tile, (640, 640), (128, 128, 128))

    if result is None:
        return []

    tile_resized, scale, pad_x, pad_y = result

    image = cv.cvtColor(tile_resized, cv.COLOR_BGR2RGB)
    image = image.astype(np.float32) / 255.0
    image = image.transpose(2, 0, 1)[None, ...]

    detections = model.run(None, {"images": image})[0][0]

    tile_h, tile_w = tile.shape[:2]

    tile_detections = []

    margin_x, margin_y = tile_w * 0.05, tile_h * 0.05

    for detection in detections:
        x1, y1, x2, y2, confidence, _ = detection

        if confidence < 0.75:
            continue

        x1, y1 = x1 - pad_x, y1 - pad_y
        x2, y2 = x2 - pad_x, y2 - pad_y

        x1, y1 = x1 / scale, y1 / scale
        x2, y2 = x2 / scale, y2 / scale

        box_w, box_h = x2 - x1, y2 - y1

        if box_w <= 0 or box_h <= 0:
            continue

        touches_tile_border = (
            x1 <= margin_x or
            y1 <= margin_y or
            x2 >= tile_w - margin_x or
            y2 >= tile_h - margin_y
        )

        if touches_tile_border:
            continue

        p_w, p_h = int(box_w * 0.25), int(box_h * 0.25)

        local_x1 = int(max(0, x1 - p_w))
        local_y1 = int(max(0, y1 - p_h))
        local_x2 = int(min(tile_w, x2 + p_w))
        local_y2 = int(min(tile_h, y2 + p_h))

        face_crop = tile[local_y1:local_y2, local_x1:local_x2]

        if face_crop.size == 0:
            continue

        result = letterbox(face_crop, (224, 224), (0, 0, 0))

        if result is None:
            continue

        face_crop, _, _, _ = result

        global_x1, global_y1 = int(round(local_x1 + offset_x)), int(round(local_y1 + offset_y))
        global_x2, global_y2 = int(round(local_x2 + offset_x)), int(round(local_y2 + offset_y))

        global_x1, global_y1 = max(0, min(global_x1, frame_width - 1)), max(0, min(global_y1, frame_height - 1))
        global_x2, global_y2 = max(0, min(global_x2, frame_width)), max(0, min(global_y2, frame_height))

        if global_x2 <= global_x1 or global_y2 <= global_y1:
            continue

        tile_detections.append({
            "bbox": (global_x1, global_y1, global_x2, global_y2),
            "confidence": float(confidence),
            "face": face_crop,
        })

    return tile_detections


def process_faces(frames: list[np.ndarray | None]) -> list[list[dict[str, Any]]]:

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
                tiles = split_into_tiles(region, tile_size=tile_size, overlap=overlap)

                for tile, offset_x, offset_y in tiles:
                    tile_tasks.append((tile, offset_x, offset_y, w, h, region_offset_y))

        all_detections: list[dict[str, Any]] = []

        if tile_tasks:
            with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
                tile_results = executor.map(_process_tile, tile_tasks)

                for detections in tile_results:
                    all_detections.extend(detections)

        if all_detections:
            boxes = [
                [
                    d["bbox"][0],
                    d["bbox"][1],
                    d["bbox"][2] - d["bbox"][0],
                    d["bbox"][3] - d["bbox"][1]
                ]
                for d in all_detections
            ]

            confidences = [d["confidence"] for d in all_detections]

            indices = cv.dnn.NMSBoxes(boxes, confidences, 0.20, 0.40)

            if len(indices) > 0:
                indices = np.array(indices).reshape(-1)
                final_detections = [all_detections[i] for i in indices]
            else:
                final_detections = []
        else:
            final_detections = []

        faces = []

        for detection in final_detections:
            face_crop = detection["face"]

            face_data = {
                "bbox": detection["bbox"],
                "face": face_crop,
            }

            if frame_index == 1 and (embedding := compute_face_embedding(face_crop)) is not None:
                face_data["embedding"] = embedding

            faces.append(face_data)

        logger.info(f"Frame {frame_index} | {len(final_detections)} detections")

        results.append(sorted(faces, key=lambda f: (f["bbox"][1], f["bbox"][0])))

    return results