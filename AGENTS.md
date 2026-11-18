# Halal Scanner — shared agent instructions

Single source of truth for any coding agent working in this folder (Codex reads this file directly; Claude Code reads it through `CLAUDE.md`). Keep it short and current. Update **Current state** at the end of every task that changes files.

## What this project is

A private shopping aid for packaged food and drinks in Singapore, used one-handed on an Android phone in a shop aisle. The user photographs a barcode or an ingredient label and gets one clear, stamped answer about whether anything in the ingredients makes the product impermissible from an Islamic standpoint. When the app is wrong, the user corrects it, the app remembers, and the correction is logged in `CORRECTIONS.md`.

| Path | Role | Rule |
|---|---|---|
| `Halal Scanner - Redesigned.html` | The app. Single self-contained file. | Edit here. |
| `index.html` | Redirects the GitHub Pages root to the app. | Keep. |
| `corrections.json` | Machine-readable corrections, written by the app via the GitHub API and read back on every device. | Don't hand-edit while the app is in use. Keep it valid JSON. |
| `CORRECTIONS.md` | Human-readable log of every correction (from the app and from chat), with status and the rule/test change. | **Absolute requirement: every correction is logged here.** Review `open` rows. |
| `checks/web-rules-check.js` | Expected-results test for the rules and correction logic. | Run after any web change. |
| `Halal Scanner.html` | Original web version. Historical baseline. | Do not edit. |
| `app/`, `checks/VerdictCheck.java` | Native Android pilot; still uses the original rules. | Out of scope unless the user asks. |

## The app (Snap and stamp, updated 18 Nov 2026)

**Flow:**
1. **Home:**
   - Two large buttons: **Photograph barcode** and **Photograph ingredients**.
   - **Type it or use a saved photo**, which opens saved-photo pickers, a barcode number field and a typed-ingredients box.
   - **Corrections & GitHub**: connection status, account / repository / access key, upload now, disconnect.
   - An offline banner appears whenever the phone has no connection.
2. **Result:**
   - The user's photo (or a drawn card for typed input) with a rubber stamp across it.
   - A solid verdict card (title, reason, next step), plus the halal-logo question where relevant and a "✎ Corrected by you" badge when a correction applied.
   - **This is wrong?**, the ingredient text with trigger words highlighted, notes, **Text wrong? Fix it and check again**, and **Scan another**.
3. **Problems** (no stamp): unreadable barcode or label, product not found, lookup failed, each with next-step buttons.

**Stamps** (wording chosen by the user, 18 Nov 2026):

| Result | Big line | Small line |
|---|---|---|
| Red | **NOT SAFE** | *Item prohibited – contains lard* |
| Amber | **PROCEED WITH CAUTION** | Always names the uncertainty, e.g. *Gelatin – source not stated*, *E471 – may be animal fat (+1 more)*, *Couldn't read the whole list*, *Ingredient list incomplete*, *No ingredient list found*, *Halal logo, but lists rum* |
| Green | **SAFE** | *No haram ingredients found*, or *Halal logo + no haram ingredients* |

Green always adds "This is not a halal certification" (or the certifier reminder) on the verdict card.

**Rules** (`RULES START … RULES END` block; pure functions, no DOM):
- **Red:** pork, porcine, pig, swine (incl. pork gelatin / extract / fat), lard, bacon, ham, pancetta, prosciutto, *babi* terms, khinzir; wine, beer, rum, vodka, whisky/whiskey, brandy, sake, liqueur, liquor, cognac, gin, champagne, cooking / rice / Shaoxing / Hua Tiao wine.
- **Amber:**
  - Gelatin or E441 with no source stated; beef / bovine gelatin.
  - Ethanol, alcohol, mirin, ginger beer, a drink named as a flavour source.
  - E471 / E472 / mono- and diglycerides; L-cysteine / E920; shortening, animal fat, tallow, suet; rennet; glycerin / E422 with no plant source; meat and poultry words.
  - Plain "flavouring" / "emulsifier" is not flagged.
- **Safe** (green highlight or ignored): fish gelatin; agar E406, pectin E440, carrageenan E407, xanthan E415, guar E412; soy / sunflower lecithin; vegetable glycerin; vegetable / palm shortening; microbial rennet; alcohol-free, non-alcoholic, pork-free, wine / beer / rice vinegar, sugar alcohols, root beer.
- **Origin** ("Made in / Product of …"): a note only, masked from matching. Adds the gelatin-origin hint. Never turns amber into green.
- **Green guard:** at least 12 characters and either an "Ingredients / Bahan / Komposisi" heading or 3+ separators (2+ for Open Food Facts). Otherwise amber.
- **Halal logo** (`withLogo`):
  - On a fully read amber list → green **SAFE / Halal logo + no haram ingredients**.
  - On a red result → amber **PROCEED WITH CAUTION / Halal logo, but lists …** (a conflict worth checking with the certifier; never green).
  - Partial or missing lists are unchanged.
  - The question is offered on red results and fully read amber results.
  - If the label text contains "halal" (not "non-halal"), a note prompts the user to check for the logo.

