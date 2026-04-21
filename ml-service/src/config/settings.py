import os

from pathlib import Path
from typing import Dict

# ==== FRAME CAPTURE CONFIGURATION ====
CAMERA_ID: int = 0
NUMBER_OF_FRAMES: int = 20
CAPTURE_DURATION: float = 10.0


# ==== PATHS ====
CURRENT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = CURRENT_DIR.parents[1]

ASSETS_DIR = PROJECT_ROOT / "assets"
MODELS_DIR = ASSETS_DIR / "models"

# ==== MODEL PATHS ====
FACE_DETECTOR_MODEL_PATH = MODELS_DIR / "detection" / "face-detector-yolo-v12n.onnx"
ENGAGEMENT_MODEL_PATH = MODELS_DIR / "engagement" / "attention-level-classifier.h5"


# ==== THREADING CONFIG ====
MAX_WORKER_THREADS: int = max(1, (os.cpu_count() or 5) - 1)


# ==== LABEL MAP ====
ENGAGEMENT_LABEL_MAP: Dict[int, str] = {
    0: "low",
    1: "medium",
    2: "high"
}