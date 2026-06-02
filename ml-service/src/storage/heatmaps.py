import cv2 as cv
import numpy as np
import logging

from src.database.client import supabase

logger = logging.getLogger(__name__)


def upload_snapshot_heatmap(heatmap: np.ndarray | None, session_id: str, snapshot_id: str) -> str | None:
    """
    Upload a heatmap image to Supabase Storage and return its public URL.
    """
    if heatmap is None or heatmap.size == 0:
        return None

    success, buffer = cv.imencode(".png", heatmap)

    if not success:
        return None

    path = f"{session_id}/{snapshot_id}.png"

    try:
        supabase.storage.from_("heatmaps").upload(
            path,
            buffer.tobytes(),
            {
                "content-type": "image/png",
            }
        )

        return supabase.storage.from_("heatmaps").get_public_url(path)

    except Exception as error:
        logger.error(f"Failed to upload heatmap: {error}")
        return None