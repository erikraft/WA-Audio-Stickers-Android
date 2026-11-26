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
