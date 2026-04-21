from datetime import datetime, timezone

def compute_observation_number(session: dict) -> int:
    now = datetime.now(timezone.utc)

    start = datetime.fromisoformat(session["start_time"])
    end = datetime.fromisoformat(session["end_time"])

    if start.tzinfo is None:
        start = start.replace(tzinfo=timezone.utc)
    else:
        start = start.astimezone(timezone.utc)

    if end.tzinfo is None:
        end = end.replace(tzinfo=timezone.utc)
    else:
        end = end.astimezone(timezone.utc)

    total_observations = session["planned_observations"]

    if total_observations <= 0:
        return 0

    total_seconds = (end - start).total_seconds()
    if total_seconds <= 0:
        return 0

    elapsed_seconds = (now - start).total_seconds()

    if elapsed_seconds < 0:
        return 0

    if elapsed_seconds >= total_seconds:
        return total_observations

    interval = total_seconds / total_observations

    observation_number = int(elapsed_seconds // interval) + 1
    observation_number = min(observation_number, total_observations)

    return observation_number