from datetime import datetime, timezone

from src.database.client import supabase
from src.database.retry import execute_with_retry


def get_active_sessions(device_id: str) -> list[dict]:
    """
    Retrieve currently active sessions for a classroom device.
    """
    now = datetime.now(timezone.utc).isoformat()

    response = execute_with_retry(
        lambda: supabase.table("sessions")
            .select("*")
            .eq("classroom", device_id)
            .lte("start_time", now)
            .gte("end_time", now)
            .execute()
    )

    if response is None:
        return []

    return response.data or []