from datetime import datetime
from pathlib import Path
import cv2 as cv

def draw_bboxes(frame, faces, output_dir: Path):
    """
    Draw filled black bounding boxes over detected faces in a frame
    and save the resulting image to disk.

    Args:
        frame (np.array): Original BGR frame.
        faces (list[dict]): Detected faces for this frame.
        output_dir (Path): Path to the directory where the image will be saved.

    Returns:
        None.
    """
    if frame is None or frame.size == 0:
        print("Invalid frame.")
        return

    if not faces:
        print("No faces to draw.")
        return

    output_dir.mkdir(parents=True, exist_ok=True)

    # Work on a copy to avoid modifying the original frame
    output_frame = frame.copy()
    h, w = frame.shape[:2]

    for face_dict in faces:
        bbox = face_dict.get('bbox')
        if bbox is None or len(bbox) != 4:
            continue

        x1, y1, x2, y2 = bbox

        x1 = max(0, min(int(x1), w - 1))
        y1 = max(0, min(int(y1), h - 1))
        x2 = max(0, min(int(x2), w - 1))
        y2 = max(0, min(int(y2), h - 1))

        cv.rectangle(
            output_frame,
            (x1, y1),
            (x2, y2),
            color=(0, 0, 0),
            thickness=-1,
        )

    # Generate timestamp-based filename
    timestamp = datetime.now().strftime('%Y.%m.%d-%H.%M.%S.%f')
    filename = f"frame_{timestamp}.png"
    filepath = output_dir / filename

    # Save image to disk
    cv.imwrite(str(filepath), output_frame)

