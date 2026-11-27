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

function frameTimes(fps, count = 12) {
  return Array.from({ length: count }, (_, index) => index / fps);
}

test("automatic FPS follows common source frame rates without exceeding the 30 FPS cap", () => {
  assert.equal(fpsUtils.chooseOptimalFps(frameTimes(60)), 30);
  assert.equal(fpsUtils.chooseOptimalFps(frameTimes(30)), 30);
  assert.equal(fpsUtils.chooseOptimalFps(frameTimes(24)), 25);
  assert.equal(fpsUtils.chooseOptimalFps(frameTimes(15)), 15);
  assert.equal(fpsUtils.chooseOptimalFps(frameTimes(10)), 10);
  assert.equal(fpsUtils.chooseOptimalFps(frameTimes(5)), 5);
  assert.equal(fpsUtils.chooseOptimalFps([0, 0.2, 0.4, 0.6]), 5);
});

test("automatic image FPS preserves GIF animation and keeps still images light", () => {
  assert.equal(fpsUtils.defaultImageFps({ type: "image/gif", name: "animation.bin" }), 30);
  assert.equal(fpsUtils.defaultImageFps({ type: "application/octet-stream", name: "ANIMATION.GIF" }), 30);
  assert.equal(fpsUtils.defaultImageFps({ type: "image/webp", name: "still.webp" }), 5);
  assert.equal(fpsUtils.defaultImageFps({ type: "image/png", name: "still.png" }), 5);
  assert.equal(fpsUtils.defaultImageFps(null), 5);
});

test("automatic FPS ignores large dropped-frame gaps when estimating cadence", () => {
  const times = [0, 1 / 30, 2 / 30, 3 / 30, 1, 1 + 1 / 30, 1 + 2 / 30, 1 + 3 / 30];
  assert.equal(fpsUtils.chooseOptimalFps(times), 30);
});

test("automatic FPS falls back safely when frame timing is unavailable or too short", () => {
  assert.equal(fpsUtils.chooseOptimalFps(null), 30);
  assert.equal(fpsUtils.chooseOptimalFps([]), 30);
  assert.equal(fpsUtils.chooseOptimalFps([0, 0.033, 0.066]), 30);
  assert.equal(fpsUtils.chooseOptimalFps([0, 0.033, Number.NaN, 0.066, 0.099]), 30);
});

test("manual FPS accepts only supported integer values", () => {
  assert.equal(fpsUtils.normalizeExportFps("25"), 25);
  assert.equal(fpsUtils.normalizeExportFps(5), 5);
  assert.equal(fpsUtils.normalizeExportFps("23.976"), null);
  assert.equal(fpsUtils.normalizeExportFps("0"), null);
  assert.equal(fpsUtils.normalizeExportFps(""), null);
  assert.equal(fpsUtils.normalizeExportFps(null), null);
});
