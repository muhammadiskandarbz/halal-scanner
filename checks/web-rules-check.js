// Expected-results test for the Snap and stamp ingredient rules in
// "Halal Scanner - Redesigned.html". Each case gives label text and the
// expected stamp: bad (red), doubt (amber) or good (green), plus the kind
// and, where it matters, the term that must be flagged.
//
// Usage (from the project folder):  node checks/web-rules-check.js
// Updated 18 Nov 2026 for the Safe / Proceed with caution / Not safe wording and user corrections.
'use strict';
const fs = require('fs');
const path = require('path');

const html = fs.readFileSync(path.join(__dirname, '..', 'Halal Scanner - Redesigned.html'), 'utf8');
const block = html.match(/\/\* RULES START \*\/([\s\S]*?)\/\* RULES END \*\//);
if (!block) throw new Error('Rules block not found');
const Rules = new Function(block[1] + '; return Rules;')();

const L = items => 'Ingredients: ' + items;   // a complete-looking list
// [label, source, expected tone, expected kind, term that must be flagged (optional)]
const cases = [
  // ---- red: pork ----
  [L('wheat flour, sugar, lard, salt'), 'photo', 'bad', 'red', 'lard'],
  [L('pork gelatin, sugar, glucose syrup'), 'photo', 'bad', 'red', 'pork gelatin'],
  [L('sugar, gelatin (pork), citric acid'), 'photo', 'bad', 'red', 'gelatin (pork)'],
  [L('flour, porcine gelatine, water'), 'photo', 'bad', 'red', 'porcine gelatine'],
  [L('rice, bacon bits, salt'), 'photo', 'bad', 'red', 'bacon'],
  [L('bread, ham, cheese, butter'), 'photo', 'bad', 'red', 'ham'],
  [L('tepung, minyak babi, garam'), 'photo', 'bad', 'red', 'minyak babi'],
  [L('gula, lemak babi, garam'), 'photo', 'bad', 'red', 'lemak babi'],
  [L('noodles, pork extract, salt'), 'photo', 'bad', 'red', 'pork extract'],
  // ---- red: alcoholic drinks ----
  [L('beef, wine, onion'), 'photo', 'bad', 'red', 'wine'],
  [L('chocolate, sugar, rum, cocoa butter'), 'photo', 'bad', 'red', 'rum'],
  [L('soy sauce, shaoxing wine, sugar'), 'photo', 'bad', 'red', 'shaoxing wine'],
  [L('cream, sugar, brandy, eggs'), 'photo', 'bad', 'red', 'brandy'],
  [L('rice, sake, salt, sugar'), 'photo', 'bad', 'red', 'sake'],
  [L('chicken, cooking wine, ginger'), 'photo', 'bad', 'red', 'cooking wine'],
  // ---- amber: gelatin rules ----
  [L('sugar, glucose syrup, gelatin, citric acid'), 'photo', 'doubt', 'doubtful', 'gelatin'],
  [L('sugar, gelatine, starch, acid'), 'photo', 'doubt', 'doubtful', 'gelatine'],
  [L('sugar, thickener (E441), flavouring'), 'photo', 'doubt', 'doubtful', 'E441'],
  [L('sugar, beef gelatin, starch'), 'photo', 'doubt', 'doubtful', 'beef gelatin'],
  [L('sugar, bovine gelatine, colour'), 'photo', 'doubt', 'doubtful', 'bovine gelatine'],
  [L('sugar, gelatin (bovine), acid'), 'photo', 'doubt', 'doubtful', 'gelatin (bovine)'],
  // ---- amber: other doubtful ----
  [L('water, sugar, ethanol, flavouring'), 'photo', 'doubt', 'doubtful', 'ethanol'],
  [L('flour, sugar, emulsifier (E471), salt'), 'photo', 'doubt', 'doubtful', 'E471'],
  [L('flour, mono- and diglycerides of fatty acids, salt'), 'photo', 'doubt', 'doubtful'],
  [L('flour, shortening, sugar, salt'), 'photo', 'doubt', 'doubtful', 'shortening'],
  [L('milk, salt, rennet, cultures'), 'photo', 'doubt', 'doubtful', 'rennet'],
  [L('flour, water, yeast, L-cysteine'), 'photo', 'doubt', 'doubtful', 'L-cysteine'],
  [L('sugar, glycerin, water, colour'), 'photo', 'doubt', 'doubtful', 'glycerin'],
  [L('chicken, salt, pepper, onion'), 'photo', 'doubt', 'doubtful', 'chicken'],
  [L('sugar, cocoa, rum flavouring, milk'), 'photo', 'doubt', 'doubtful', 'rum flavouring'],
  [L('soy sauce, mirin, sugar, salt'), 'photo', 'doubt', 'doubtful', 'mirin'],
  // ---- green: nothing flagged ----
  [L('rice, salt, sugar, palm oil'), 'photo', 'good', 'clear'],
  [L('wheat flour, palm oil, sugar, salt, raising agents (E500, E503), soy lecithin'), 'photo', 'good', 'clear'],
  [L('sugar, fish gelatin, citric acid, colour'), 'photo', 'good', 'clear'],
  [L('fruit, sugar, pectin (E440), agar-agar, carrageenan'), 'photo', 'good', 'clear'],
  [L('water, sugar, xanthan gum, guar gum, salt'), 'photo', 'good', 'clear'],
  [L('flour, sugar, alcohol-free flavouring, salt'), 'photo', 'good', 'clear'],
  [L('vegetables, wine vinegar, salt, sugar'), 'photo', 'good', 'clear'],
  [L('sugar, sweetener (sugar alcohols), cocoa, milk'), 'photo', 'good', 'clear'],
  [L('water, vegetable glycerin, flavouring, salt'), 'photo', 'good', 'clear'],
  [L('sugar, flavouring, emulsifier (soy lecithin), salt'), 'photo', 'good', 'clear'],
  [L('flour, vegetable shortening, sugar, salt'), 'photo', 'good', 'clear'],
  ['Wheat flour, sugar, salt, palm oil', 'off', 'good', 'clear'],
  // ---- incomplete lists ----
  ['Wheat flour, sug', 'photo', 'doubt', 'incomplete'],
  ['rice', 'typed', 'doubt', 'incomplete'],
  ['', 'photo', 'doubt', 'incomplete'],
  // ---- origin is a note only ----
  [L('sugar, glucose syrup, gelatin, citric acid. Made in Germany'), 'photo', 'doubt', 'doubtful', 'gelatin'],
  [L('rice, salt, sugar, palm oil. Product of Turkey'), 'photo', 'good', 'clear'],
];

let failures = 0;
for (const [label, source, tone, kind, term] of cases) {
  const r = Rules.analyse(label, source);
  const termOk = !term || r.hits.some(h => h.tone === tone && h.term.toLowerCase() === term.toLowerCase());
  const ok = r.tone === tone && r.kind === kind && termOk;
  if (!ok) failures++;
  console.log((ok ? 'PASS ' : 'FAIL ') + tone.padEnd(6) + kind.padEnd(11) + JSON.stringify(label).slice(0, 70)
    + (ok ? '' : '\n      got ' + r.tone + '/' + r.kind + ' hits=' + JSON.stringify(r.hits.map(h => h.tone + ':' + h.term))));
}

// Notes and the halal-logo upgrade
const extra = [];
const g = Rules.analyse(L('sugar, glucose syrup, gelatin, citric acid. Made in Germany'), 'photo');
extra.push(['Germany gelatin note', g.notes.some(n => /very often pork-derived/.test(n))]);
const m = Rules.analyse(L('sugar, gelatin, citric acid, colour. Made in Malaysia'), 'photo');
extra.push(['Malaysia gelatin note', m.notes.some(n => /often halal beef or fish/.test(n)) && m.tone === 'doubt']);
const t = Rules.analyse(L('rice, salt, sugar, palm oil. Product of Turkey'), 'photo');
extra.push(['Turkey is origin, not meat', t.tone === 'good' && t.notes.some(n => /Made in Turkey/.test(n))]);
extra.push(['Logo upgrades doubtful to green', Rules.withLogo(Rules.analyse(L('sugar, gelatin, acid'), 'photo')).kind === 'logo']);
extra.push(['Logo + red term gives amber conflict, never green', (r => r.tone === 'doubt' && r.kind === 'logo-conflict' && /halal logo, but the label lists lard/.test(r.title))(Rules.withLogo(Rules.analyse(L('sugar, lard, salt'), 'photo')))]);
extra.push(['Logo never upgrades incomplete', Rules.withLogo(Rules.analyse('rice', 'typed')).kind === 'incomplete']);
extra.push(['Doubtful ingredients do not ask about logo on a partial list', Rules.analyse('gelatin', 'typed').askLogo === false]);
extra.push(['Partial Open Food Facts list asks for a photo', (r => r.kind === 'incomplete' && /Open Food Facts only has part/.test(r.reason) && /Photograph the ingredients/.test(r.next))(Rules.analyse('sugar, palm oil', 'off'))]);
extra.push(['Red result lists doubtful extras', Rules.analyse(L('flour, lard, E471, salt'), 'photo').notes.some(n => /Also uncertain: E471/.test(n))]);
for (const [name, ok] of extra) { if (!ok) failures++; console.log((ok ? 'PASS ' : 'FAIL ') + name); }

// ---- 18 Nov 2026: wording, uncertainty line, halal mention, user corrections ----
const wording = [
  ['Green says Safe', (r => r.stampBig === 'Safe' && r.stampSmall === 'No haram ingredients found' && /^Safe/.test(r.title))(Rules.analyse(L('rice, salt, sugar, palm oil'), 'photo'))],
  ['Red says Not safe – item prohibited', (r => r.stampBig === 'Not safe' && /^Item prohibited – contains lard/.test(r.stampSmall) && /^Not safe — item prohibited/.test(r.title))(Rules.analyse(L('flour, lard, salt'), 'photo'))],
  ['Amber says Proceed with caution and names the uncertainty', (r => r.stampBig === 'Proceed with caution' && r.stampSmall === 'Gelatin – source not stated' && /Proceed with caution — Gelatin – source not stated/.test(r.title))(Rules.analyse(L('sugar, gelatin, acid'), 'photo'))],
  ['Amber counts other uncertain items', (r => /\(\+1 more\)$/.test(r.stampSmall))(Rules.analyse(L('sugar, gelatin, E471, acid'), 'photo'))],
  ['Amber names E471 uncertainty', (r => r.stampSmall === 'E471 – may be animal fat')(Rules.analyse(L('flour, E471, salt, sugar'), 'photo'))],
  ['Partial list is Proceed with caution', (r => r.stampBig === 'Proceed with caution' && r.stampSmall === 'Couldn’t read the whole list')(Rules.analyse('Wheat flour, sug', 'photo'))],
  ['Red results offer the halal-logo question', Rules.analyse(L('flour, lard, salt'), 'photo').askLogo === true],
  ['Logo on a clean doubtful list says Safe', (r => r.stampBig === 'Safe' && r.stampSmall === 'Halal logo + no haram ingredients')(Rules.withLogo(Rules.analyse(L('sugar, gelatin, acid'), 'photo')))],
  ['Printed "halal" prompts the logo question', Rules.analyse(L('sugar, gelatin, acid. HALAL'), 'photo').notes.some(n => /mentions “halal”/.test(n))],
  ['"Non-halal" is not read as a halal mention', Rules.analyse(L('sugar, gelatin, acid. Non-halal'), 'photo').halalSeen === false],
  ['Phrase exception makes a misread harmless', (r => r.tone === 'good' && r.notes.some(n => /vir gin olive/.test(n)))(Rules.analyse(L('virgin olive oil, salt, garlic, pepper'.replace('virgin', 'vir gin')), 'photo', {exceptions: ['vir gin olive']}))],
  ['Without the exception the misread is red', Rules.analyse(L('vir gin olive oil, salt, garlic, pepper'), 'photo').tone === 'bad'],
  ['A single-word exception is ignored', Rules.analyse(L('flour, gin, salt, sugar'), 'photo', {exceptions: ['gin']}).tone === 'bad'],
  ['Exception does not hide the word elsewhere', Rules.analyse(L('vir gin olive oil, gin, salt, pepper'), 'photo', {exceptions: ['vir gin olive']}).tone === 'bad'],
  ['Override to Safe', (r => r.tone === 'good' && r.kind === 'override' && r.stampBig === 'Safe' && /Not safe/.test(r.next))(Rules.override(Rules.analyse(L('flour, lard, salt'), 'photo'), 'good', 'Halal certified by MUIS', '2026-11-18T10:00:00Z'))],
  ['Override to Not safe', Rules.override(Rules.analyse(L('rice, salt, sugar, oil'), 'photo'), 'bad', '', '2026-11-18').stampBig === 'Not safe'],
  ['Same label in a new photo is recognised', Rules.similar('Ingredients: wheat flour, sugar, palm oil, salt, soy lecithin', 'INGREDIENTS wheat flour sugar palm oil salt soy lecithin.') >= 0.85],
  ['Different label is not matched', Rules.similar('Ingredients: wheat flour, sugar, palm oil, salt', 'Ingredients: rice, water, chilli, garlic') < 0.85],
];
for (const [name, ok] of wording) { if (!ok) failures++; console.log((ok ? 'PASS ' : 'FAIL ') + name); }

const total = cases.length + extra.length + wording.length;
console.log(failures ? failures + ' of ' + total + ' checks failed' : 'All ' + total + ' checks passed');
process.exit(failures ? 1 : 0);
