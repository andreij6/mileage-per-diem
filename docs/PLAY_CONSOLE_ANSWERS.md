# Play Console answer sheet: Mileage & Per Diem v1.0.0

Ready-to-paste answers for every Play Console section, in the order you'll meet them. Sources and the reasoning behind each answer are in [`PLAY_PUBLISHING.md`](PLAY_PUBLISHING.md). Items marked ⚠ are judgment calls to double-check in the Console.

## Create app
| Field | Answer |
|---|---|
| App name | `Mileage & Per Diem` |
| Default language | English (United States) – en-US |
| App or game | App |
| Free or paid | Free |
| Package | `com.aijones.mileage` (permanent) |
| Play App Signing | Enroll (Google holds the app signing key, and you upload with `~/keys/mileage-upload.jks`) |

## Store settings
| Field | Answer |
|---|---|
| Category | **Business** (not Finance, so the app isn't mistaken for a financial-services app) |
| Tags | Business, Productivity, Travel & local, Auto & vehicles (Play has no mileage or expense tag; Finance is left out on purpose) |
| Contact email | the developer account email |
| Website | `https://andreij6.github.io/mileage-per-diem/` |
| Countries | **United States only**. IRS rates and GSA per diems are U.S. rules. |

## App content
| Section | Answer |
|---|---|
| **Privacy policy** | `https://andreij6.github.io/mileage-per-diem/privacy/` |
| **App access** | All functionality is available without special access (no login). |
| **Ads** | No, the app does not contain ads. |
| **Content rating** (IARC) | Category: *Utility, Productivity, Communication or Other*. Every content question: **No** (no violence, sexual content, profanity, drugs, gambling or user-generated content; no sharing of the user's location with other users; no digital purchases). Expected result: Everyone / PEGI 3. |
| **Target audience** | **18 and over** only. Not designed for children. |
| **News app** | No |
| **Health apps** | No health features. Activity recognition is only used to suggest that a drive may be starting. ⚠ |
| **Financial features** | "My app doesn't provide any financial features." It calculates reimbursement amounts but moves no money and provides no financial services. |
| **Government apps** | No. It uses public IRS and GSA data but isn't affiliated with any government; the About screen and listing say "Data sources". |
| **Data safety** | See below. |
| **Foreground service permissions** | See below. |
| **Location permissions declaration** | **Not required.** The app does not request `ACCESS_BACKGROUND_LOCATION` (checked by `:app:verifyNoForbiddenPermissions`). |

### Data safety
- **Does your app collect or share any of the required user data types?** → **No.**
  - Location, trip details, per diem stays and settings are processed and stored **only on the device**. Play's definition excludes data "only processed locally on the user's device and not sent off device".
  - Reports and share images leave the device only through a **user-initiated** share-sheet action, which Play exempts from "sharing".
  - There is no `INTERNET` permission, no SDK that transmits data, and no analytics leaves the device.
- **Is all data encrypted in transit?** → Not applicable (nothing is transmitted).
- **Can users request deletion?** → Users can delete everything in-app (About & privacy → Delete all trips and reports) or by uninstalling.
- ⚠ **Android Auto Backup** includes the trips database and settings in the user's own Google device backup. That is user-controlled OS backup rather than developer collection. If the form or a reviewer treats it as collection, either declare "Location" and "App activity" as collected with the purpose *App functionality*, or turn off backup (`android:allowBackup="false"`).

### Foreground service declaration
| Field | Answer |
|---|---|
| Foreground service type | `location` |
| Use case | **User-initiated location sharing / tracking** ("vehicle activity tracking") |
| Feature description | Records the route and distance of a business drive that the user starts from the app, a widget or the Quick Settings tile, so the trip can be costed at the IRS mileage rate and exported as an expense report. A persistent notification is shown while recording, with Pause and Stop. Recording stops as soon as the user ends the trip. |
| Impact if deferred or interrupted | The trip's distance would be missing or wrong, so the user's mileage reimbursement and expense report would be inaccurate. |
| Video link | Upload [`store-listing/video/fgs-location-demo.mp4`](../store-listing/video/fgs-location-demo.mp4) to YouTube as **Unlisted** and paste the link. It shows: fresh install → location disclosure → permission prompts → Start trip → recording notification → app in the background and screen off while recording continues → reopen from the notification → slide to end → trip saved. |

## Store listing
Copy, graphics and the 8 screenshots are in [`store-listing/`](../store-listing/PLAY_STORE_LISTING.md). The screenshots are captured from the running app (API 36 emulator).

## Testing and release
1. **Internal testing:** upload `app-release.aab` from `./gradlew :app:bundleRelease` (see [`RELEASING.md`](RELEASING.md)), add yourself, and smoke-test the Play-installed build.
2. **Closed testing:** new personal accounts need **at least 12 testers opted in for 14 consecutive days**. Use a Google Group as the tester list. Ask testers to install and *keep* the app installed; drives and screenshots of any issues help.
3. **Apply for production access** (Dashboard). You'll answer how testing went and what changed; Google says reviews take about 7 days.
4. **Production:** a staged rollout (for example 20% → 100%), with Android vitals and the pre-launch report watched at each step.
