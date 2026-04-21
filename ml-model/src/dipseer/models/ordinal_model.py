import os
import tensorflow.keras as keras

from tensorflow.keras import layers, Model
from src.shared.models.backbone import build_backbone

def build_dipseer_model():

    inputs, x = build_backbone()

    attention_output = layers.Dense(2, activation='sigmoid', name='att_output')(x)

    return Model(inputs=inputs, outputs=attention_output, name='Dipseer')


def compile_model(model, learning_rate=1e-4, weight_decay=1e-4, clipnorm=1.0):

    optimizer = keras.optimizers.Adam(
        learning_rate=learning_rate,
        weight_decay=weight_decay,
        clipnorm=clipnorm
    )

    model.compile(
        optimizer=optimizer,
        loss=keras.losses.BinaryCrossentropy(),
        metrics=[keras.metrics.BinaryAccuracy()]
    )


def get_callbacks():

    os.makedirs("checkpoints/ordinal", exist_ok=True)
    checkpoint_path = "checkpoints/ordinal/best_model.h5"

    return [
        keras.callbacks.EarlyStopping(
            monitor='val_loss',
            patience=10,
            restore_best_weights=True,
            verbose=1
        ),

        keras.callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=5,
            min_lr=1e-6,
            verbose=1
        ),

        keras.callbacks.ModelCheckpoint(
            checkpoint_path,
            monitor='val_loss',
            mode='min',
            save_best_only=True,
            save_weights_only=True,
            verbose=1
        )
    ]