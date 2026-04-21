from datetime import datetime, timezone
from src.database.client import supabase

def create_session_snapshot(session_id):
    response = supabase.table("session_snapshots") \
        .select("snapshot_index") \
        .eq("session_id", session_id) \
        .order("snapshot_index", desc=True) \
        .limit(1) \
        .execute()

    if response.data:
        last_number = response.data[0]["snapshot_index"]
        next_number = last_number + 1
    else:
        next_number = 1

    insert_response = supabase.table("session_snapshots").insert({
        "session_id": session_id,
        "snapshot_index": next_number,
        "captured_at": datetime.now(timezone.utc).isoformat(),
    }).execute()

    snapshot_id = insert_response.data[0]["id"]

    return snapshot_id, next_number