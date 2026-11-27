export const SUPPORTED_FPS = Object.freeze([30, 25, 20, 15, 12, 10, 8, 6, 5]);
export const DEFAULT_FPS = 30;

export function normalizeExportFps(value) {
  if (typeof value !== "number" && typeof value !== "string") return null;
  if (typeof value === "string" && value.trim() === "") return null;
  const fps = Number(value);
  return Number.isSafeInteger(fps) && SUPPORTED_FPS.includes(fps) ? fps : null;
}

/**
 * Chooses the supported output rate closest to the cadence actually presented
 * by the source video. A missing/unreliable sample intentionally falls back to
 * 30 FPS instead of silently producing an unexpectedly choppy clip.
 */
export function chooseOptimalFps(mediaTimes, maximumFps = DEFAULT_FPS) {
  if (!Array.isArray(mediaTimes)) return DEFAULT_FPS;
  if (!Number.isFinite(maximumFps) || maximumFps < 5) return DEFAULT_FPS;

  const intervals = [];
  for (let index = 1; index < mediaTimes.length; index += 1) {
    const previous = mediaTimes[index - 1];
    const current = mediaTimes[index];
    if (!Number.isFinite(previous) || !Number.isFinite(current)) continue;
    const interval = current - previous;
    if (interval >= 1 / 120 && interval <= 1.5) intervals.push(interval);
  }

  if (intervals.length < 3) return DEFAULT_FPS;
  intervals.sort((a, b) => a - b);
  const middle = Math.floor(intervals.length / 2);
  const medianInterval = intervals.length % 2
    ? intervals[middle]
    : (intervals[middle - 1] + intervals[middle]) / 2;

  const measuredFps = Math.min(maximumFps, 1 / medianInterval);
  let selected = SUPPORTED_FPS[0];
  let smallestDifference = Number.POSITIVE_INFINITY;

  for (const fps of SUPPORTED_FPS) {
    const candidate = Math.min(fps, maximumFps);
    if (candidate !== fps) continue;
    const difference = Math.abs(fps - measuredFps);
    if (difference < smallestDifference ||
        (difference === smallestDifference && fps > selected)) {
      selected = fps;
      smallestDifference = difference;
    }
  }

  return selected;
}
