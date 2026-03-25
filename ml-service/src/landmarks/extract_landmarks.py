from src.config.settings import MAX_WORKER_THREADS

from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import mediapipe as mp
import cv2 as cv
import json

mp_face_mesh = mp.solutions.face_mesh.FaceMesh(
    static_image_mode=True,
    max_num_faces=1,
    refine_landmarks=True,
)


def extract_facemesh(face):
    """
    Extract MediaPipe FaceMesh landmarks from a face crop.

    Args:
        face (np.array): BGR face crop image.

    Returns:
        list[tuple]: List of (x, y, z) normalized landmarks.
    """
    if face is None or face.size == 0:
        return None

    face = cv.cvtColor(face, cv.COLOR_BGR2RGB)
    results = mp_face_mesh.process(face)

    if not results.multi_face_landmarks:
        return None

    landmarks = results.multi_face_landmarks[0].landmark

    facemesh = []
    for idx, lm in enumerate(landmarks):
        facemesh.append({
            "id": idx,
            "x": float(lm.x),
            "y": float(lm.y),
            "z": float(lm.z),
        })
    return facemesh

def save_json(output_path, data):
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=4)


def export_facemesh(student_queues, run_dir: Path):
    """
    Export MediaPipe FaceMesh to JSON files per student, grouped by run.

    Structure:
        run_x:
            student_0:
                frame_0.json
                frame_1.json
            student_1:
                frame_0.json

    Args:
        student_queues (dict): Dictionary of student ID and face landmarks.
        run_dir (Path): Path to the directory where the JSON files are saved.
    """
    run_dir.mkdir(parents=True, exist_ok=True)

    students_with_landmarks = 0

    for student_id, faces in student_queues.items():
        student_dir = run_dir / f"student_{student_id + 1}"
        student_dir.mkdir(parents=True, exist_ok=True)

        student_has_landmarks = False
        tasks = []

        with ThreadPoolExecutor(max_workers=MAX_WORKER_THREADS) as executor:

            for frame_index, face_dict in enumerate(faces, start=1):
                face = face_dict.get("face")
                bbox = face_dict.get("bbox")

                if bbox is None:
                    continue

                facemesh = extract_facemesh(face)
                if facemesh is None:
                    continue

                student_has_landmarks = True

                x0, y0, x1, y1 = bbox

                frame_json = {
                    "student_id": student_id,
                    "frame_index": frame_index,
                    "face": {
                        "bounding_box": {
                            "x0": int(x0),
                            "y0": int(y0),
                            "x1": int(x1),
                            "y1": int(y1),
                        },
                        "facemesh": facemesh,
                    }
                }
                output_path = student_dir / f"frame_{frame_index}.json"

                tasks.append(executor.submit(save_json, output_path, frame_json))

            for t in tasks:
                t.result()

            if student_has_landmarks:
                students_with_landmarks += 1

    print(f"Extracted landmarks for: {students_with_landmarks}/{len(student_queues)} students.")
