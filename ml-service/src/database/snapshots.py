from datetime import datetime, timezone

from src.database.client import supabase
from src.database.retry import execute_with_retry


def create_session_snapshot(session_id: int) -> tuple[int | None, int | None]:
    """
    Create a new snapshot entry for a monitoring session.
    """

    response = execute_with_retry(
        lambda: supabase.table("session_snapshots")
            .select("snapshot_index")
            .eq("session_id", session_id)
            .order("snapshot_index", desc=True)
            .limit(1)
            .execute()
    )

    if response is None:
        return None, None

    if response.data:
        last_number = response.data[0]["snapshot_index"]
        next_number = last_number + 1
    else:
        next_number = 1

    insert_response = execute_with_retry(
        lambda: supabase.table("session_snapshots").insert({
            "session_id": session_id,
            "snapshot_index": next_number,
            "captured_at": datetime.now(timezone.utc).isoformat(),
        }).execute()
    )

    if insert_response is None or not insert_response.data:
        return None, None

    snapshot_id = insert_response.data[0]["id"]

    return snapshot_id, next_number


def save_snapshot_heatmap(snapshot_id: str, image_url: str) -> bool:
    """
    Attach a generated heatmap image to a session snapshot.
    """
    response = execute_with_retry(
        lambda: supabase.table("session_snapshots")
        .update({
            "image_url": image_url,
        })
        .eq("id", snapshot_id)
        .execute()
    )

    if response is None or not response.data:
        return False

    return True