import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(
  new URL("../app/src/main/assets/www/src/fps-utils.js", import.meta.url),
  "utf8"
);
const fpsUtils = await import(
  "data:text/javascript;base64," + Buffer.from(source).toString("base64")
);

function frameSamples(fps, count = 12, firstPresentedFrame = 1) {
  return Array.from({ length: count }, (_, index) => ({
    mediaTime: index / fps,
    presentedFrames: firstPresentedFrame + index
  }));
}

test("automatic FPS follows common source frame rates without exceeding the 30 FPS cap", () => {
  assert.equal(fpsUtils.chooseOptimalFps(frameSamples(60)), 30);
  assert.equal(fpsUtils.chooseOptimalFps(frameSamples(30)), 30);
  assert.equal(fpsUtils.chooseOptimalFps(frameSamples(24)), 25);
  assert.equal(fpsUtils.chooseOptimalFps(frameSamples(15)), 15);
  assert.equal(fpsUtils.chooseOptimalFps(frameSamples(10)), 10);
  assert.equal(fpsUtils.chooseOptimalFps(frameSamples(5)), 5);
  assert.equal(fpsUtils.chooseOptimalFps(frameSamples(5, 12, 900)), 5);
});

test("automatic image FPS preserves GIF animation and keeps still images light", () => {
  assert.equal(fpsUtils.defaultImageFps({ type: "image/gif", name: "animation.bin" }), 30);
  assert.equal(fpsUtils.defaultImageFps({ type: "application/octet-stream", name: "ANIMATION.GIF" }), 30);
  assert.equal(fpsUtils.defaultImageFps({ type: "image/webp", name: "still.webp" }), 5);
  assert.equal(fpsUtils.defaultImageFps({ type: "image/png", name: "still.png" }), 5);
  assert.equal(fpsUtils.defaultImageFps(null), 5);
});

test("automatic FPS normalizes gaps using presentedFrames deltas", () => {
  const samples = [
    { mediaTime: 0, presentedFrames: 20 },
    { mediaTime: 1 / 30, presentedFrames: 21 },
    { mediaTime: 3 / 30, presentedFrames: 23 },
    { mediaTime: 4 / 30, presentedFrames: 24 },
    { mediaTime: 6 / 30, presentedFrames: 26 },
    { mediaTime: 7 / 30, presentedFrames: 27 }
  ];
  assert.equal(fpsUtils.chooseOptimalFps(samples), 30);
});

test("automatic FPS preserves genuine consecutive low-rate source frames", () => {
  assert.equal(fpsUtils.chooseOptimalFps(frameSamples(5)), 5);
  assert.equal(fpsUtils.chooseOptimalFps(frameSamples(10)), 10);
});

test("automatic FPS falls back when frame timing or presented-frame counters are unreliable", () => {
  assert.equal(fpsUtils.chooseOptimalFps(null), 30);
  assert.equal(fpsUtils.chooseOptimalFps([]), 30);
  assert.equal(fpsUtils.chooseOptimalFps([{ mediaTime: 0, presentedFrames: 1 }]), 30);
  assert.equal(fpsUtils.chooseOptimalFps([
    { mediaTime: 0, presentedFrames: 5 },
    { mediaTime: 0.033, presentedFrames: 4 },
    { mediaTime: 0.066, presentedFrames: 6 },
    { mediaTime: 0.099, presentedFrames: 7 }
  ]), 30);
  assert.equal(fpsUtils.chooseOptimalFps([
    { mediaTime: 0, presentedFrames: 5 },
    { mediaTime: 0.033 },
    { mediaTime: 0.066, presentedFrames: 7 },
    { mediaTime: 0.099, presentedFrames: 8 }
  ]), 30);
});

test("manual FPS accepts only supported integer values", () => {
  assert.equal(fpsUtils.normalizeExportFps("25"), 25);
  assert.equal(fpsUtils.normalizeExportFps(5), 5);
  assert.equal(fpsUtils.normalizeExportFps("23.976"), null);
  assert.equal(fpsUtils.normalizeExportFps("0"), null);
  assert.equal(fpsUtils.normalizeExportFps(""), null);
  assert.equal(fpsUtils.normalizeExportFps(null), null);
});
