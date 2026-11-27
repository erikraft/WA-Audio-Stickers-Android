export const SUPPORTED_FPS = Object.freeze([30, 25, 20, 15, 12, 10, 8, 6, 5]);
export const DEFAULT_FPS = 30;

export function defaultImageFps(file) {
  if (!file || typeof file !== "object") return 5;
  const mimeType = typeof file.type === "string" ? file.type.toLowerCase() : "";
  const name = typeof file.name === "string" ? file.name : "";
  return mimeType === "image/gif" || /\.gif$/i.test(name) ? 30 : 5;
}

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
export function chooseOptimalFps(samples, maximumFps = DEFAULT_FPS) {
  if (!Array.isArray(samples)) return DEFAULT_FPS;
  if (!Number.isFinite(maximumFps) || maximumFps < 5) return DEFAULT_FPS;

  const intervals = [];
  for (let index = 1; index < samples.length; index += 1) {
    const previous = samples[index - 1];
    const current = samples[index];
    if (!previous || !current || typeof previous !== "object" || typeof current !== "object") {
      return DEFAULT_FPS;
    }

    const previousTime = previous.mediaTime;
    const currentTime = current.mediaTime;
    const previousFrames = previous.presentedFrames;
    const currentFrames = current.presentedFrames;
    if (!Number.isFinite(previousTime) || !Number.isFinite(currentTime) ||
        !Number.isSafeInteger(previousFrames) || !Number.isSafeInteger(currentFrames)) {
      return DEFAULT_FPS;
    }

    const frameDelta = currentFrames - previousFrames;
    const timeDelta = currentTime - previousTime;
    if (frameDelta <= 0 || timeDelta <= 0) return DEFAULT_FPS;

    // presentedFrames advances by more than one when compositor callbacks were
    // skipped. Divide media-time by that count so a dropped callback does not
    // make a 30 FPS source look like a 15 FPS source.
    const interval = timeDelta / frameDelta;
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
    if (fps > maximumFps) continue;
    const difference = Math.abs(fps - measuredFps);
    if (difference < smallestDifference ||
        (difference === smallestDifference && fps > selected)) {
      selected = fps;
      smallestDifference = difference;
    }
  }

  return selected;
}
