from pathlib import Path

# ==== CAMERA DISCOVERY ====
# Maximum camera device index checked in find_available_cameras()
MAX_CAMERAS_CHECKED = 3


# ==== FRAME CAPTURE CONFIGURATION ====
# Camera configuration
CAMERA_ID = 0
# Number of frames to capture
NUMBER_OF_FRAMES = 5
# Duration (in seconds) over which frames will be captured
CAPTURE_DURATION = 10


# ==== PROJECT PATHS ====
CURRENT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = CURRENT_DIR.parents[1]

ASSETS_DIR = PROJECT_ROOT / "assets"
MODELS_DIR = ASSETS_DIR / "models"

YOLO_PATH = MODELS_DIR / "detection" / "yolov12n-face.onnx"
MOBILE_FACE_NET_PATH = MODELS_DIR / "recognition" / "MobileFaceNet.onnx"


# Number of worker threads used for parallel face processing
MAX_WORKER_THREADS = 4


# ==== SESSION CONFIGURATION ====
# Number of recording sessions
NUMBER_OF_SESSIONS = 1
# Pause duration between sessions
DURATION_BETWEEN_SESSIONS = 30


# ==== OUTPUT DIRECTORIES ====
# Directory where frames with drawn bounding boxes are saved
# FRAMES_DIR = Path(r"E:\Data\Frames")
# Directory where extracted facial landmarks are stored
# LANDMARKS_DIR = Path(r"E:\Data\Landmarks")


DATA_DIR = PROJECT_ROOT / "data"

FRAMES_DIR = DATA_DIR / "frames"
LANDMARKS_DIR = DATA_DIR / "landmarks"