**Corrections** (learning):
- **Three choices under "This is wrong?":**
  1. **There's a halal logo on the pack:** a product correction. Same as tapping "Yes, there is".
  2. **A word was misread or is harmless here:** a phrase exception. It must be 2+ words (e.g. "vir gin olive"), applies to every future scan, and never hides the word elsewhere. Single words are rejected by `validException`.
  3. **The stamp should be different:** a product override to Safe / Proceed with caution / Not safe, with an optional reason.
- **Product matching:** same barcode, or (for label photos and typed text without a barcode) word-overlap similarity ≥ 0.85 (`Rules.similar`). The latest matching correction wins.
- **Storage:**
  - On the phone: `localStorage` keys `hs-corrections`, `hs-pending` and `hs-github` (the access key lives only on the phone).
  - Online: `corrections.json` plus a new row appended to `CORRECTIONS.md`. Each row ends with `<!-- id:… -->` so it is never added twice.
  - While offline, corrections are queued and upload on the `online` event or on the next app start.
  - Without a GitHub connection, the app still reads the published `corrections.json` next to the page.
- **Agent duty:** review `open` rows in `CORRECTIONS.md`. When a pattern should become a rule:
  - change the rules;
  - add a test to `checks/web-rules-check.js`;
  - set the row's status to `rule fixed` and fill in "Rule / test change".

  A product-specific fact stays `product only`.

## Hosting (approved 18 Nov 2026)

- **GitHub Pages**, from a **public** repository the user owns (the user accepted that code and the correction log are publicly visible).
- The user creates the account, repository and access key. Agents never create accounts or handle the key.
- The key is a fine-grained personal access token limited to that one repository with **Contents: read and write**, pasted by the user into the app's **Corrections & GitHub** panel.
- Online services in use: GitHub (Pages + contents API), Open Food Facts, and Tesseract.js 6.0.1 from jsDelivr. No others without approval.

## Working agreement

1. Read the request and the relevant files first. For substantive work, give a short brief (outcome, files, scope, checks) and get the user's approval before editing. Approval of a brief covers that brief only.
2. Say which kind of change it is: **UI-only** (layout, styling, motion) or **logic** (rules, network, storage, validation, timeouts). Logic needs explicit approval; the rules above are approved.
3. Photos are processed on the device and never uploaded. What leaves the phone: the barcode number (Open Food Facts) and corrections, which include up to 600 characters of label text (GitHub).
4. Photo capture/selection must start from a direct user tap. Keep stale-result protection (the `operation` counter).
5. Never insert label, Open Food Facts or correction text as HTML. Use `textContent` / text nodes. Escape `|` in `CORRECTIONS.md` cells.
6. Verdict text sits on solid surfaces next to the stamp; colour, icon and words together.
7. Honour `prefers-reduced-motion`, ≥ 44 px touch targets, no horizontal overflow from 320 px, no flashing, shake or sound. Motion code stays separate from rule code.
8. Report verification honestly. Label mocked checks as mocked; list what was not tested on a real phone.

## Checks

```sh
node checks/web-rules-check.js
```

It must print `All 75 checks passed` and exit 0. Add a case for every rule or correction-logic change.

Browser checks: serve the folder (`python3 -m http.server 8766 --bind 127.0.0.1`) and open the app. Mock `window.fetch` (Open Food Facts, `api.github.com` contents GET/PUT, `corrections.json`) and `window.BarcodeDetector`. For OCR, set `window.Tesseract` and intercept `document.head.append` for the Tesseract script. Exercise:
- every result and problem screen;
- the halal-logo conflict;
- each correction type, the remembered correction on a rescan, and a single-word exception being rejected;
- connecting with a wrong and a right key;
- an offline correction uploading after going back online;
- Home during a request, and a new scan during OCR.

## Known behaviour

- Open Food Facts data is crowd-sourced and can be partial; the green guard then shows amber "Ingredient list incomplete".
- OCR is English-trained; Malay terms are matched in text but may be misread from photos.
- The rules catch named ingredients only, not undeclared processing aids or cross-contamination.
- Two phones saving corrections at the same moment can make one upload fail (GitHub file version conflict). The correction stays queued and retries on the next sync.

## Current state

_Last updated: 18 Nov 2026, by Claude Code._

- **Built** (in `Halal Scanner - Redesigned.html`, plus `index.html`, `corrections.json` and `.nojekyll`):
  - the new wording, halal-logo conflict and "halal" mention;
  - the three correction types, remembered corrections and the offline queue;
  - GitHub sync to `corrections.json` and `CORRECTIONS.md`.
- **Checked:**
  - rules test 75/75;
  - mocked browser run of every correction and sync flow above, with a mocked GitHub API.
- **Not done yet:**
  - the user still has to create the GitHub repository, upload the files, turn on Pages and create the access key;
  - no real GitHub upload has happened.
- **Not yet tested:** real phone camera, barcode decoding of real photos, OCR of real packs, the live GitHub Pages site and real API writes.
- **`CORRECTIONS.md` #1** (halal-logo pack stamped red, 18 Nov) is open, waiting for the product name or photo.
- Git isn't configured on the user's Mac (no name or email), so files go to GitHub through the web upload page for now.
