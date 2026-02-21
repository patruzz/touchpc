"""Mouse movement interpolation for smoother cursor motion."""

from __future__ import annotations

from typing import List, Tuple


class MouseSmoother:
    """Splits large mouse deltas into a sequence of smaller steps.

    When the magnitude of a movement exceeds ``threshold`` pixels the delta
    is broken into roughly equal sub-steps so that no single step is larger
    than ``max_step`` pixels along either axis.
    """

    def __init__(self, threshold: int = 50, max_step: int = 25) -> None:
        self._threshold = threshold
        self._max_step = max_step

    def smooth(self, dx: int, dy: int) -> List[Tuple[int, int]]:
        """Return a list of ``(dx, dy)`` pairs that sum to the original delta.

        For small movements (magnitude <= threshold) the original delta is
        returned as a single-element list.  For larger movements it is split
        via linear interpolation.
        """
        magnitude = max(abs(dx), abs(dy))
        if magnitude <= self._threshold:
            return [(dx, dy)]

        # Number of steps so that each step <= max_step
        steps = max(2, (magnitude + self._max_step - 1) // self._max_step)

        result: List[Tuple[int, int]] = []
        accum_x = 0
        accum_y = 0
        for i in range(1, steps + 1):
            target_x = round(dx * i / steps)
            target_y = round(dy * i / steps)
            step_dx = target_x - accum_x
            step_dy = target_y - accum_y
            accum_x = target_x
            accum_y = target_y
            result.append((step_dx, step_dy))

        return result
