from datetime import datetime
import time

from src.camera.utils import get_available_cameras
from src.camera.capture import capture_frames
from src.config.settings import CAMERA_ID, NUMBER_OF_FRAMES, CAPTURE_DURATION, NUMBER_OF_SESSIONS, FRAMES_DIR, DURATION_BETWEEN_SESSIONS, LANDMARKS_DIR
from src.visualization.frames import display_frames
from src.visualization.faces import display_faces
from src.visualization.bboxes import draw_bboxes
from src.detection.face_detector import detect_and_process_faces
from src.tracking.associate_faces import associate_faces
from src.landmarks.extract_landmarks import export_facemesh


# Get available cameras
print("Available cameras: ", get_available_cameras())

for session_index in range(1, NUMBER_OF_SESSIONS + 1):
    print(f"Starting session: {session_index}/{NUMBER_OF_SESSIONS}")

    run_timestamp = datetime.now().strftime("%Y.%m.%d-%H.%M.%S.%f")
    run_dir = LANDMARKS_DIR / run_timestamp
    run_dir.mkdir(parents=True, exist_ok=True)

    frames = capture_frames(CAMERA_ID, NUMBER_OF_FRAMES, CAPTURE_DURATION)
    display_frames(frames)

    faces = detect_and_process_faces(frames)
    display_faces(associate_faces(faces))

    # Draw anonymized bounding boxes for reference (only first frame only)
    if session_index == 1 and frames and faces:
        draw_bboxes(frames[0], faces[0], FRAMES_DIR)
    # Associate faces across frames
    student_queues = associate_faces(faces)
    # Extract and export landmarks
    export_facemesh(student_queues, run_dir)
    # Pause between sessions
    if session_index < NUMBER_OF_SESSIONS:
        time.sleep(DURATION_BETWEEN_SESSIONS)
