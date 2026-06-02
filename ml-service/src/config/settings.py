import os

from pathlib import Path
from typing import Dict
from dotenv import load_dotenv


# ==== LOAD ENVIRONMENT VARIABLES ====
load_dotenv()


# ==== FRAME CAPTURE CONFIGURATION ====
CAMERA_ID: int = 0
FRAME_COUNT: int = 20
CAPTURE_DURATION: float = 10.0


# ==== SNAPSHOT TIMING CONFIGURATION (in seconds) ====
PRE_START_DELAY: int = 30
SNAPSHOT_INTERVAL: int = 30
PRE_END_DELAY: int = 30


# ==== DEVICE CONFIGURATION ====
CLASSROOM_ID: str = os.getenv("CLASSROOM") or "default-classroom"


# ==== PATHS ====
CURRENT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = CURRENT_DIR.parents[1]

ASSETS_DIR = PROJECT_ROOT / "assets"
MODELS_DIR = ASSETS_DIR / "models"

# ==== MODEL PATHS ====
FACE_DETECTOR_MODEL_PATH = MODELS_DIR / "detection" / "face_detector.onnx"
ENGAGEMENT_MODEL_PATH = MODELS_DIR / "engagement" / "engagement_classifier.h5"
EMBEDDER_MODEL_PATH = MODELS_DIR / "recognition" / "face_embedder.onnx"


# ==== THREADING CONFIG ====
MAX_WORKERS: int = max(1, (os.cpu_count() or 5) - 1)


# ==== LABEL MAP ====
ENGAGEMENT_LABEL_MAP: Dict[int, str] = {
    0: "low",
    1: "medium",
    2: "high"
}