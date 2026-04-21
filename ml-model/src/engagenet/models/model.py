import tensorflow.keras
import os

from tensorflow.keras.models import Model
from tensorflow.keras import layers
from shared.models.backbone import build_backbone

def build_engagenet_model():

    inputs, x = build_backbone()

    output = layers.Dense(4, activation='softmax', name="engagenet_output", dtype='float32')(x)

    return Model(inputs=inputs, outputs=output, name='EngageNet')


def compile_model(model, learning_rate=1e-3, weight_decay=1e-4, clipnorm=1.0):

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


def get_callbacks():
    os.makedirs("checkpoints", exist_ok=True)
    checkpoint_path = "checkpoints/best_model.h5"

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