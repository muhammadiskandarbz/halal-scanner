# Halal Scanner — corrections log

Every time the app gets a result wrong, it is recorded here. This file is the record of truth for corrections. Codex and Claude Code both read it, review new entries, and turn patterns into rules with test cases in `checks/web-rules-check.js`.

**How entries get here**
- **From the app:** each **This is wrong?** correction (or a "Yes, there is" halal-logo answer) is saved on the phone and uploaded here automatically, together with `corrections.json`. App rows end with a hidden `<!-- id:… -->` marker so a row is never added twice. Don't remove it.
- **From chat:** when the user reports a wrong result, the agent adds a row by hand.

**Status values:** `open` (not yet reviewed) · `rule fixed` (rule changed and test added) · `product only` (remembered for that product, no rule change needed) · `kept` (the app was right; reason given).

Keep the log table as the **last thing in this file**: the app appends new rows to the end.

## Log

| # | Date | Product / barcode | App said | Correct answer | Why (user's words) | Status | Rule / test change |
|---|---|---|---|---|---|---|---|
| 1 | 2026-11-18 | Not yet given | Not permissible (red) | Pack shows a halal logo | "It returns me a not halal, when… on the packaging itself, it shows halal." Seen on a barcode and/or ingredients photo. | open | Waiting for the product name or photos to find the word that triggered red. The new halal-logo rule (logo + red → amber "Proceed with caution") now covers this case in general. |
