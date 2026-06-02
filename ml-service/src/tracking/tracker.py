import numpy as np

from src.tracking.bbox import bbox_centroid, centroid_distance


def track_faces_in_snapshot(
    frame_faces: list[list[dict]],
    distance_threshold: float = 500.0
) -> list[dict]:
    """
    Track faces between consecutive frames inside a snapshot.
    """
    if not frame_faces:
        return []

    tracks = []

    first_valid_index = 0
    first_valid_frame = None

    for i, faces in enumerate(frame_faces):
        if faces:
            first_valid_frame = faces
            first_valid_index = i
            break

    if first_valid_frame is None:
        return []

    for detection in first_valid_frame:
        centroid = tuple(bbox_centroid(detection["bbox"]))

        tracks.append({
            "embedding": detection.get("embedding"),
            "faces": [None] * first_valid_index + [detection["face"]],
            "centroids": [None] * first_valid_index + [centroid],
            "bboxes": [None] * first_valid_index + [detection["bbox"]],
        })

    for frame_faces in frame_faces[first_valid_index + 1:]:
        matched_tracks = set()

        for detection in frame_faces:
            centroid = tuple(bbox_centroid(detection["bbox"]))

            best_track_index = None
            best_distance = float("inf")

            for track_index, track in enumerate(tracks):
                if track_index in matched_tracks:
                    continue

                last_centroid = next(
                    (c for c in reversed(track["centroids"]) if c is not None),
                    None
                )

                if last_centroid is None:
                    continue

                distance = centroid_distance(centroid, last_centroid)

                if distance < best_distance:
                    best_distance = distance
                    best_track_index = track_index

            if best_track_index is not None and best_distance < distance_threshold:
                track = tracks[best_track_index]

                track["faces"].append(detection["face"])
                track["centroids"].append(centroid)
                track["bboxes"].append(detection["bbox"])

                if track["embedding"] is None:
                    track["embedding"] = detection.get("embedding")

                matched_tracks.add(best_track_index)

        for track_index, track in enumerate(tracks):
            if track_index not in matched_tracks:
                track["faces"].append(None)
                track["centroids"].append(None)
                track["bboxes"].append(None)

    return tracks


def track_faces_between_snapshots(
    existing_identities: list[dict],
    current_tracks: list[dict],
    next_identifier: int = 1
) -> tuple[list[dict], int]:
    """
    Associate tracked identities between consecutive snapshots.
    """
    if not existing_identities:
        identities = []

        for track in current_tracks:
            identities.append({
                "id": next_identifier,
                "embedding": track["embedding"],
                "faces": track["faces"],
                "centroids": track["centroids"],
                "bboxes": track["bboxes"],
            })

            next_identifier += 1

        return identities, next_identifier

    updated_identities = []
    used_previous_identifiers = set()

    for track in current_tracks:
        current_centroid = next((c for c in reversed(track["centroids"]) if c is not None), None)

        best_match = None
        best_score = -1.0

        for previous_identity in existing_identities:
            if previous_identity["id"] in used_previous_identifiers:
                continue

            previous_centroid = next((c for c in reversed(previous_identity["centroids"]) if c is not None), None)

            score = 0.0

            if track["embedding"] is not None and previous_identity["embedding"] is not None:
                embedding_similarity = float(np.dot(track["embedding"], previous_identity["embedding"]))

                dist_score = (
                    max(0.0, 1.0 - (centroid_distance(current_centroid, previous_centroid) / 500.0))
                    if current_centroid is not None and previous_centroid is not None
                    else 0.0
                )

                score = (0.6 * embedding_similarity + 0.4 * dist_score)

            elif current_centroid is not None and previous_centroid is not None:
                score = max(0.0, 1.0 - (centroid_distance(current_centroid, previous_centroid) / 500.0))

            if score > best_score:
                best_score = score
                best_match = previous_identity

        if best_match is not None and best_score > 0.5:
            updated_identities.append({
                "id": best_match["id"],
                "embedding": (track["embedding"] if track["embedding"] is not None else best_match["embedding"]),
                "faces": track["faces"],
                "centroids": track["centroids"],
                "bboxes": track["bboxes"],
            })

            used_previous_identifiers.add(best_match["id"])

        else:
            updated_identities.append({
                "id": next_identifier,
                "embedding": track["embedding"],
                "faces": track["faces"],
                "centroids": track["centroids"],
                "bboxes": track["bboxes"],
            })

            next_identifier += 1

    return updated_identities, next_identifier