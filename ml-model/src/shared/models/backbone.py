from tensorflow.keras import layers

def build_backbone(input_dim=1434, sequence_length=20, dropout=0.5):

    inputs = layers.Input(shape=(sequence_length, input_dim), name='inputs')

    x = layers.LSTM(256, return_sequences=True)(inputs)
    x = layers.LSTM(128, return_sequences=False)(x)

    x = layers.Dropout(dropout)(x)

    x = layers.Dense(128, activation='relu')(x)
    x = layers.Dense(64, activation='relu')(x)

    return inputs, x