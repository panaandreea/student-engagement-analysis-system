from src.database.client import supabase
from src.database.student import get_or_create_student

def insert_student_states(predictions: dict, session_id: str, observation_id: str):
    rows = []

    for student_id, data in predictions.items():
        if not data:
            continue

        attention = data.get("attention_label")
        if attention is None:
            continue

        database_student_id = get_or_create_student(session_id, student_id)

        rows.append({
            "student_id": database_student_id,
            "snapshot_id": observation_id,
            "attention": attention,
            "emotion": "unknown",
        })

    if rows:
        supabase.table("student_states").insert(rows).execute()