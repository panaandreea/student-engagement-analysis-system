from src.database.client import supabase
from src.database.retry import execute_with_retry


def get_or_create_student(session_id: str, student_id: int) -> str | None:
    """
    Retrieve an existing student or create a new database entry.
    """

    response = execute_with_retry(
        lambda: supabase.table("students")
            .select("id")
            .eq("session_id", session_id)
            .eq("local_id", str(student_id))
            .execute()
    )

    if response and response.data:
        return response.data[0]["id"]

    insert_response = execute_with_retry(
        lambda: supabase.table("students").insert({
            "session_id": session_id,
            "local_id": str(student_id),
        }).execute()
    )

    if insert_response is None or not insert_response.data:
        return None

    return insert_response.data[0]["id"]