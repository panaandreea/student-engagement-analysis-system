from src.database.client import supabase

def get_or_create_student(session_id: str, student_id: int) -> str:
    response = supabase.table("students") \
        .select("id") \
        .eq("session_id", session_id) \
        .eq("local_id", str(student_id)) \
        .execute()

    if response.data:
        return response.data[0]["id"]

    insert_response = supabase.table("students").insert({
        "session_id": session_id,
        "local_id": str(student_id),
    }).execute()

    return insert_response.data[0]["id"]