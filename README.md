# Halal Scanner

Private Android pilot for checking packaged food and drinks in Singapore.

## Build and install

Open this directory in Android Studio with JDK 17 or newer and Android SDK 35. The project uses Android Gradle Plugin 8.9.2 and Gradle 8.11.1. With those tools available, run `gradle :app:assembleDebug`. The APK output is `app/build/outputs/apk/debug/app-debug.apk`. Use Android Studio's Run action with your Android phone connected to build and install the pilot in one step.

## Using the pilot

Scan a retail barcode for an Open Food Facts product candidate. Compare the name and variant with the pack. Open the official directory and search using the copied product name. Record a certificate only after confirming the current official listing covers the exact pack. For imported products, check the issuer and relevant category on the MUIS recognition list as well.

If certification is unverified, photograph the complete ingredient label, review the extracted text, and confirm the list. The ingredient rules cover selected English and Malay/Indonesian terms. Flagged words are shown with the result. OCR is configured for Latin script. Enter the ingredient list manually if the app can't read it.

The app uses the phone's camera app, keeps captures in temporary app storage, and deletes them after decoding. The barcode is sent to Open Food Facts. Official lookups open in the browser and require manual verification in this pilot. Certificate notes are session-only.

## Web version

**Use `Halal Scanner - Redesigned.html`**, published as a web link on GitHub Pages (see "Putting it online" below). `Halal Scanner.html` is the original, kept unchanged for comparison.

Photograph the barcode or the ingredients, and a stamp lands across your photo:

- **Red – NOT SAFE / Item prohibited** (✕): pork, lard, bacon, ham, *babi* terms, pork gelatin, and alcoholic drinks such as wine, beer, rum and sake.
- **Amber – PROCEED WITH CAUTION** (?): the stamp always says what's uncertain, e.g. "Gelatin – source not stated", "E471 – may be animal fat", or "Couldn't read the whole list".
- **Green – SAFE / No haram ingredients found** (tick): nothing flagged and the whole list was read. This is not a halal certification.

**Halal logo:**
- Tap **Yes, there is** when the pack has one.
- On an amber result with a fully read list, it gives **SAFE / Halal logo + no haram ingredients**.
- If the label also lists a prohibited ingredient, it gives **PROCEED WITH CAUTION / Halal logo, but lists …**, so you can check the certifier.

**When the app is wrong**, tap **This is wrong?** under the result. You can:
- say there's a halal logo on the pack;
- mark a misread phrase as harmless (two or more words, e.g. "vir gin olive");
- set the right stamp for that product.

The app remembers your correction the next time you scan the same barcode or label, marked "✎ Corrected by you". Every correction is logged in `CORRECTIONS.md`, where Codex or Claude Code review it and turn patterns into rules.

**Privacy:** photos stay on the phone. What leaves the phone:
- the barcode number, sent to Open Food Facts to find the product;
- your corrections (including part of the label text), sent to your GitHub repository.

Open Food Facts lists are crowd-sourced and sometimes incomplete; the app says so and asks for a photo. Full rules are in `AGENTS.md`.

### Putting it online (one-time setup, done by you)

1. Sign in to github.com (or create a free account).
2. Create a new **public** repository, for example `halal-scanner`. Anyone can see public repositories; private ones need a paid plan for Pages.
3. On the repository page, choose **Add file → Upload files**. Drag in everything from this folder except `.gradle`, `.idea` and any `build` folders, then **Commit changes**.
   - The upload page may skip the hidden `.nojekyll` file. That's fine: the site still works.
4. Go to **Settings → Pages**. Under "Build and deployment", choose **Deploy from a branch**, pick `main` and `/ (root)`, then **Save**. After a minute the site is at `https://YOUR-NAME.github.io/halal-scanner/`. Bookmark it on your phone, or use Chrome's **Add to Home screen**.
5. Create the access key:
   - Go to **Settings (your profile) → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token**.
   - Under **Repository access**, choose **Only select repositories** and pick `halal-scanner`.
   - Under **Permissions → Repository permissions → Contents**, choose **Read and write**.
   - Choose an expiry date, then generate and copy the key.
6. On your phone, open the site, then **Corrections & GitHub** on the home screen. Enter your account name, `halal-scanner` and the key, then tap **Connect**. The key is stored only in that phone's browser.

## Verification

Check the web app's ingredient rules and correction logic (Node.js required):

```sh
node checks/web-rules-check.js
```

For the Android app:

Run the verdict checks with JDK 17 or newer from this directory.

```sh
halal_check_dir="$(mktemp -d)"
javac -d "$halal_check_dir" app/src/main/java/sg/pilot/halalscan/Verdict.java checks/VerdictCheck.java
java -cp "$halal_check_dir" sg.pilot.halalscan.VerdictCheck
```

On a physical phone, test a known barcode, an unknown barcode, a similar variant, readable and blurred ingredient photos, an exact current certificate, a conflicting ingredient, and a network outage. Recheck navigation, TalkBack, and larger system text.

## Sources

- [MUIS product directory](https://halal.muis.gov.sg/halal/products)
- [MUIS guidance on imported products](https://www.muis.gov.sg/halal/fhcb/)
- [MUIS consumer guidance](https://www.muis.gov.sg/halal/for-consumers/)
- [MUIS ethanol guidance](https://www.muis.gov.sg/resources/khutbah-and-religious-advice/fatwa/ethanol--english/)
- [JAKIM halal status check](https://www.islam.gov.my/en/law-legal/halal-status-check)
- [BPJPH product directory](https://bpjph.halal.go.id/cari/sertifikat)
