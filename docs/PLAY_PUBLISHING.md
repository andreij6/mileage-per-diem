# Publishing to Google Play: Mileage & Per Diem + Card Controls Studio

Researched **2026-11-16** from official Google sources only (Play Console Help, developer.android.com). Each rule links to the page it came from.

- **⚠ verify** means the official page didn't state the point outright, or it may change soon. Check it in Play Console before you rely on it.
- **Judgment** marks my own recommendation. It isn't Google policy.

Scope: a solo developer on a **personal** account, publishing two apps with no login, no backend, and all data kept on the device.

| App | What drives review risk |
|---|---|
| **Mileage & Per Diem** | Location runtime permissions, a `location` foreground service (FGS), optional trip auto-detect (activity recognition, and possibly background location), PDF/CSV export, bundled GSA per diem data |
| **Card Controls Studio** | It looks like a banking or card app but has no payments and no network. The risks are the Financial features declaration, Impersonation and Deceptive Behavior. |

---

## Key numbers at a glance

| Item | Value | Source |
|---|---|---|
| Registration fee | **US$25**, one time | [Get started with Play Console](https://support.google.com/googleplay/android-developer/answer/6112435) |
| Minimum age | 18 | [Get started](https://support.google.com/googleplay/android-developer/answer/6112435) |
| Closed test before production (personal accounts created after **Nov 13, 2023**) | **12 testers**, opted in **continuously for 14 days** | [App testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465) |
| Production-access review | "seven days or less", sometimes longer | [App testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465) |
| Target API for new apps and updates (from **Aug 31, 2026**) | **API 36 (Android 16)**. An extension to **Nov 1, 2026** was available. | [Target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878), [target-sdk](https://developer.android.com/google/play/requirements/target-sdk) |
| 16 KB page-size support | Updates that don't support it are blocked from **Feb 1, 2027** (applies to targetSdk 35+) | [Page sizes](https://developer.android.com/guide/practices/page-sizes) |
| Developer verification enforcement | **Sep 30, 2026** in Brazil, Indonesia, Singapore and Thailand; global in **2027** | [Developer verification](https://developer.android.com/developer-verification) |
| Location button policy (targetSdk 37+) | Effective **Jan 27, 2027** | [Policy preview](https://support.google.com/googleplay/android-developer/answer/16909972) |
| Title / short / full description | **30 / 80 / 4,000** characters | [Create and set up your app](https://support.google.com/googleplay/android-developer/answer/9859152) |
| Icon / feature graphic | **512×512** PNG, at most 1024 KB / **1024×500** | [Preview assets](https://support.google.com/googleplay/android-developer/answer/9866151) |
| Internal testing cap | 100 testers | [Testing tracks](https://support.google.com/googleplay/android-developer/answer/9845334) |
| App review time | "up to seven days or longer in exceptional cases" | [Publish your app](https://support.google.com/googleplay/android-developer/answer/9859751) |

---

## Step 1: Create the developer account

### 1a. Personal or organization

| | Personal | Organization |
|---|---|---|
| Intended for | "personal use", such as students, hobbyists and amateur developers ([source](https://support.google.com/googleplay/android-developer/answer/13634885)) | Commercial, professional or governmental activity ([source](https://support.google.com/googleplay/android-developer/answer/13634885)) |
| D-U-N-S number | Not needed | **Required.** "You can't create an organization developer account without a valid D-U-N-S number" ([source](https://support.google.com/googleplay/android-developer/answer/13628312)). Free from Dun & Bradstreet, but it can take **up to 28 days** ([verification FAQ](https://developer.android.com/developer-verification/guides/faq)). |
| Mandatory for | — | Apps that offer **financial products or services**, health apps, VpnService apps and government apps ([source](https://support.google.com/googleplay/android-developer/answer/13634885)) |
| Closed-test gate (12 testers / 14 days) | **Yes**, for accounts created after Nov 13, 2023 ([source](https://support.google.com/googleplay/android-developer/answer/14151465)) | No |
| Device verification in the Play Console app | **Yes** ([source](https://support.google.com/googleplay/android-developer/answer/14316361)) | Not described as required |
| Shown publicly on Google Play | Legal name, country and developer email. Your full address is shown if you monetize. ([source](https://support.google.com/googleplay/android-developer/answer/13628312)) | Legal name, D-U-N-S, legal address, developer email and developer phone ([source](https://support.google.com/googleplay/android-developer/answer/13628312)) |

**Recommendation for you:** a personal account. Card Controls Studio offers no financial product or service, so the organization requirement shouldn't apply. That depends on the app genuinely having no financial features (see Step 8). **Judgment.**

### 1b. Registration checklist

- [ ] Use a Google account you'll keep. You must be **18 or older** ([source](https://support.google.com/googleplay/android-developer/answer/6112435)).
- [ ] Pay the **US$25** one-time fee by credit or debit card. **Prepaid cards are not accepted** ([source](https://support.google.com/googleplay/android-developer/answer/6112435)).
- [ ] Complete identity verification. Google may ask for "a valid government ID and a credit card, both under your legal name." **The fee isn't refunded** if verification fails ([source](https://support.google.com/googleplay/android-developer/answer/6112435)).
- [ ] Provide: developer name, legal name, legal address, contact email, contact phone, developer email, and trader status where it applies ([source](https://support.google.com/googleplay/android-developer/answer/13628312)).
- [ ] Verify every contact detail and public detail with a one-time password. These channels "must remain operational for the duration of your developer account" ([source](https://support.google.com/googleplay/android-developer/answer/13628312)).
- [ ] If you'll monetize later, verify a payment method. "Verification can take up to five days" ([source](https://support.google.com/googleplay/android-developer/answer/13628312)). Not needed for free apps. **⚠ verify** in your Console.
- [ ] Use a developer email you're comfortable making **public**, because it appears on your store listing ([source](https://support.google.com/googleplay/android-developer/answer/13628312)).

### 1c. Device verification (personal accounts)

"Personal account owners must verify access to an Android device using the Play Console mobile app before publishing apps" ([source](https://support.google.com/googleplay/android-developer/answer/6112435)).

- [ ] Use a **non-rooted, physical** Android device running **Android 10 or higher** ([source](https://support.google.com/googleplay/android-developer/answer/14316361)).
- [ ] In the web Play Console Home page, open the task "Verify that you have access to an Android mobile device", then scan the QR code ([source](https://support.google.com/googleplay/android-developer/answer/14316361)).
- [ ] In the Play Console app, sign in as the account owner, select the account, and tap **Verify**. It takes less than a minute. The phone number doesn't need to match the one you registered with ([source](https://support.google.com/googleplay/android-developer/answer/14316361)).

### 1d. Android developer verification program (announced 2025, rolling out 2026)

- **What it is:** identity verification plus **package-name registration** for apps on certified Android devices, whether or not they're distributed on Play ([developer.android.com/developer-verification](https://developer.android.com/developer-verification)).
- **Timeline** ([source](https://developer.android.com/developer-verification)):
  - **August 2026:** developer APIs, limited distribution accounts and the power-user "advanced flow" launched.
  - **Sep 30, 2026:** protections began for installs from participating stores in **Brazil, Indonesia, Singapore and Thailand** on certified devices running Android 7+.
  - **2027:** global expansion.
- **What it means for Play developers:**
  - The Play Console is the single place to manage verification, including for apps distributed outside Play ([FAQ](https://developer.android.com/developer-verification/guides/faq)).
  - "Google Play automatically registers 99% of apps" ([source](https://developer.android.com/developer-verification)).
  - Apps that use **Play App Signing are claimed automatically** ([FAQ](https://developer.android.com/developer-verification/guides/faq)).
  - Registration status appears on the Play Console's *Android developer verification* page ([FAQ](https://developer.android.com/developer-verification/guides/faq)).
- **Limited distribution account:** for hobbyists sharing with up to **20 devices**. It needs no ID and has no fee. It isn't needed if you publish on Play ([FAQ](https://developer.android.com/developer-verification/guides/faq)).
- **Sideloading your own builds:** **ADB installs don't require verification** ([FAQ](https://developer.android.com/developer-verification/guides/faq)).
- [ ] After your first upload, open Play Console's *Android developer verification* page and confirm both package names show as **Registered**. **⚠ verify:** the page's exact location in the Console UI.
- [ ] **Don't lose your keys.** "If you lose your signing key you won't be able to register your packages" ([FAQ](https://developer.android.com/developer-verification/guides/faq)). Play App Signing keeps the app signing key with Google (Step 3).

---

## Step 2: Clear the new-personal-account testing gate (both apps)

Rules from [App testing requirements for new personal developer accounts](https://support.google.com/googleplay/android-developer/answer/14151465), re-checked 2026-11-16 and **still 12 testers / 14 days**:

- At least **12 testers** must be opted in to your **closed** test when you apply.
- They must have been opted in **continuously for the preceding 14 days**. A tester who opts out and back in restarts their count. Testers who leave before 14 days don't count.
- **Production** and **Pre-registration** stay locked until you meet the requirement.
- Google may make you keep testing if you had too few testers "or insufficient tester engagement".
- **Per app?** The page ties the gate to "their app", and you apply for production access from a single app's Dashboard. Plan on **one closed test per app**. **⚠ verify:** the page doesn't say explicitly whether the gate is per app or per account.

**Checklist**

- [ ] Recruit **at least 12 real people**. Judgment: aim for 15–20 for buffer. Each needs a Google account and an Android device.
- [ ] Create a **Closed testing** track. Add testers by email list (up to 2,000 per list) or a Google Group (no size limit) ([testing tracks](https://support.google.com/googleplay/android-developer/answer/9845334)). **⚠ verify** that Google-Group members count toward the 12 (not stated).
- [ ] Publish the closed release and share the **opt-in link**. It only appears once status is "Published" and can take several hours ([source](https://support.google.com/googleplay/android-developer/answer/9845334)).
- [ ] Ask testers to opt in, install, and **actually use** the features (engagement is judged).
- [ ] Collect feedback through something you can summarize, such as a form or group chat.
- [ ] Ship at least one improvement during the test so you can answer "what did you change?"
- [ ] On day 15 or later: **Dashboard → Apply for production**.

**Questions on the production-access application** ([source](https://support.google.com/googleplay/android-developer/answer/14151465)):

1. *About your closed test:*
   - How easy was it to recruit testers?
   - Did testers use all the features?
   - How did tester usage compare with expected production usage?
   - Summarize the feedback and how you collected it.
2. *About your app:*
   - Who is the target audience?
   - What is the value proposition?
   - What is your estimated install range for the first year?
3. *Production readiness:*
   - What did you change based on the test?
   - How did you decide the app was ready?

Google says the review usually takes **"seven days or less"**.

**Timing tip:** run both apps' closed tests **at the same time**. They're independent 14-day clocks. **Judgment.**

---

## Step 3: Build requirements (target API, 16 KB, AAB, signing)

### Target API level

- **Since Aug 31, 2026:** new apps and updates must target **Android 16 (API 36)** or higher. An extension to **Nov 1, 2026** could be requested and has now passed ([Play Help](https://support.google.com/googleplay/android-developer/answer/11926878), [developer.android.com](https://developer.android.com/google/play/requirements/target-sdk)).
- Existing apps must target **API 35+** to stay available to new users on newer Android versions ([source](https://support.google.com/googleplay/android-developer/answer/11926878)).
- **Next deadline:** neither page I fetched announces the 2027 date or level. By the annual pattern it would be API 37 around Aug 31, 2027. **⚠ verify:** that is extrapolation, not sourced.
- **Already announced for targetSdk 37:**
  - The **location button** policy takes effect **Jan 27, 2027**.
  - Using `ACCESS_FINE_LOCATION` will need a **Play Console declaration** ([preview](https://support.google.com/googleplay/android-developer/answer/16909972), [policy](https://support.google.com/googleplay/android-developer/answer/16585319)).
  - This matters for the Mileage app when you move to API 37 (see Step 6).

### 16 KB memory page size

- All apps targeting **API 35+** must support 16 KB pages on 64-bit devices. **From Feb 1, 2027**, updates that don't support it can't be released ([source](https://developer.android.com/guide/practices/page-sizes)).
- "If your app only uses code written in the Java programming language or in Kotlin, including all libraries or SDKs, then your app already supports 16 KB devices" ([source](https://developer.android.com/guide/practices/page-sizes)).
- [ ] Check for `.so` files with **Build → Analyze APK** and look in `lib/`. If there's no `lib/` folder, you're done ([source](https://developer.android.com/guide/practices/page-sizes)).
- [ ] If native libraries exist, for example from a PDF library, use **AGP 8.5.1+** and **NDK r28+**, then verify with `zipalign -c -P 16 -v 4` ([source](https://developer.android.com/guide/practices/page-sizes)).

### Android App Bundle (AAB) and Play App Signing

- "From August 2021, new apps are required to publish with the Android App Bundle on Google Play" ([source](https://developer.android.com/guide/app-bundle)).
- New apps are **enrolled in Play App Signing by default**. Google holds the **app signing key**, and you sign uploads with your **upload key** ([source](https://support.google.com/googleplay/android-developer/answer/9842756)).
- The upload key "must be an RSA key of 2048 bits or more" ([source](https://support.google.com/googleplay/android-developer/answer/9842756)).
- If you lose the upload key, you can request a reset from the Play App Signing page ([source](https://support.google.com/googleplay/android-developer/answer/9842756)).
- Creating an app requires accepting the Developer Program Policies, US export laws and **Play App Signing Terms of Service** ([source](https://support.google.com/googleplay/android-developer/answer/9859152)).

**Checklist (per app)**

- [ ] Set `targetSdk = 36` (or higher) and `compileSdk` to match.
- [ ] Generate an upload keystore (RSA 2048+). Back it up outside the repo.
- [ ] Build a signed release with `./gradlew bundleRelease` to produce an `.aab`.
- [ ] Choose a final `applicationId`. You can't change it after upload, and it's what gets registered for developer verification.
- [ ] Test on Android 13, 14 and 16 devices or emulators, including POST_NOTIFICATIONS denial and location denial.

---

## Step 4: Privacy policy (required for both apps)

- **Every app needs one**, even with no data collection: "Apps that do not access any personal and sensitive user data must still submit a privacy policy" ([User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311)).
- It must be linked **in Play Console** and accessible **inside the app** ([source](https://support.google.com/googleplay/android-developer/answer/10144311)).
- **Hosting rules** ([Prepare your app for review](https://support.google.com/googleplay/android-developer/answer/9859455)):
  - An active, public URL with a **valid SSL certificate**.
  - Accessible worldwide with **no geofencing or login**.
  - A standard **non-editable web page, not a PDF** or downloadable file.
- **Required content** ([source](https://support.google.com/googleplay/android-developer/answer/10144311)):
  - Developer information and a privacy contact.
  - The types of data accessed, collected, used and shared, and who receives it.
  - Secure handling procedures.
  - Your retention and deletion policy.
  - Clearly labeled as a privacy policy, and naming the developer or app shown in the store listing.

**Checklist**

- [ ] Write one HTML page per app, or one page with a section per app, naming each app.
- [ ] Host it on a static HTTPS host such as **GitHub Pages**. That meets the URL/SSL/no-login/not-PDF rules above as long as the repo is public. **Judgment.**
- [ ] For the **Mileage** policy, cover:
  - Precise location is read on-device only to record trips.
  - Trips are stored only on the device and never sent to the developer.
  - Exports leave the device only when the user shares them.
  - Uninstalling or clearing app data deletes everything.
  - No account, no analytics, no ads. Only claim this if it's true, including for every SDK.
- [ ] For **Card Controls**, cover:
  - The app collects no data and has no network access.
  - All cards are fictional.
- [ ] Add a "Privacy policy" link in each app's Settings/About screen.

---

## Step 5: Data safety form (both apps)

Required for apps on closed, open or production tracks. Apps that are **only** on internal testing are exempt ([source](https://support.google.com/googleplay/android-developer/answer/10787469)). Even apps that collect nothing must complete it ([source](https://support.google.com/googleplay/android-developer/answer/10787469)).

**Key definitions** ([source](https://support.google.com/googleplay/android-developer/answer/10787469)):

| Term | Meaning | Effect for you |
|---|---|---|
| **Collect** | "transmitting data from your app off a user's device" | Location that stays on the device is **not collected** |
| On-device only | Data "only processed locally on the user's device and not sent off device" doesn't need to be disclosed | Trip logs and GPS points stay off the form |
| **Ephemeral** | Data accessed and used only in memory, for the specific request, without being stored | Not relevant if everything stays on the device |
| **Share** | Transferring user data to a third party, including on-device transfers to another app | Exempt when it's a "specific user-initiated action, where the user reasonably expects the data to be shared" |
| Approximate vs precise location | Approximate covers an area of 3 km² or more. Precise is under 3 km². | Applies only if you ever transmit location |

**Expected answers (only if true for every library you ship):**

| Question | Mileage & Per Diem | Card Controls Studio |
|---|---|---|
| Does your app collect or share any required user data types? | **No** | **No** |
| Encrypted in transit | Not applicable (nothing collected) | Not applicable |
| Account creation / deletion | Select the "app doesn't allow account creation" option. The [account deletion requirement](https://support.google.com/googleplay/android-developer/answer/13327111) applies only to apps that let users create an account. | Same |

- **Export caveat:** a user sharing a PDF or CSV through the share sheet should fall under the user-initiated sharing exemption. The Help page doesn't address the share sheet directly. **⚠ verify.**
- **SDK caveat:** some libraries collect or transmit data on their own, such as crash reporters, analytics, maps tiles or font downloads. "No data collected" is only true if **no dependency** transmits anything. Audit the merged manifest for `INTERNET`. The page's definition covers transfers made by your libraries and SDKs ([source](https://support.google.com/googleplay/android-developer/answer/10787469)).
- [ ] **Card Controls:** remove the `INTERNET` permission entirely. That backs up the "no network" claim and the demo positioning. **Judgment.**

---

## Step 6: Location permissions (Mileage & Per Diem)

### 6a. What counts as foreground and what triggers background review

- **Android's definition** ([request location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions)): an app is using **foreground** location when:
  - one of its activities is visible, **or**
  - it is running a foreground service (FGS). In that case the app "retains access when it's placed in the background, such as when the user presses the Home button … or turns their device's display off."
- **Background location** is any other access. It requires `ACCESS_BACKGROUND_LOCATION` on Android 10+ ([source](https://developer.android.com/develop/sensors-and-location/location/permissions)).
- **Play's background-location review is triggered by `ACCESS_BACKGROUND_LOCATION`.** Location access is declared under App content → Sensitive app permissions → Location permissions, and background location "require[s] approval through the permission declaration process" ([source](https://support.google.com/googleplay/android-developer/answer/9799150)).
- **But Play also polices FGS location.** With only foreground access, FGS location is allowed if it "has been initiated as a continuation of an in-app user-initiated action" **and** "is terminated immediately after the intended use case of the user-initiated action is completed" ([Permissions policy](https://support.google.com/googleplay/android-developer/answer/16585319)). If your FGS use is effectively background access, the background rules apply ([source](https://support.google.com/googleplay/android-developer/answer/9799150)).

**Answer to your question:** yes. A `location` FGS started from a **visible activity** after the user taps **Start trip** is foreground access. It doesn't need `ACCESS_BACKGROUND_LOCATION`, and it keeps working with the screen off. You still need the **FGS declaration** (Step 7).

### 6b. Trip auto-detection: what forces background location

| Design | Needs `ACCESS_BACKGROUND_LOCATION`? | Why |
|---|---|---|
| **A. Manual Start/Stop trip** (FGS started from the visible app) | **No** | Matches the definition above |
| **B. Activity Recognition detects driving → post a notification "Drive detected, tap to log" → tap opens the app → user starts the trip** | **No** (Judgment) | Activity Recognition transitions may start some FGS from the background, but a `location` FGS needs while-in-use permissions and "cannot *create*" from the background ([restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)). Starting from a **notification interaction** is a listed while-in-use exemption ([same page](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)). Opening an activity and starting from there is safest for Play's "in-app user-initiated" rule. **⚠ verify** whether a reviewer accepts the notification-tap path. |
| **C. Fully automatic: start logging with no user tap** | **Yes** | Without background location, creating a `location` FGS from the background throws `SecurityException` on targetSdk 34+ ([source](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start), [service types](https://developer.android.com/develop/background-work/services/fgs/service-types)). Play also names "monitoring when a user starts or stops driving" as outside the navigation foreground exception ([source](https://support.google.com/googleplay/android-developer/answer/9799150)). Expect a full background-location review. |

**Recommendation: ship A, optionally with B. Avoid C for v1.** **Judgment.**

Google lists "physical safety, perceived safety, and health/fitness" as *significant* benefits and "convenience" as a *minimal* one ([source](https://support.google.com/googleplay/android-developer/answer/9799150)). Automatic mileage logging is easy for a reviewer to classify as convenience. **⚠ verify:** no official page mentions mileage apps by name.

### 6c. Fine vs coarse, approximate location on Android 12+

- Always declare `ACCESS_COARSE_LOCATION`. Add `ACCESS_FINE_LOCATION` only if you benefit from precision ([source](https://developer.android.com/develop/sensors-and-location/location/permissions)). On Android 12+ "you must request the `ACCESS_COARSE_LOCATION` permission any time you request `ACCESS_FINE_LOCATION`" ([source](https://developer.android.com/google/play/requirements/target-sdk)).
- The user can grant **approximate only**. Approximate is about **3 km²**, and precise is usually within about **50 m** ([source](https://developer.android.com/develop/sensors-and-location/location/permissions)).
- Your app "should still work when the user grants only approximate location" ([source](https://developer.android.com/develop/sensors-and-location/location/permissions)). For mileage, show "Precise location needed for accurate distance" and offer manual trip entry as a fallback.
- Play asks for "the minimum scope necessary … coarse instead of fine, foreground instead of background" ([source](https://support.google.com/googleplay/android-developer/answer/9799150)).
- **Coming with targetSdk 37 (effective Jan 27, 2027):** `ACCESS_FINE_LOCATION` will require a Play Console declaration ([source](https://support.google.com/googleplay/android-developer/answer/16585319)).

### 6d. Prominent disclosure and consent

Required for background location ([source](https://support.google.com/googleplay/android-developer/answer/9799150)), and generally whenever data use may go beyond what users reasonably expect ([User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311)). Do it for foreground trip tracking as well. **Judgment.**

**Exact rules** ([prominent disclosure guide](https://support.google.com/googleplay/android-developer/answer/11150561), [User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311), [background location](https://support.google.com/googleplay/android-developer/answer/9799150)):

- [ ] **Placement:**
  - Show it in the app during normal use, **immediately before** the runtime permission prompt.
  - Not only in the store listing, a website, a settings menu, or the privacy policy or terms.
  - Don't bundle it with unrelated disclosures.
- [ ] **Content:** cover *what* data, *why* (which core feature), and *how* it's used or shared. Use plain language at about a **13-year-old reading level**.
- [ ] **Background location, if you ever request it:**
  - Include the word **"location"** and a phrase such as **"when the app is closed"** or **"not in use"**.
  - List **every** feature that uses it.
  - Suggested template: "collects location data to enable [feature] even when the app is closed or not in use."
- [ ] **Look:** it must not imitate Android system dialogs. Use your app's own theme.
- [ ] **Consent:**
  - Use an affirmative button such as **"Agree"**. Avoid "Allow access" and "Got it".
  - Also offer a decline option such as **"Not now"**.
  - Backing out, tapping outside, or auto-dismissing does **not** count as consent.
- [ ] Don't collect any data until the user consents. If they decline, degrade gracefully: disable auto-tracking and keep manual entry working.

### 6e. Background location declaration and video (only if you choose design C)

From [Understanding location in the background permissions](https://support.google.com/googleplay/android-developer/answer/9799150):

- [ ] Go to **App content → Sensitive app permissions → Location permissions**.
- [ ] Describe **one** background-location feature. Listing several gets the declaration rejected. Explain why it can't work without background access.
- [ ] **Video:**
  - Show the **prominent disclosure**, the **runtime prompt**, and the feature **working from the background**.
  - Google recommends **30 seconds or less**.
  - A **YouTube link is preferred**. A Google Drive link to an MP4 is also accepted.
  - It must show the Android app.
- [ ] Per the disclosure guide, the video should also show the app opening, the full disclosure text, both the accept and the decline flows, and the core feature ([source](https://support.google.com/googleplay/android-developer/answer/11150561)).
- Without approval, updates may be blocked and the app removed ([source](https://support.google.com/googleplay/android-developer/answer/9799150)).
- Review duration for this declaration: not stated. **⚠ verify.**

---

## Step 7: Foreground service type and Play FGS declaration (Mileage)

### Manifest (Android 14+)

From [FGS service types](https://developer.android.com/develop/background-work/services/fgs/service-types):

- Since Android 14 (API 34), you must declare a type for every FGS. Each type also needs its matching permission, on top of `FOREGROUND_SERVICE`.
- For **`location`**:
  - Declare `android:foregroundServiceType="location"` on the `<service>`.
  - Add the `FOREGROUND_SERVICE_LOCATION` manifest permission.
  - Pass `FOREGROUND_SERVICE_TYPE_LOCATION` to `startForeground()`.
  - Runtime prerequisite: location services are on **and** `ACCESS_COARSE_LOCATION` or `ACCESS_FINE_LOCATION` is granted.
  - You can't create it while the app is in the background unless you hold `ACCESS_BACKGROUND_LOCATION`.

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<!-- Only for design B (activity transitions). Add the gms permission too if minSdk <= 28: -->
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<!-- Do NOT add ACCESS_BACKGROUND_LOCATION unless you choose design C -->

<service android:name=".trip.TripTrackingService"
         android:foregroundServiceType="location"
         android:exported="false" />
```

### Play policy for FGS

From [Device and Network Abuse → Foreground Service Permissions](https://support.google.com/googleplay/android-developer/answer/9888379). The FGS must:

- provide "a feature that is beneficial to the user and relevant to the core functionality of the app"
- be "initiated by the user or … user perceptible"
- be something the user can stop
- be work that can't be deferred without harming the user
- run "only for as long as necessary".

### Play Console FGS declaration (App content)

Apps targeting Android 14+ declare each FGS type ([source](https://support.google.com/googleplay/android-developer/answer/13392821)). Checklist:

- [ ] **Description** of the feature that uses `location`. For example: "Records the route and distance of a business trip the user starts from the Trips screen, so the trip can be exported as a mileage expense."
- [ ] **User impact** if the system defers or interrupts it. For example: "Distance would be missing or wrong, so the expense report would be inaccurate."
- [ ] **Use case:** the preset list for location includes:
  - *User-initiated location sharing*, with the example "vehicle activity tracking"
  - *Navigation*, with the example "ride-share tracking"
  - *Geofencing*

  Navigation or user-initiated tracking fits best. **Judgment.**
- [ ] **Video link** showing "the steps the user needs to take in your app in order to trigger the feature". For location, it should show "the user initiating active navigation or ride tracking" ([source](https://support.google.com/googleplay/android-developer/answer/13392821)).
  - Script: open app → disclosure → permission prompts → tap **Start trip** → notification appears → press Home or turn the screen off → drive or simulate → tap **Stop** → trip saved.
  - Keep it short. No length is specified for this video. **⚠ verify.**
  - An unlisted YouTube link is the usual format. **⚠ verify:** the page doesn't state an accepted host.
- [ ] **Stop the FGS** as soon as the trip ends. That's required by the policy's "terminated immediately" rule ([source](https://support.google.com/googleplay/android-developer/answer/16585319)).

---

## Step 8: Other runtime permissions and declarations

### POST_NOTIFICATIONS (Android 13+)

- "Apps don't need to request the `POST_NOTIFICATIONS` permission in order to launch a foreground service. However, apps must include a notification when they start a foreground service" ([source](https://developer.android.com/develop/ui/views/notifications/notification-permission)).
- If the user denies it, FGS notices still appear in the **Task Manager** but **not in the notification drawer** ([source](https://developer.android.com/develop/ui/views/notifications/notification-permission)).
- [ ] Request it in context, for example on the first **Start trip**, so the user sees the persistent "Trip in progress" notification.
- [ ] Design B depends on the "Drive detected" notification. If the permission is denied, explain that auto-detect needs it.

### ACTIVITY_RECOGNITION (design B only)

- Android 10+ restricts physical-activity data behind the `ACTIVITY_RECOGNITION` runtime permission ([source](https://developer.android.com/google/play/requirements/target-sdk)).
- The Transition API needs `play-services-location` and the `com.google.android.gms.permission.ACTIVITY_RECOGNITION` manifest permission ([source](https://developer.android.com/develop/sensors-and-location/location/transitions)).
- Google's own example: "a mileage tracking app could start tracking miles when a user starts driving" ([source](https://developer.android.com/develop/sensors-and-location/location/transitions)).
- [ ] Request it at runtime, with its own short explanation shown first.
- [ ] Data safety: activity data that stays on the device isn't "collected" ([source](https://support.google.com/googleplay/android-developer/answer/10787469)).
- [ ] **Health apps declaration:** see below.

### App content declarations (both apps)

| Declaration | Who must complete it | Mileage | Card Controls |
|---|---|---|---|
| **Privacy policy** | All apps ([source](https://support.google.com/googleplay/android-developer/answer/9859455)) | URL | URL |
| **Ads** | All apps: yes or no, counting SDK ads ([source](https://support.google.com/googleplay/android-developer/answer/9859455)) | No | No |
| **App access** (sign-in details) | Only if part of the app is restricted ([source](https://support.google.com/googleplay/android-developer/answer/9859455)) | All functionality available without restrictions | Same |
| **Content rating** (IARC questionnaire) | Every app. Unrated apps may be removed ([source](https://support.google.com/googleplay/android-developer/answer/9898843)) | Complete it, likely a low rating | Same. Answer "no" on real-money gambling or purchases honestly |
| **Target audience and content** | All apps. Including any under-13 group triggers the **Families Policy** ([source](https://support.google.com/googleplay/android-developer/answer/9867159)) | **18 and over**. It's a business-expense tool. **Judgment.** | **18 and over** recommended so a card UI isn't aimed at kids. **Judgment.** |
| **Data safety** | Closed, open and production tracks ([source](https://support.google.com/googleplay/android-developer/answer/10787469)) | Step 5 | Step 5 |
| **Financial features** | **All developers**, including on testing tracks. Apps without financial features choose **"My app doesn't provide any financial features"** ([source](https://support.google.com/googleplay/android-developer/answer/13849271)) | "Doesn't provide". It records expenses and moves no money. **⚠ verify** whether expense tracking fits any listed category; none matches. | **"Doesn't provide"**. It's a UI demo with no payments, wallet or banking. |
| **Health apps** | **All developers**. Apps without health features choose "My app doesn't provide any health features" ([source](https://support.google.com/googleplay/android-developer/answer/14738291)) | "Doesn't provide" if Activity Recognition is used only to detect driving. **⚠ verify:** "Activity and Fitness" covers apps that "monitor and record physical activities", and a trip log isn't fitness. | "Doesn't provide" |
| **Government apps** | Required since Jan 31, 2023 ([search summary of Play Help](https://support.google.com/googleplay/android-developer/answer/9514050)) | **Not a government app.** The GSA per diem data is public, but say you're **not affiliated with GSA** (see Impersonation). | Not a government app |
| **News and magazine** | Only apps that call themselves news or magazine apps, or sit in that category ([source](https://support.google.com/googleplay/android-developer/answer/16189314)) | No | No |
| **Foreground service** | Apps targeting 14+ that use FGS ([source](https://support.google.com/googleplay/android-developer/answer/13392821)) | Step 7 | None |
| **Location permissions** | Background location, and soon fine location on targetSdk 37+ ([source](https://support.google.com/googleplay/android-developer/answer/9799150)) | Only for design C | None |
| **Account deletion** | Only apps that allow account creation ([source](https://support.google.com/googleplay/android-developer/answer/13327111)) | N/A | N/A |

**Content rating cost:** the page doesn't say it's free ([source](https://support.google.com/googleplay/android-developer/answer/9898843)). It's a questionnaire in Play Console. **⚠ verify.**

---

## Step 9: Card Controls Studio, financial look-alike risk

**Policies that apply:**

- **Impersonation:**
  - "Don't imply that your app is related to or authorized by someone that it isn't."
  - Violations include copying a business's logo to suggest an official app ([Impersonation](https://support.google.com/googleplay/android-developer/answer/9888374)).
- **Deceptive Behavior:**
  - Metadata must accurately describe functionality.
  - "Apps that claim functionalities that are not possible to implement" are banned "even if it is represented as a prank, fake, joke, etc."
  - Apps must not "mimic other apps or websites to trick users into disclosing personal or authentication information" ([Deceptive Behavior](https://support.google.com/googleplay/android-developer/answer/17006354)).
- **Metadata:** don't use a brand's logo without permission ([Metadata](https://support.google.com/googleplay/android-developer/answer/9898842)).
- **Financial features declaration:** declare none ([source](https://support.google.com/googleplay/android-developer/answer/13849271)). If the app *did* offer financial services, a personal account wouldn't be allowed ([source](https://support.google.com/googleplay/android-developer/answer/13634885)).

**How to make the demo obvious.** These items are **Judgment**, derived from the policies above:

- [ ] Put **"Demo"** or **"Concept"** in the title, within 30 characters. Example: "Card Controls Studio (Demo)" is 27 characters.
- [ ] Start the short description with it. Example: "UI concept demo. Fictional cards, no real payments, no network."
- [ ] Make the first paragraph of the full description state:
  - all cards are fictional
  - the app can't connect to any bank or card
  - it never asks for real card numbers.
- [ ] **No real bank, card-network or fintech names or logos** anywhere: no Visa, Mastercard, Amex, Ramp or bank marks. Use your own invented brand mark.
- [ ] **No input fields that accept a real PAN, CVV, PIN or bank login.** Generate fake numbers that obviously can't be real, for example 4000 0000 0000 0000 marked "DEMO". The deceptive-behavior ban on collecting credentials is the main risk.
- [ ] Show a persistent **"DEMO, not a real card"** watermark on every card and on screenshots.
- [ ] Add a first-launch screen: "This is a design demo. No real money moves." Keep it reachable later from About.
- [ ] **Remove the `INTERNET` permission.** It backs up the "no network" claim.
- [ ] Category: choose something like **Tools**, **Productivity** or **Art & Design** instead of **Finance**, so reviewers and users don't expect a real financial service. **⚠ verify:** no official page requires a particular category. This is a precaution.
- [ ] Don't mention a real employer or product in the name or icon. A portfolio README can say "inspired by", but the store listing shouldn't.

---

## Step 10: Store listing assets (both apps)

From [Create and set up your app](https://support.google.com/googleplay/android-developer/answer/9859152) and [Preview assets](https://support.google.com/googleplay/android-developer/answer/9866151):

| Asset | Requirement |
|---|---|
| App name | at most **30** characters |
| Short description | at most **80** characters. Required. |
| Full description | at most **4,000** characters |
| App icon | **512 × 512**, 32-bit PNG with alpha, at most **1024 KB** |
| Feature graphic | **1024 × 500**, JPEG or 24-bit PNG with no alpha |
| Screenshots | JPEG or 24-bit PNG with no alpha. **320–3840 px** per side, and the long side at most **2×** the short side. Up to **8 per device type**, and **at least 2** to publish. |
| Screenshots for promotion eligibility | **4 or more** at a minimum of 1080 px, for example 1080×1920 portrait |
| Preview video (optional) | Public or unlisted YouTube URL with no ads/monetization, not age-restricted, embeddable. A direct video link, not a playlist. Only the first **30 s** autoplays. |
| Contact email | **Required** |
| Website / phone | Optional |

**Metadata rules** ([Metadata policy](https://support.google.com/googleplay/android-developer/answer/9898842)):

- [ ] In the title, icon and developer name: no emojis or repeated special characters, and no ALL CAPS unless it's your brand.
- [ ] No ranking, performance, price or Play-program claims, such as "#1", "Best of Play", "Popular", "App of the year", "free for limited time", "Editor's choice" or "New".
- [ ] No "best …" style superlatives. "best game ever" is given as a deceptive example.
- [ ] No anonymous testimonials. No keyword lists or repetition.
- [ ] Screenshots and video must reflect the real app experience.
- [ ] **Mileage:**
  - Don't imply GSA or IRS endorsement.
  - Say per diem rates are "based on public GSA data for FY20XX".
  - Explain that users should verify rates with their employer.
  - **Judgment**, derived from the [Impersonation](https://support.google.com/googleplay/android-developer/answer/9888374) rules on government affiliation.

---

## Step 11: Release process

### Tracks

From [testing tracks](https://support.google.com/googleplay/android-developer/answer/9845334):

| Track | Size | Notes |
|---|---|---|
| **Internal** | at most **100** testers | Available within minutes. May skip standard policy review. Exempt from Data safety. |
| **Closed** | Email lists of up to 2,000 each, or Google Groups (no limit) | **This is where the 12/14 gate runs** |
| **Open** | Unlimited, or capped at **1,000 or more** | Public on Play. Available after production access. |
| **Production** | Everyone | Locked until production access is granted (Step 2) |

### Flow (per app)

1. **Internal:** upload the AAB and smoke-test on your own device. Read the **pre-launch report**.
   - It's generated automatically on upload and usually takes about an hour.
   - It tests stability, compatibility, performance and accessibility on Android 9+ devices.
   - No credentials are needed because there's no login ([source](https://support.google.com/googleplay/android-developer/answer/9842757)).
2. **Complete every App content declaration** (Steps 4–8) and the store listing. Errors block publishing ([source](https://support.google.com/googleplay/android-developer/answer/9859751)).
3. **Closed:** roll out and recruit 12 or more testers. Wait **14 continuous days** (Step 2).
4. **Apply for production.** Allow about 7 days.
5. **Production:**
   - Release to 100%. "Staged rollouts can only be used for app updates, not when publishing an app for the first time" ([source](https://support.google.com/googleplay/android-developer/answer/6346149)).
   - For later updates, choose a % rollout, raise it manually, and **halt** it if vitals spike ([source](https://support.google.com/googleplay/android-developer/answer/6346149)).
6. Optionally turn on **Managed publishing** to control when approved changes go live ([source](https://support.google.com/googleplay/android-developer/answer/9859751)).

### Review times

- "up to seven days or longer in exceptional cases". Some accounts and apps get extended review ([source](https://support.google.com/googleplay/android-developer/answer/9859751)).
- Submitting new changes while others are in review can reset the queue, so leave a buffer of at least a week ([control publishing](https://support.google.com/googleplay/android-developer/answer/9859654), via search summary, **⚠ verify**).

### Android vitals: bad-behavior thresholds

From [Android vitals](https://support.google.com/googleplay/android-developer/answer/9844486):

| Metric | Overall | Per device model |
|---|---|---|
| User-perceived crash rate | **1.09%** | **8%** |
| User-perceived ANR rate | **0.47%** | **8%** |

Exceeding them reduces discoverability and can add a warning on your listing.

### Rejections and appeals

- Check **Play Console → [app] → Policy status**. It shows rejection, removal or suspension details ([source](https://support.google.com/googleplay/android-developer/answer/9842754)).
- **Rejected:** fix the issue and resubmit. If you think it was a mistake, appeal. "Until a policy violation has been fixed, don't republish a rejected app" ([source](https://support.google.com/googleplay/android-developer/answer/2477981)).
- When fixing, upload a compliant bundle to **every** track and **deactivate** the non-compliant bundles, or the resubmission fails ([source](https://support.google.com/googleplay/android-developer/answer/2477981)).
- **One appeal per enforcement action**, in English, Chinese, Japanese or Korean ([source](https://support.google.com/googleplay/android-developer/answer/2477981)).
- Appeal response time "may take up to 7 days" (Policy Status page via search summary, **⚠ verify**).

---

## Mileage app: specific rejection risks

| Likely rejection | Cause | Avoid it by |
|---|---|---|
| **FGS declaration rejected** | The video doesn't show the user starting the feature, or the FGS keeps running after the trip | The video shows the **Start trip** tap. Stop the service on **Stop** or when auto-end fires. Never start it from boot. |
| **Background location rejected** | Requesting `ACCESS_BACKGROUND_LOCATION` for auto-detect ("convenience"), or a library merging it into the manifest | Don't request it (designs A/B). Check `app/build/intermediates/merged_manifest` for stray permissions before every upload. |
| **Prominent disclosure missing or non-compliant** | The runtime prompt appears with no prior in-app explanation, or the explanation lives only in the privacy policy, or the button says "Got it" | Show a full-screen disclosure before the prompt, with **Agree** and **Not now** buttons, written in plain language |
| **Data safety mismatch** | You declare "no data collected" but an SDK (crash reporting, analytics, map tiles) sends data | Ship with no network SDKs, or declare what they send |
| **Misleading metadata or implied affiliation** | "Official GSA rates" or "IRS-approved" wording, or US government emblems | Use "based on public GSA data" plus a disclaimer. No emblems. |
| **Crashes in the pre-launch report** | Permission denied, approximate-only location, or no GPS on test devices | Handle denial, approximate location and a missing provider. Fall back to manual trips. |
| **SecurityException at runtime** | Starting the location FGS from the background (for example from an activity-transition receiver) on targetSdk 34+ | Only start it from a visible activity, or after a notification tap (design B) |
| **Location button policy (from Jan 27, 2027, targetSdk 37)** | Requesting location for one-time actions, and the new `ACCESS_FINE_LOCATION` declaration | Before moving to API 37, re-read the [policy preview](https://support.google.com/googleplay/android-developer/answer/16909972). Continuous trip tracking isn't a one-time use, but **⚠ verify** how it's classified. |

---

## Final checklist: Mileage & Per Diem

**Account (shared)**

- [ ] Personal account, $25 paid, ID verified, contacts verified
- [ ] Device verification done in the Play Console app

**Build**

- [ ] `targetSdk` 36 or higher
- [ ] AAB signed with an RSA 2048+ upload key that's backed up
- [ ] Play App Signing enabled
- [ ] No native libs, or 16 KB-aligned ones
- [ ] Package shows as registered on the developer-verification page

**Manifest**

- [ ] COARSE + FINE location, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS`
- [ ] `ACTIVITY_RECOGNITION` only with design B
- [ ] **No `ACCESS_BACKGROUND_LOCATION`**
- [ ] `<service foregroundServiceType="location">`

**In-app behavior**

- [ ] Disclosure screen before the location prompt
- [ ] Graceful handling of denial and approximate-only location
- [ ] Notification permission requested in context
- [ ] FGS starts only from a visible UI and stops when the trip ends
- [ ] Privacy policy link in Settings
- [ ] GSA disclaimer and data year shown

**Privacy**

- [ ] Policy hosted at an HTTPS URL (GitHub Pages), not a PDF
- [ ] It covers on-device storage and user-initiated export

**App content**

- [ ] Privacy policy
- [ ] Ads: No
- [ ] App access: no restrictions
- [ ] Content rating
- [ ] Target audience: 18+
- [ ] Data safety: no data collected or shared, no account creation
- [ ] Financial features: none (**⚠ verify**)
- [ ] Health: none (**⚠ verify** if using Activity Recognition)
- [ ] Government app: no
- [ ] News: no
- [ ] **FGS declaration** with description, impact, use case and video

**Listing**

- [ ] Name at most 30 characters, short description at most 80, full description at most 4,000
- [ ] Icon 512×512, feature graphic 1024×500
- [ ] 4 or more phone screenshots at 1080×1920
- [ ] Support email
- [ ] No "best" or "#1" claims and no government emblems

**Release**

- [ ] Internal → read the pre-launch report
- [ ] Closed with 12+ testers for 14 continuous days → apply for production
- [ ] Production at 100%
- [ ] Watch vitals (crash 1.09%, ANR 0.47%)

## Final checklist: Card Controls Studio

**Account**

- [ ] Same as above

**Build**

- [ ] `targetSdk` 36 or higher
- [ ] AAB + Play App Signing + upload key backed up
- [ ] **`INTERNET` permission absent** from the merged manifest
- [ ] No sensitive permissions

**Demo clarity**

- [ ] "Demo"/"Concept" in the title and short description
- [ ] DEMO watermark on cards
- [ ] First-launch notice
- [ ] Fake card numbers only
- [ ] **No fields for real card, PIN or bank credentials**
- [ ] No real bank, network or fintech logos or names

**Privacy**

- [ ] One-paragraph policy: collects nothing, no network, cards fictional
- [ ] Link it in the app's About screen

**App content**

- [ ] Privacy policy
- [ ] Ads: No
- [ ] App access: no restrictions
- [ ] Content rating
- [ ] Target audience: 18+
- [ ] Data safety: no data collected or shared
- [ ] **Financial features: "My app doesn't provide any financial features"**
- [ ] Health: none
- [ ] Government: no
- [ ] News: no
- [ ] No FGS declaration needed

**Listing**

- [ ] Category not Finance (**⚠ verify**)
- [ ] Screenshots show the DEMO marking
- [ ] Description leads with "fictional cards, no real payments"
- [ ] Assets at the sizes above

**Release**

- [ ] Its own closed test: 12+ testers for 14 days (**⚠ verify** whether it's per app)
- [ ] Apply for production → production

---

## Things I couldn't verify (check in Play Console)

1. **Next target-API deadline** (likely Aug 2027 / API 37). It isn't announced on the pages fetched.
2. Whether the **12/14 closed-test gate is per app or per account**. The page implies per app.
3. Whether **Google Group** testers count toward the 12.
4. Whether a reviewer accepts the **notification-tap** trip start (design B) as "in-app user-initiated".
5. **Video length and host** for the FGS declaration. For background location, 30 s or less on YouTube or Drive is documented.
6. **Review time** for permission and FGS declarations, and the appeal response time.
7. Whether **Activity Recognition** for driving detection requires answering "Activity and Fitness" on the Health apps declaration.
8. Whether exporting through the **share sheet** needs a "shared" entry in Data safety. It probably falls under the user-initiated exemption.
9. Whether the IARC content rating is free.

---

## Sources

**Play Console Help (support.google.com/googleplay/android-developer)**

- [Get started with Play Console (6112435)](https://support.google.com/googleplay/android-developer/answer/6112435)
- [Choose a developer account type (13634885)](https://support.google.com/googleplay/android-developer/answer/13634885)
- [Required information to create a developer account (13628312)](https://support.google.com/googleplay/android-developer/answer/13628312)
- [Device verification requirements for new developer accounts (14316361)](https://support.google.com/googleplay/android-developer/answer/14316361)
- [App testing requirements for new personal developer accounts (14151465)](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Target API level requirements (11926878)](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Play App Signing (9842756)](https://support.google.com/googleplay/android-developer/answer/9842756)
- [Data safety section (10787469)](https://support.google.com/googleplay/android-developer/answer/10787469)
- [User Data policy (10144311)](https://support.google.com/googleplay/android-developer/answer/10144311)
- [Prepare your app for review / App content (9859455)](https://support.google.com/googleplay/android-developer/answer/9859455)
- [Understanding location in the background permissions (9799150)](https://support.google.com/googleplay/android-developer/answer/9799150)
- [Best practices for prominent disclosure and consent (11150561)](https://support.google.com/googleplay/android-developer/answer/11150561)
- [Permissions and APIs that Access Sensitive Information (16585319)](https://support.google.com/googleplay/android-developer/answer/16585319)
- [Preview: Permissions policy changes, location button (16909972)](https://support.google.com/googleplay/android-developer/answer/16909972)
- [Foreground service and full-screen intent requirements (13392821)](https://support.google.com/googleplay/android-developer/answer/13392821)
- [Device and Network Abuse, incl. FGS permissions (9888379)](https://support.google.com/googleplay/android-developer/answer/9888379)
- [Financial Services policy (9876821)](https://support.google.com/googleplay/android-developer/answer/9876821)
- [Financial features declaration (13849271)](https://support.google.com/googleplay/android-developer/answer/13849271)
- [Health apps declaration (14738291)](https://support.google.com/googleplay/android-developer/answer/14738291)
- [Government apps requirements (9514050)](https://support.google.com/googleplay/android-developer/answer/9514050)
- [News and magazine declaration (16189314)](https://support.google.com/googleplay/android-developer/answer/16189314)
- [Account deletion requirements (13327111)](https://support.google.com/googleplay/android-developer/answer/13327111)
- [Content ratings (9898843)](https://support.google.com/googleplay/android-developer/answer/9898843)
- [Target audience and content (9867159)](https://support.google.com/googleplay/android-developer/answer/9867159)
- [Impersonation (9888374)](https://support.google.com/googleplay/android-developer/answer/9888374)
- [Deceptive Behavior (17006354)](https://support.google.com/googleplay/android-developer/answer/17006354)
- [Metadata (9898842)](https://support.google.com/googleplay/android-developer/answer/9898842)
- [Create and set up your app (9859152)](https://support.google.com/googleplay/android-developer/answer/9859152)
- [Add preview assets (9866151)](https://support.google.com/googleplay/android-developer/answer/9866151)
- [Set up testing tracks (9845334)](https://support.google.com/googleplay/android-developer/answer/9845334)
- [Prepare and roll out a release (9859348)](https://support.google.com/googleplay/android-developer/answer/9859348)
- [Publish your app (9859751)](https://support.google.com/googleplay/android-developer/answer/9859751)
- [Control when app changes are reviewed and published (9859654)](https://support.google.com/googleplay/android-developer/answer/9859654)
- [Staged rollouts (6346149)](https://support.google.com/googleplay/android-developer/answer/6346149)
- [Pre-launch report (9842757)](https://support.google.com/googleplay/android-developer/answer/9842757)
- [Android vitals (9844486)](https://support.google.com/googleplay/android-developer/answer/9844486)
- [Check your app's policy status (9842754)](https://support.google.com/googleplay/android-developer/answer/9842754)
- [My app has been removed from Google Play (2477981)](https://support.google.com/googleplay/android-developer/answer/2477981)

**developer.android.com**

- [Target API level requirements](https://developer.android.com/google/play/requirements/target-sdk)
- [Android developer verification](https://developer.android.com/developer-verification)
- [Developer verification FAQ](https://developer.android.com/developer-verification/guides/faq)
- [Support 16 KB page sizes](https://developer.android.com/guide/practices/page-sizes)
- [Android App Bundles](https://developer.android.com/guide/app-bundle)
- [Request location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions)
- [Access location in the background](https://developer.android.com/develop/sensors-and-location/location/background)
- [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Restrictions on starting a foreground service from the background](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Notification runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission)
- [Detect when users start or end an activity (Transition API)](https://developer.android.com/develop/sensors-and-location/location/transitions)
