from src.tracking.bbox_utils import bbox_similarity, bbox_distance

import numpy as np

def cosine_similarity(face1, face2):
    """
    Compute cosine similarity between L2-normalized embeddings.
    """
    return float(np.dot(face1, face2))


def compute_average_embedding(queue):
    """
    Compute average embedding for a student queue.
    """
    embeddings = [f["embedding"] for f in queue if f.get("embedding") is not None]

    if not embeddings:
        return None

    avg = np.mean(embeddings, axis=0)

    return avg / (np.linalg.norm(avg) + 1e-12)


def associate_faces(frame_faces):
    """
    Associate detected faces across frames using hybrid tracking:
        - embedding similarity (primary)
        - bbox centroid similarity (fallback)

    Args:
        frame_faces (list[list[dict]]):
            - outer list -> Frames
            - inner list -> Detected faces in that frame

    Returns:
        dict:
            - student_id -> List of associated face_dicts
    """
    if not frame_faces:
        return {}

    student_queues = {}
    initialized = False

    for faces in frame_faces:
        if not initialized:
            valid_faces = [f for f in faces if f.get("embedding") is not None]

            if not valid_faces:
                continue

            for student_id, face_dict in enumerate(valid_faces):
                face_dict["student_id"] = student_id
                student_queues[student_id] = [face_dict]

            initialized = True
            continue

        for face_dict in faces:
            current_embedding = face_dict.get("embedding")
            current_bbox = face_dict.get("bbox")

            if current_bbox is None:
                continue

            best_student_id = None
            best_score = -1

            for student_id, queue in student_queues.items():
                last_bbox = queue[-1]["bbox"]
                reference_embedding = compute_average_embedding(queue)

                if bbox_distance(current_bbox, last_bbox) > 100:
                    continue

                spatial_score = bbox_similarity(current_bbox, last_bbox)

                if current_embedding is not None and reference_embedding is not None:
                    embedding_score = cosine_similarity(current_embedding, reference_embedding)
                    score = 0.5 * embedding_score + 0.5 * spatial_score
                else:
                    score = spatial_score

                if score > best_score:
                    best_score = score
                    best_student_id = student_id

            if best_student_id is not None and best_score >= 0.2:
                face_dict["student_id"] = best_student_id
                student_queues[best_student_id].append(face_dict)

    print(f"Total students: {len(student_queues)}")
    return student_queues