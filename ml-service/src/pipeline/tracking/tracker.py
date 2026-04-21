import logging

from pipeline.tracking.bbox import bbox_distance

logger = logging.getLogger(__name__)

def associate_faces(frame_faces, student_queues=None, next_student_id=0, distance_threshold=400):
    """
    Associate faces across frames using greedy bbox matching.

    Args:
        frame_faces (list[list[dict]]):
            - outer list -> Frames
            - inner list -> Detected faces in that frame.
        student_queues:
            - existing tracked students across session.
        next_student_id (int):
            - next student ID.
        distance_threshold (int):
            - max distance between two students.

    Returns:
        tuple:
            - student_queues (dict)
            - next_student_id (int)
    """
    if not frame_faces:
        return student_queues or {}, next_student_id

    if student_queues is None:
        student_queues = {}

    ALLOW_NEW_IDS = len(student_queues) == 0

    for frame_index, faces in enumerate(frame_faces):
        used_students = set()

        for face_dict in faces:
            current_bbox = face_dict.get("bbox")
            if current_bbox is None:
                continue

            face_dict["frame_index"] = frame_index

            best_student_id = None
            best_distance = float("inf")

            for student_id, queue in student_queues.items():
                if student_id in used_students:
                    continue

                last_valid = None
                for past in reversed(queue):
                    if past is not None:
                        last_valid = past
                        break

                if last_valid is None:
                    continue

                last_bbox = last_valid["bbox"]
                dist = bbox_distance(current_bbox, last_bbox)

                if dist < best_distance:
                    best_distance = dist
                    best_student_id = student_id

            if best_student_id is not None and best_distance < distance_threshold:
                face_dict["student_id"] = best_student_id
                student_queues[best_student_id].append(face_dict)
                used_students.add(best_student_id)

            else:
                if ALLOW_NEW_IDS and frame_index < 5:
                    face_dict["student_id"] = next_student_id

                    student_queues[next_student_id] = [None] * frame_index
                    student_queues[next_student_id].append(face_dict)

                    used_students.add(next_student_id)
                    next_student_id += 1
                else:
                    continue

        for student_id in student_queues:
            if student_id not in used_students:
                student_queues[student_id].append(None)

    logger.info(f"Total students tracked: {len(student_queues)}")

    return student_queues, next_student_id