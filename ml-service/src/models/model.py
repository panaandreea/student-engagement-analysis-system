import numpy as np

from tensorflow.keras import layers
from tensorflow.keras.models import Model

from src.config.settings import ENGAGEMENT_MODEL_PATH, ENGAGEMENT_LABEL_MAP


def build_model(input_dim: int = 1434, sequence_length: int = 20, dropout: float = 0.5) -> Model:
    inputs = layers.Input(shape=(sequence_length, input_dim), name='inputs')

    x = layers.LSTM(256, return_sequences=True)(inputs)
    x = layers.LSTM(128, return_sequences=False)(x)

    x = layers.Dropout(dropout)(x)

    x = layers.Dense(128, activation='relu')(x)
    x = layers.Dense(64, activation='relu')(x)

    output = layers.Dense(3, activation='softmax', name="att_output")(x)

    model = Model(inputs=inputs, outputs=output, name="Dipseer")

    model.load_weights(ENGAGEMENT_MODEL_PATH)

    return model


def predict_states(model: Model, student_sequences: dict) -> dict:
    predictions = {}

    for student_id, sequence in student_sequences.items():
        if sequence is None or len(sequence) == 0:
            continue

        sequence = np.asarray(sequence, dtype=np.float32)

        if sequence.shape[0] != model.input_shape[1]:
            continue

        if sequence.shape[1] != model.input_shape[2]:
            continue

        sequence = np.expand_dims(sequence, axis=0)

        probabilities = model.predict(sequence, verbose=0)[0]
        label_index = int(np.argmax(probabilities))

        predictions[student_id] = {
            "label_index": label_index,
            "attention_label": ENGAGEMENT_LABEL_MAP[label_index],
            "probabilities": probabilities,
        }

    return predictions