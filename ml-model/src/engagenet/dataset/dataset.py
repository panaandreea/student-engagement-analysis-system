import os
import numpy as np

from src.engagenet.dataset.labels import ENGAGENET_LABEL_MAP
from src.engagenet.preprocessing.pipeline import preprocess_sequence

def build_dataset(dataset_path: str):
    """
    Build dataset (X, y) from directory structure:
        dataset_path/
            class1/
                file1.json
                file2.json
            class2/
                ...

    Returns:
        X: (num_samples, T, num_features)
        y: (num_samples)
    """
    X, y = [], []

    for label_name in sorted(os.listdir(dataset_path)):
        if label_name not in ENGAGENET_LABEL_MAP:
            continue

        label = ENGAGENET_LABEL_MAP[label_name]
        label_path = os.path.join(dataset_path, label_name)

        for file in sorted(os.listdir(label_path)):
            if not file.endswith(".json"):
                continue

            file_path = os.path.join(label_path, file)

            features = preprocess_sequence(file_path)

            X.append(features)
            y.append(label)

    return np.array(X, dtype=np.float32), np.array(y, dtype=np.int32)