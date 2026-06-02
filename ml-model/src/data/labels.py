# Attention: 1-5 -> 0-2 (Low/Medium/High)
ATTENTION_MAP = {1:0, 2:0, 3:1, 4:2, 5:2}
ATTENTION_LABELS = [
    "Low",
    "Medium",
    "High",
]

# Emotion: 1-9 -> 0-3
EMOTION_MAP = {
    9:0, 8:0, 7:0,  # 0 = Positive Activating (Enjoyment, Hope, Pride)
    6:1,    # 1 = Positive Deactivating (Relief)
    5:2, 4:2, 3:2,  # 2 = Negative Activating (Anger, Anxiety, Shame)
    2:3, 1:3    # 3 = Negative Deactivating (Hopelessness -> Despair, Boredom)
}
EMOTION_LABELS = [
    "Positive-Activating",
    "Positive-Deactivating",
    "Negative-Activating",
    "Negative-Deactivating",
]