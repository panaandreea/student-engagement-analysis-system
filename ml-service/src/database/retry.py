import time
import logging

from typing import Callable, Any

logger = logging.getLogger(__name__)


def execute_with_retry(func: Callable, retries: int = 3, delay: int = 30) -> Any:
    """
    Execute a function with automatic retry on failure.
    """
    for attempt in range(1, retries + 1):
        try:
            return func()

        except Exception as e:
            logger.warning(
                f"Retry {attempt}/{retries} failed: {e}"
            )

            if attempt < retries:
                time.sleep(delay)

    logger.error("All retries failed")

    return None