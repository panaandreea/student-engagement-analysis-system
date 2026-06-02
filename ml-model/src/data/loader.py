import os
import json
import numpy as np
from collections import defaultdict

from src.preprocessing.sequence.normalization import normalize_landmark_sequence
from src.preprocessing.sequence.imputation import impute_missing_frames
from src.data.labels import ATTENTION_MAP, EMOTION_MAP

SEQUENCE_LENGTH = 20
OVERLAP = 0.5
STEP = round(SEQUENCE_LENGTH * (1 - OVERLAP))
NUM_LANDMARKS = 478


def load_frame(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
    except (FileNotFoundError, json.JSONDecodeError, OSError):
        return None, None, None

    try:
        mesh = data["person"]["face"]["facemesh"]
        landmarks = np.array([[p["x"], p["y"], p["z"]] for p in mesh], dtype=np.float32)

        if landmarks.shape != (NUM_LANDMARKS, 3):
            landmarks = None

    except (KeyError, TypeError, ValueError):
        landmarks = None

    labels = data.get("labels", {})

    emotion_raw = labels.get("labeler_04", {}).get("emotion")
    attention_raw = labels.get("labeler_04", {}).get("attention")

    try:
        emotion_raw = int(emotion_raw)
        attention_raw = int(attention_raw)

    except (ValueError, TypeError):
        return landmarks, None, None

    return landmarks, attention_raw, emotion_raw


def map_attention(val):
    return ATTENTION_MAP.get(val)


def map_emotion(val):
    return EMOTION_MAP.get(val)


def build_dataset_with_statistics(split_paths):
    """
    Build datasets using sliding-window processing over frame sequences.
    """
    datasets = {}

    for split_name, split_path in split_paths.items():
        X = []
        y_attention = []
        y_emotion = []

        statistics = {
            "attention": defaultdict(int),
            "emotion": defaultdict(int),
        }

        print(f"\nBuilding {split_name}...")

        for group_name in os.listdir(split_path):

            group_path = os.path.join(split_path, group_name)

            if not os.path.isdir(group_path):
                continue

            for experiment_name in os.listdir(group_path):

                experiment_path = os.path.join(group_path, experiment_name)

                if not os.path.isdir(experiment_path):
                    continue

                for subject in os.listdir(experiment_path):

                    metadata_path = os.path.join(experiment_path, subject, "metadata")

                    if not os.path.isdir(metadata_path):
                        continue

                    frames = []

                    for file in sorted(os.listdir(metadata_path)):
                        if not file.endswith(".json"):
                            continue

                        path = os.path.join(metadata_path, file)

                        landmarks, attention_raw, emotion_raw = load_frame(path)

                        if attention_raw is None or emotion_raw is None:
                            continue

                        attention = map_attention(attention_raw)
                        emotion = map_emotion(emotion_raw)

                        if attention is None or emotion is None:
                            continue

                        if landmarks is None:
                            landmarks = np.zeros((NUM_LANDMARKS, 3), dtype=np.float32)

                        frames.append((landmarks, attention, emotion))

                    for i in range(0, len(frames) - SEQUENCE_LENGTH + 1, STEP):
                        seq = frames[i:i + SEQUENCE_LENGTH]

                        landmarks_sequence = [f[0] for f in seq]
                        attention_sequence = [f[1] for f in seq]
                        emotion_sequence = [f[2] for f in seq]

                        if len(set(attention_sequence)) != 1 or len(set(emotion_sequence)) != 1:
                            continue

                        try:
                            sequence = np.stack(landmarks_sequence)
                            sequence = impute_missing_frames(sequence)
                            sequence = normalize_landmark_sequence(sequence)

                        except (ValueError, TypeError):
                            continue

                        if np.all(sequence == 0):
                            continue

                        attention_final = attention_sequence[0]
                        emotion_final = emotion_sequence[0]

                        X.append(sequence)
                        y_attention.append(attention_final)
                        y_emotion.append(emotion_final)

                        statistics["attention"][attention_final] += 1
                        statistics["emotion"][emotion_final] += 1

        X = np.array(X, dtype=np.float32)
        y_attention = np.array(y_attention, dtype=np.int32)
        y_emotion = np.array(y_emotion, dtype=np.int32)

        datasets[split_name] = (X, y_attention, y_emotion, statistics)

        print(f"{split_name}: {len(X)} sequences")

        print("\nAttention distribution:")
        for k, v in statistics["attention"].items():
            print(f"{k}: {v}")

        print("\nEmotion distribution:")
        for k, v in statistics["emotion"].items():
            print(f"{k}: {v}")

    return datasets