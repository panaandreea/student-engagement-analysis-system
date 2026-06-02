import tensorflow.keras
import os

from tensorflow.keras import layers, Model


def build_model(input_dim=1434, sequence_length=20, dropout=0.5):

    inputs = layers.Input(shape=(sequence_length, input_dim), name='inputs')

    x = layers.LSTM(256, return_sequences=True, name="lstm_1")(inputs)
    x = layers.LSTM(128, return_sequences=False, name="lstm_2")(x)

    x = layers.Dropout(dropout, name="dropout_1")(x)

    x = layers.Dense(128, activation='relu', name="dense_1")(x)
    x = layers.Dense(64, activation='relu', name="dense_2")(x)

    outputs = layers.Dense(3, activation='softmax', name='outputs')(x)

    return Model(inputs=inputs, outputs=outputs, name='dipseer')


def compile_model(model, learning_rate=1e-4, weight_decay=1e-4, clipnorm=1.0):
    optimizer = tensorflow.keras.optimizers.Adam(
        learning_rate=learning_rate,
        weight_decay=weight_decay,
        clipnorm=clipnorm
    )

    model.compile(
        optimizer=optimizer,
        loss=tensorflow.keras.losses.SparseCategoricalCrossentropy(),
        metrics=['accuracy']
    )


def get_callbacks(model_name="best_model_01"):
    base_dir = os.path.dirname(os.path.abspath(__file__))

    project_root = os.path.abspath(os.path.join(base_dir, "..", ".."))

    checkpoints_dir = os.path.join(project_root, "checkpoints")

    os.makedirs(checkpoints_dir, exist_ok=True)

    checkpoint_path = os.path.join(checkpoints_dir, f"{model_name}.h5")

    return [
        tensorflow.keras.callbacks.EarlyStopping(
            monitor='val_loss',
            patience=10,
            restore_best_weights=True,
            verbose=1
        ),
        tensorflow.keras.callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=5,
            min_lr=1e-6,
            verbose=1
        ),
        tensorflow.keras.callbacks.ModelCheckpoint(
            checkpoint_path,
            monitor='val_loss',
            mode='min',
            save_best_only=True,
            save_weights_only=True,
            verbose=1
        )
    ]