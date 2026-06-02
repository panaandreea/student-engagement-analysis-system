import time
import logging

from datetime import datetime, timezone, timedelta

from src.camera.resolution import get_max_camera_resolution
from src.camera.capture import capture_frames
from src.detection.detector import process_faces
from src.tracking.tracker import track_faces_in_snapshot, track_faces_between_snapshots
from src.landmarks.facemesh import process_landmark_sequence
from src.database.snapshots import create_session_snapshot, save_snapshot_heatmap
from src.database.sessions import get_active_sessions
from src.database.states import insert_states
from src.config.settings import (PRE_START_DELAY, SNAPSHOT_INTERVAL, PRE_END_DELAY, CLASSROOM_ID)
from src.models.model import build_model, predict_states
from src.visualization.frames import display_frames
from src.visualization.faces import display_faces
from src.visualization.heatmap import generate_snapshot_heatmap
from src.storage.heatmaps import upload_snapshot_heatmap

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


WIDTH, HEIGHT = get_max_camera_resolution()
logger.info(f"Camera resolution: {WIDTH} x {HEIGHT}")


session_trackers = {}
model = build_model()


while True:
    active_sessions = get_active_sessions(CLASSROOM_ID)

    if not active_sessions:
        time.sleep(5)
        continue

    session = active_sessions[0]
    session_id = session["id"]

    start_time = datetime.fromisoformat(session["start_time"])
    end_time = datetime.fromisoformat(session["end_time"])

    if start_time.tzinfo is None:
        start_time = start_time.replace(tzinfo=timezone.utc)
    else:
        start_time = start_time.astimezone(timezone.utc)

    if end_time.tzinfo is None:
        end_time = end_time.replace(tzinfo=timezone.utc)
    else:
        end_time = end_time.astimezone(timezone.utc)

    now = datetime.now(timezone.utc)

    if now < start_time + timedelta(seconds=PRE_START_DELAY):
        sleep_seconds = (start_time + timedelta(seconds=PRE_START_DELAY) - now).total_seconds()
        logger.info(f"Waiting before starting snapshots: {sleep_seconds:.2f}s")
        time.sleep(max(0.0, sleep_seconds))
        continue

    if now >= end_time - timedelta(seconds=PRE_END_DELAY):
        logger.info("Session near end, skipping capture")
        time.sleep(30)
        continue

    if session_id not in session_trackers:
        session_trackers[session_id] = {
            "global_identities": [],
            "next_id": 1,
        }

    tracker = session_trackers[session_id]

    frames = capture_frames(WIDTH, HEIGHT)

    # ====== DISPLAY FRAMES ======
    display_frames(frames)

    if not frames or all(f is None for f in frames):
        time.sleep(SNAPSHOT_INTERVAL)
        continue

    faces = process_faces(frames)

    tracks = track_faces_in_snapshot(faces)

    global_identities, next_id = track_faces_between_snapshots(
        tracker["global_identities"],
        tracks,
        tracker["next_id"]
    )

    tracker["global_identities"] = global_identities
    tracker["next_id"] = next_id

    # ====== DISPLAY FACES ======
    display_faces(global_identities)

    normalized_landmarks = process_landmark_sequence(global_identities)

    snapshot_id, _ = create_session_snapshot(session_id)

    if snapshot_id is None:
        time.sleep(SNAPSHOT_INTERVAL)
        continue

    if normalized_landmarks:
        predictions = predict_states(model, normalized_landmarks)

        heatmap = generate_snapshot_heatmap(frames[-1], global_identities, predictions)

        image_url = upload_snapshot_heatmap(heatmap, str(session_id), str(snapshot_id))

        if image_url is not None:
            save_snapshot_heatmap(str(snapshot_id), image_url)
            logger.info(f"Heatmap uploaded for snapshot {snapshot_id}")
        else:
            logger.warning(f"Failed to upload heatmap for snapshot {snapshot_id}")

        insert_states(predictions, session_id, snapshot_id)



    active_session_ids = {s["id"] for s in active_sessions}

    for sid in list(session_trackers.keys()):
        if sid not in active_session_ids:
            logger.info(f"Cleaning tracker for session {sid}")
            del session_trackers[sid]

    time.sleep(SNAPSHOT_INTERVAL)