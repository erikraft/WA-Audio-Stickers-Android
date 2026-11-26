import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const editorUtilsSource = await readFile(
  new URL("../app/src/main/assets/www/src/editor-utils.js", import.meta.url),
  "utf8"
);
const editorUtils = await import(
  "data:text/javascript;base64," + Buffer.from(editorUtilsSource).toString("base64")
);
const zipSource = await readFile(
  new URL("../app/src/main/assets/www/src/zip.js", import.meta.url),
  "utf8"
);
const zip = await import(
  "data:text/javascript;base64," + Buffer.from(zipSource).toString("base64")
);

test("10-second clips tolerate floating-point boundary noise only", () => {
  assert.equal(editorUtils.isClipDurationAllowed(0, 10), true);
  assert.equal(editorUtils.isClipDurationAllowed(0.1, 10.1), true);
  assert.equal(editorUtils.isClipDurationAllowed(0, 10.0009), true);
  assert.equal(editorUtils.isClipDurationAllowed(0, 10.002), false);
  assert.equal(editorUtils.isClipDurationAllowed(1, 1), false);
  assert.equal(editorUtils.isClipDurationAllowed(0, Number.POSITIVE_INFINITY), false);
});

test("sticker IDs increase monotonically after an item is removed", () => {
  const state = { nextId: 1, pack: [] };
  const first = editorUtils.nextStickerId(state);
  state.pack.push({ id: first });
  state.pack.splice(0, 1);
  const second = editorUtils.nextStickerId(state);
  assert.equal(first, "sticker-01");
  assert.equal(second, "sticker-02");
  assert.equal(state.nextId, 3);
});

test("sticker ID generator rejects invalid state", () => {
  assert.throws(() => editorUtils.nextStickerId({ nextId: 0 }), /positive safe integer/);
  assert.throws(() => editorUtils.nextStickerId({}), /positive safe integer/);
});

test("safe archive names strip leading underscores and remain share-safe", () => {
  assert.equal(zip.safeArchiveName("__My Pack"), "my-pack");
  assert.equal(zip.safeArchiveName("___"), "sticker-pack");
  assert.equal(zip.safeArchiveName("My Pack__"), "my-pack__");
  assert.match(zip.safeArchiveName("__audio-pack"), /^[a-z0-9][a-z0-9_-]*$/);
});
