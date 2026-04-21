import time

from src.pipeline.camera.resolution import get_max_camera_resolution
from src.pipeline.camera.capture import capture_frames
from src.pipeline.detection.detector import detect_and_process_faces
from src.pipeline.tracking.tracker import associate_faces
from src.pipeline.landmarks.facemesh import process_landmark_sequence
from src.database.snapshot import create_session_snapshot
from src.database.session import get_active_sessions
from src.database.states import insert_student_states
from src.preprocessing.normalization import normalize_student_landmarks
from src.config.settings import CAMERA_ID, NUMBER_OF_FRAMES, CAPTURE_DURATION
from src.services.scheduler import compute_observation_number
from src.models.model import predict_student_states, build_model

WIDTH, HEIGHT = get_max_camera_resolution(CAMERA_ID)
print(WIDTH, HEIGHT)

model = build_model()

session_trackers = {}

while True:
    active_sessions = get_active_sessions()

    if not active_sessions:
        print("No active sessions")
        time.sleep(1)
        continue

    active_ids = {s["id"] for s in active_sessions}

    for session in active_sessions:
        session_id = session["id"]

        if session_id not in session_trackers:
            session_trackers[session_id] = {
                "student_queues": {},
                "next_student_id": 0,
                "last_observation": 0
            }

        tracker = session_trackers[session_id]

        current_obs = compute_observation_number(session)

        if current_obs == 0:
            continue

        if current_obs <= tracker["last_observation"]:
            continue

        print(f"Processing session {session_id} | Observation {current_obs}")

        frames = capture_frames(CAMERA_ID, WIDTH, HEIGHT, NUMBER_OF_FRAMES, CAPTURE_DURATION)

        if not frames:
            continue

        faces = detect_and_process_faces(frames)

        student_queues, next_student_id = associate_faces(
            faces,
            tracker["student_queues"],
            tracker["next_student_id"]
        )

        tracker["student_queues"] = student_queues
        tracker["next_student_id"] = next_student_id

        session_landmarks = process_landmark_sequence(student_queues)

        normalized = normalize_student_landmarks(session_landmarks)

        if not normalized:
            print("No valid landmarks")
            continue

        predictions = predict_student_states(model, normalized)

        snapshot_id, snapshot_index = create_session_snapshot(session_id)

        insert_student_states(predictions, session_id, snapshot_id)

        tracker["last_observation"] = current_obs

        print(f"Inserted snapshot: {snapshot_id} (#{snapshot_index})")

    time.sleep(1)