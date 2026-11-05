// Scoring for the phone-habits check-in. Pure functions; used by index.html and the Node tests.
// This is a self-reflection aid, not a clinical instrument.
// Wrapped so its names do not collide with the page script (classic scripts share one global scope).
(function () {

const THEMES = ["checking", "sleep", "focus", "control"];

// Question i (0-based) belongs to theme THEMES[Math.floor(i / 3)].
const QUESTIONS = [
  { theme: "checking", text: "I pick up my phone without having a reason to." },
  { theme: "checking", text: "I check my phone within a few minutes of putting it down." },
  { theme: "checking", text: "I reach for my phone when there's a quiet moment, like waiting in a queue." },
  { theme: "sleep", text: "I use my phone in bed after I meant to sleep." },
  { theme: "sleep", text: "My phone is the first thing I look at when I wake up." },
  { theme: "sleep", text: "Late-night scrolling leaves me feeling tired the next day." },
  { theme: "focus", text: "Notifications pull me away from work, study or conversations." },
  { theme: "focus", text: "I switch to my phone in the middle of tasks that need concentration." },
  { theme: "focus", text: "I find it hard to read or think for long without checking my phone." },
  { theme: "control", text: "I spend longer on my phone than I meant to." },
  { theme: "control", text: "I've wanted to cut down on some apps but haven't managed to." },
  { theme: "control", text: "I feel uneasy when I can't get to my phone." },
];

const SCALE = ["Never", "Rarely", "Sometimes", "Often", "Almost always"];

const BANDS = [
  { max: 14, id: "intentional", title: "Mostly intentional",
    text: "Your phone mostly works for you. A few small habits could still be worth a look." },
  { max: 28, id: "some", title: "Some autopilot",
    text: "Some of your phone use happens on autopilot. Small changes in the moments that matter can help a lot." },
  { max: 48, id: "often", title: "Often on autopilot",
    text: "A lot of your phone use seems to happen without you choosing it. That's very common, and it can change step by step." },
];

const SUGGESTIONS = {
  checking: [
    { tip: "Keep your home screen to a few essential apps so there's less to grab your attention.",
      yoga: "Yoga's text-only home shows only the apps you choose." },
    { tip: "Add a short pause before the apps you open most on autopilot.",
      yoga: "Yoga's mindful pause gives you a few seconds to choose on purpose." },
  ],
  sleep: [
    { tip: "Charge your phone out of reach of your bed.",
      yoga: "A bedtime focus schedule tucks distracting apps away at night." },
    { tip: "Make late-night screens less rewarding.",
      yoga: "Yoga can turn the screen grey during your bedtime schedule." },
  ],
  focus: [
    { tip: "Batch notifications instead of answering each one as it arrives.",
      yoga: "Quiet notifications hold chosen apps and show them in a digest." },
    { tip: "Work in short, bounded sessions.",
      yoga: "Start a timebox or a focus session from the home screen." },
  ],
  control: [
    { tip: "Pick a daily allowance for the apps that run away with your time.",
      yoga: "Yoga shows a quiet progress line and a gentle note near your limit." },
    { tip: "Look at your week, not just today.",
      yoga: "Yoga's insights show trends, late nights and your longest breaks." },
  ],
};

function scoreAnswers(answers) {
  if (!Array.isArray(answers) || answers.length !== QUESTIONS.length) {
    throw new Error(`Expected ${QUESTIONS.length} answers`);
  }
  answers.forEach((a, i) => {
    if (!Number.isInteger(a) || a < 0 || a > 4) throw new Error(`Answer ${i + 1} must be a whole number from 0 to 4`);
  });
  const themes = { checking: 0, sleep: 0, focus: 0, control: 0 };
  answers.forEach((a, i) => { themes[QUESTIONS[i].theme] += a; });
  const total = answers.reduce((sum, a) => sum + a, 0);
  const band = BANDS.find((b) => total <= b.max);
  // Stable tie-break by theme order: checking, sleep, focus, control.
  const topThemes = THEMES.map((t, order) => ({ t, score: themes[t], order }))
    .sort((x, y) => y.score - x.score || x.order - y.order)
    .slice(0, 2)
    .map((x) => x.t);
  return { total, band: band.id, themes, topThemes };
}

const api = { THEMES, QUESTIONS, SCALE, BANDS, SUGGESTIONS, scoreAnswers };
if (typeof module !== "undefined" && module.exports) module.exports = api;
if (typeof window !== "undefined") window.HabitsScoring = api;
})();
