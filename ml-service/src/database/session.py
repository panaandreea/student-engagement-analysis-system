from datetime import datetime, timezone
from src.database.client import supabase

def get_active_sessions():
    now = datetime.now(timezone.utc).isoformat()

    response = supabase.table("sessions") \
        .select("*") \
        .lte("start_time", now) \
        .gte("end_time", now) \
        .execute()

    if response.data is None:
        return []

    return response.data