import os
import json
import numpy as np
from collections import defaultdict

from src.shared.preprocessing.normalization import normalize_landmark_sequence
from src.shared.preprocessing.cleaning import fill_missing_frames
from src.dipseer.dataset.labels import ATTENTION_MAP, EMOTION_MAP_4

SEQUENCE_LENGTH = 20
STEP = SEQUENCE_LENGTH // 2
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

    emo_raw = labels.get("labeler_04", {}).get("emotion")
    att_raw = labels.get("labeler_04", {}).get("attention")

    try:
        emo_raw = int(emo_raw)
        att_raw = int(att_raw)
    except (ValueError, TypeError):
        return landmarks, None, None

    return landmarks, att_raw, emo_raw


def map_attention(val):
    return ATTENTION_MAP.get(val)


def map_emotion(val):
    return EMOTION_MAP_4.get(val)


def build_dataset_with_stats(split_paths, allowed_experiments):
    datasets = {}

    for split_name, split_path in split_paths.items():
        X = []
        y_attention = []
        y_emotion = []

        stats = {
            "attention": defaultdict(int),
            "emotion": defaultdict(int),
            "valid": 0
        }

        print(f"\nBuilding {split_name}...")

        for exp_rel_path in allowed_experiments:
            exp_path = os.path.join(str(split_path), str(exp_rel_path))

            if not os.path.isdir(exp_path):
                continue

            for subject in os.listdir(exp_path):
                metadata_path = os.path.join(exp_path, subject, "metadata")

                if not os.path.isdir(metadata_path):
                    continue

                frames = []

                for file in sorted(os.listdir(metadata_path)):
                    if not file.endswith(".json"):
                        continue

                    path = os.path.join(metadata_path, file)

                    landmarks, att_raw, emo_raw = load_frame(path)

                    if att_raw is None or emo_raw is None:
                        continue

                    att = map_attention(att_raw)
                    emo = map_emotion(emo_raw)

                    if att is None or emo is None:
                        continue

                    if landmarks is None:
                        landmarks = np.zeros((NUM_LANDMARKS, 3), dtype=np.float32)

                    frames.append((landmarks, att, emo))

                for i in range(0, len(frames) - SEQUENCE_LENGTH + 1, STEP):
                    seq = frames[i:i + SEQUENCE_LENGTH]

                    landmarks_seq = [f[0] for f in seq]
                    att_seq = [f[1] for f in seq]
                    emo_seq = [f[2] for f in seq]

                    if len(set(att_seq)) != 1 or len(set(emo_seq)) != 1:
                        continue

                    try:
                        sequence = np.stack(landmarks_seq)
                        sequence = fill_missing_frames(sequence)
                        sequence = normalize_landmark_sequence(sequence)
                    except (ValueError, TypeError):
                        continue

                    if np.all(sequence == 0):
                        continue

                    att_final = att_seq[0]
                    emo_final = emo_seq[0]

                    X.append(sequence)
                    y_attention.append(att_final)
                    y_emotion.append(emo_final)

                    stats["valid"] += 1
                    stats["attention"][att_final] += 1
                    stats["emotion"][emo_final] += 1

        X = np.array(X, dtype=np.float32)
        y_attention = np.array(y_attention, dtype=np.int32)
        y_emotion = np.array(y_emotion, dtype=np.int32)

        datasets[split_name] = (X, y_emotion, y_attention, stats)

        print(f"{split_name}: {len(X)} sequences")

        print("\nAttention distribution:")
        for k, v in stats["attention"].items():
            print(f"{k}: {v}")

        print("\nEmotion distribution:")
        for k, v in stats["emotion"].items():
            print(f"{k}: {v}")

    return datasets