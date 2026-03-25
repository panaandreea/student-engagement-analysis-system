import cv2 as cv

def resize_with_padding(image, target_size, pad_color):
    """
    Resize image while preserving aspect ratio and pad to target size.

    Args:
        image (np.ndarray): Input image.
        target_size (tuple): Target size as (width, height).
        pad_color (tuple): Padding color.

    Returns:
        np.ndarray | None: Resized and padded image, or None if input is invalid.
    """

    if image is None or image.size == 0:
        print("Invalid image.")
        return None

    target_width, target_height = target_size
    if target_width <= 0 or target_height <= 0:
        print("Invalid target size.")
        return None

    original_height, original_width = image.shape[:2]

    # Compute scale
    scale = min(target_width / original_width, target_height / original_height)

    new_width = max(1, int(original_width * scale))
    new_height = max(1, int(original_height * scale))

    # Choose interpolation based on scaling
    if scale < 1.0:
        interpolation = cv.INTER_AREA   # downscaling
    else:
        interpolation = cv.INTER_CUBIC  # upscaling

    # Resize image
    resized = cv.resize(image, (new_width, new_height), interpolation = interpolation)

    # Compute padding
    pad_left = (target_width - new_width) // 2
    pad_right = target_width - new_width - pad_left
    pad_top = (target_height - new_height) // 2
    pad_bottom = target_height - new_height - pad_top

    # Apply padding (black borders)
    padded = cv.copyMakeBorder(resized, pad_top, pad_bottom, pad_left, pad_right, borderType=cv.BORDER_CONSTANT, value = pad_color)

    return padded
