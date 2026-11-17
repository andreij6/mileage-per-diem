# Mileage & Per Diem — Google Play Store Listing

> Prepared from `design/mileage-per-diem.figcraft` (`design/renders/`) and `docs/ARCHITECTURE.md`.

---

## 1. App Identity & Limits

| Field | Value | Character Count |
|---|---|---|
| **App Name** (max 30) | `Mileage & Per Diem` | 18 / 30 |
| **Developer / Brand** | `AI.Jones` | — |
| **Short Description** (max 80) | `Offline GPS mileage tracker, GSA per diem lookup & PDF/CSV expense reports.` | 75 / 80 |
| **App Category** | `Business` (or `Finance`) | — |
| **Tags** | `Mileage tracker`, `Expense tracker`, `Per diem`, `IRS mileage`, `Business travel` | — |
| **Price** | `Free` (no ads, no subscriptions, no in-app purchases) | — |

---

## 2. Full Description (max 4,000 characters)

```text
Track business drives, look up GSA per diem rates, and export clean PDF or CSV expense reports — completely offline on your phone.

Mileage & Per Diem is built for contractors, consultants, field teams, and business travelers who want fast, accurate reimbursement records without accounts, subscriptions, or cloud tracking. The app does not even request the INTERNET permission: your routes, trips, and reports never leave your device.

KEY FEATURES

• Automatic & One-Tap GPS Trip Recording
  Start a trip from the home screen, home-screen widget, or Quick Settings tile. Watch live distance on a tactile odometer, elapsed time, speed, and estimated reimbursement as you drive — with adaptive battery mode and a transparent foreground notification.

• Built-In Offline Route Canvas
  Every drive is drawn on an in-app route canvas with start/end pins and place labels — no external map SDK, no API keys, and zero network dependency.

• Effective-Dated IRS Mileage Rates
  Trips automatically snapshot the IRS standard mileage rate for the date of your drive (including mid-year rate changes like 72.5¢ Jan–Jun 2026 and 76¢ Jul 2026+), or use your own custom reimbursement rate and saved vehicles.

• Bundled GSA Per Diem Lookup (Works Offline)
  Search U.S. cities for official FY2027 GSA lodging and Meals & Incidentals (M&IE) rates — broken down by Breakfast, Lunch, Dinner, and Incidentals, with monthly lodging variations, 75% first/last travel day calculation, and multi-day stay estimates.

• Swipe-to-Sort Trip Review & Printed Receipts
  Sort unclassified drives in seconds with a swipe deck (swipe right for Business, left for Personal, with one-tap Undo and purpose tags). Every saved trip generates a clean itemized receipt with route preview, rate snapshot, and total.

• One-Tap PDF & CSV Expense Reports
  Combine business mileage and per diem stays for any date range into a polished PDF or RFC 4180 CSV report, generated locally on your phone and shared only through the Android share sheet.

• Monthly Insights, Heatmaps & Widgets
  Spot your busiest days on a monthly mileage calendar heatmap, view longest trips and top stops, explore your shareable "Year on the Road" summary cards, and glance at your monthly total or active drive from home-screen widgets.

PRIVACY BY DESIGN
• No account or sign-in required
• No backend servers, no cloud sync, no third-party analytics
• No INTERNET permission requested
• Foreground-only location: GPS is used only while you are actively recording a trip (optional on-device drive detection only suggests starting a trip via notification)
```

*(~2,490 characters — well within the 4,000-character Play Store limit.)*

---

## 3. What's New (v1.0.0 Release Notes, max 500 characters)

```text
Initial release of Mileage & Per Diem by AI.Jones:
• Offline GPS trip recording with live odometer & in-app route canvas
• Effective-dated IRS mileage rates (72.5¢ Jan–Jun 2026, 76¢ Jul 2026+) & custom rates
• Bundled FY2027 GSA per diem city lookup & multi-day stay estimator
• Swipe-to-sort trip review deck & itemized trip receipts
• Local PDF & CSV expense report export
• Monthly mileage insights, home-screen widgets & Quick Settings tile
```

