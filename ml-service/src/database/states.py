from src.database.client import supabase
from src.database.students import get_or_create_student
from src.database.retry import execute_with_retry


def insert_states(predictions: dict, session_id: str, snapshot_id: int) -> bool:
    """
    Insert predicted engagement states for tracked students.
    """
    rows = []

    for student_id, data in predictions.items():
        if not data:
            continue

        attention = data.get("attention_label")
        probabilities = data.get("probabilities")

        if attention is None or probabilities is None:
            continue

        confidence = float(max(probabilities))

        database_student_id = get_or_create_student(session_id, student_id)

        if database_student_id is None:
            continue

        rows.append({
            "student_id": database_student_id,
            "snapshot_id": snapshot_id,
            "attention": attention,
            "confidence": confidence,
        })

    if not rows:
        return False

    response = execute_with_retry(
        lambda: supabase.table("student_states").insert(rows).execute()
    )

    if response is None or not response.data:
        return False

    return True