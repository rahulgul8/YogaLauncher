import { test } from "node:test";
import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { scoreAnswers, QUESTIONS } = require("./scoring.js");

const answers = (total) => {
  // Spread a total across 12 answers (max 4 each), filling from the first question.
  const out = Array(12).fill(0);
  let left = total;
  for (let i = 0; i < 12 && left > 0; i++) { out[i] = Math.min(4, left); left -= out[i]; }
  return out;
};

test("has twelve questions, three per theme", () => {
  assert.equal(QUESTIONS.length, 12);
  for (const theme of ["checking", "sleep", "focus", "control"]) {
    assert.equal(QUESTIONS.filter((q) => q.theme === theme).length, 3);
  }
});

test("band boundaries", () => {
  assert.equal(scoreAnswers(answers(14)).band, "intentional");
  assert.equal(scoreAnswers(answers(15)).band, "some");
  assert.equal(scoreAnswers(answers(28)).band, "some");
  assert.equal(scoreAnswers(answers(29)).band, "often");
  assert.equal(scoreAnswers(answers(48)).band, "often");
  assert.equal(scoreAnswers(answers(0)).total, 0);
});

test("theme sums and top themes", () => {
  const result = scoreAnswers([0, 0, 0, 4, 4, 4, 1, 1, 1, 2, 2, 3]);
  assert.deepEqual(result.themes, { checking: 0, sleep: 12, focus: 3, control: 7 });
  assert.deepEqual(result.topThemes, ["sleep", "control"]);
});

test("ties break in theme order", () => {
  assert.deepEqual(scoreAnswers(Array(12).fill(2)).topThemes, ["checking", "sleep"]);
  assert.deepEqual(scoreAnswers([0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1]).topThemes, ["sleep", "focus"]);
});

test("rejects invalid input", () => {
  assert.throws(() => scoreAnswers([1, 2, 3]));
  assert.throws(() => scoreAnswers(Array(12).fill(5)));
  assert.throws(() => scoreAnswers(Array(12).fill(1.5)));
  assert.throws(() => scoreAnswers("nope"));
});