---

## 4. Store Listing Graphics Inventory

All generated assets live in `store-listing/assets/`:

| Asset | Dimensions | Format | File |
|---|---|---|---|
| **App Icon** | 512 × 512 px | PNG | `store-listing/assets/play_store_icon_512x512.png` |
| **Feature Graphic** | 1024 × 500 px | PNG | `store-listing/assets/feature_graphic_1024x500.png` |
| **Screenshot 1 (Home)** | 1080 × 1920 px | PNG | `store-listing/assets/screenshots/01_home_odometer.png` |
| **Screenshot 2 (Live Tracking)** | 1080 × 1920 px | PNG | `store-listing/assets/screenshots/02_live_tracking.png` |
| **Screenshot 3 (Trip Receipt)** | 1080 × 1920 px | PNG | `store-listing/assets/screenshots/03_trip_receipt.png` |
| **Screenshot 4 (Swipe Review)** | 1080 × 1920 px | PNG | `store-listing/assets/screenshots/04_swipe_review.png` |
| **Screenshot 5 (Per Diem)** | 1080 × 1920 px | PNG | `store-listing/assets/screenshots/05_per_diem_gsa.png` |
| **Screenshot 6 (PDF/CSV Reports)** | 1080 × 1920 px | PNG | `store-listing/assets/screenshots/06_reports_export.png` |
| **Screenshot 7 (Monthly Insights)** | 1080 × 1920 px | PNG | `store-listing/assets/screenshots/07_monthly_insights.png` |
| **Screenshot 8 (Widgets & Dark Mode)** | 1080 × 1920 px | PNG | `store-listing/assets/screenshots/08_widgets_quick_settings.png` |

---

## 5. Screenshot Headlines & Captions

1. **01 — Home & Odometer** (`design/renders/trips/home-light.png`)
   - Headline: `Track every business mile`
   - Subhead: `Monthly totals, live odometer & IRS rate snapshots`
2. **02 — Live GPS Tracking** (`design/renders/tracking/live-light.png`)
   - Headline: `Live route, 100% offline`
   - Subhead: `In-app route canvas & battery-aware GPS recording`
3. **03 — Trip Receipt** (`design/renders/tracking/receipt-light.png`)
   - Headline: `Instant itemized trip receipts`
   - Subhead: `Route preview, effective IRS rate & exact reimbursement`
4. **04 — Swipe to Sort** (`design/renders/trips/review-light.png`)
   - Headline: `Sort drives in seconds`
   - Subhead: `Swipe right for business, left for personal`
5. **05 — GSA Per Diem** (`design/renders/per-diem/per_diem-light.png`)
   - Headline: `Bundled GSA per diem rates`
   - Subhead: `Offline city lookup for lodging & daily M&IE`
6. **06 — Reports Export** (`design/renders/reports/report-light.png`)
   - Headline: `Export PDF & CSV reports`
   - Subhead: `Generated on your phone — shared only by you`
7. **07 — Monthly Insights** (`design/renders/insights/insights-light.png`)
   - Headline: `See your year on the road`
   - Subhead: `Mileage heatmaps, top stops & shareable stories`
8. **08 — Widgets & Quick Settings** (`design/renders/widgets/widgets-light.png`)
   - Headline: `Start drives in one tap`
   - Subhead: `Home-screen widgets & Quick Settings tile`

---

## 6. Play Console Data Safety & Policy Notes

- **Data collected:** None.
- **Data shared with third parties:** None.
- **Network / `INTERNET` permission:** Not declared. All storage (Room + DataStore) and reference data (IRS rates + bundled `perdiem.db`) live on-device.
- **Location permission:** Foreground location only (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` + `FOREGROUND_SERVICE_LOCATION`). `ACCESS_BACKGROUND_LOCATION` is **not** requested; optional Activity Recognition drive detection only posts a local notification prompting the user to open the app and start recording.
