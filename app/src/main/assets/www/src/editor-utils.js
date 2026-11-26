export const MAX_CLIP_DURATION_SECONDS = 10;
export const CLIP_DURATION_TOLERANCE_SECONDS = 0.001;

export function isClipDurationAllowed(start, end, maxDuration = MAX_CLIP_DURATION_SECONDS) {
  const duration = end - start;
  return Number.isFinite(duration) &&
    duration > 0 &&
    Number.isFinite(maxDuration) &&
    maxDuration > 0 &&
    duration <= maxDuration + CLIP_DURATION_TOLERANCE_SECONDS;
}

export function nextStickerId(state) {
  if (!state || !Number.isSafeInteger(state.nextId) || state.nextId < 1) {
    throw new TypeError("Sticker ID state must contain a positive safe integer nextId.");
  }
  const id = "sticker-" + String(state.nextId).padStart(2, "0");
  state.nextId += 1;
  return id;
}


export const MIN_CLIP_DURATION_SECONDS = 0.1;

export function clampClipRange(start, end, sourceDuration, minimumDuration = MIN_CLIP_DURATION_SECONDS) {
  if (!Number.isFinite(sourceDuration) || !Number.isFinite(minimumDuration) ||
      minimumDuration <= 0 || sourceDuration < minimumDuration) return null;
  const requestedStart = Number.isFinite(start) ? start : 0;
  const requestedEnd = Number.isFinite(end) ? end : sourceDuration;
  const safeStart = Math.min(Math.max(0, requestedStart), sourceDuration - minimumDuration);
  const safeEnd = Math.min(sourceDuration, Math.max(safeStart + minimumDuration, requestedEnd));
  return { start: safeStart, end: safeEnd };
}

export function isClipRangeAllowed(start, end, sourceDuration,
  maxDuration = MAX_CLIP_DURATION_SECONDS,
  minimumDuration = MIN_CLIP_DURATION_SECONDS) {
  return Number.isFinite(sourceDuration) && sourceDuration >= minimumDuration &&
    Number.isFinite(start) && Number.isFinite(end) && start >= 0 &&
    end >= start + minimumDuration - Number.EPSILON &&
    end <= sourceDuration && isClipDurationAllowed(start, end, maxDuration);
}
