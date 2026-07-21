# Privacy Policy — Clever Password

**Last updated: July 21, 2026**

Clever Password ("the app") is a password and word generator developed by TomClaw
Software ("we", "us", "our"). This policy explains, in plain language, exactly what
the app does and does not collect.

**Short version:** the app never sees, stores, or transmits the passwords, words, or
mask templates it generates — they are created on your device and stay on your
device. The only information that leaves your device is a small amount of **anonymous**
usage and crash data used to fix bugs and improve the app. There are **no ads and no
third‑party trackers**.

## What we never collect

We do **not** collect, transmit, or have any way to access:

- **The passwords, words, nicknames, or custom mask templates you generate or copy.**
  They are produced entirely on your device and are never sent anywhere.
- Your name, email address, phone number, or any account — the app has no sign‑in and
  no user accounts.
- Your contacts, photos, files, messages, clipboard history, or calendar.
- Your GPS or network‑based location.
- Any advertising identifier (we do **not** use the Google Advertising ID) or any
  hardware identifier such as IMEI, MAC address, or serial number.
- Biometric data.

The app contains **no advertising** and **no third‑party advertising or analytics
SDKs** — there is no Google Analytics/Firebase, no Meta/Facebook SDK, no ad networks,
and no cross‑app or cross‑site tracking.

## What is collected — anonymously

The app includes a lightweight, self‑hosted analytics component (the open‑source
[Bananalytics](https://github.com/solkin/bananalytics-android) library) operated by us.
It collects only aggregate, anonymous data that cannot identify you:

**Anonymous identifiers**

- A random **installation ID** — a UUID generated locally the first time you open the
  app. It is not derived from your device, phone number, or any advertising ID, and it
  is not linked to your identity. Clearing the app's data or reinstalling creates a new
  one.
- A random **session ID** for each time the app is opened.

**Device and app environment**

- App version and package name.
- Android version, device manufacturer, and device model.
- Country and language, as reported by your device's own settings.

**Anonymous usage events**

- Which features you use — for example: generating a password or word, copying a
  result, opening the practice ("memorize") screen, generating from the Quick Settings
  tile, or changing a setting — together with **non‑content** parameters of that action,
  such as the selected strength preset, the chosen grammar language, whether an option
  is on or off, or the number of blocks in a custom mask.
- These events **never** contain the generated passwords, words, or mask contents.

**Crash diagnostics**

- If the app crashes, a technical error report (stack trace, thread name, timestamp) is
  stored and sent so we can find and fix the problem. We do not intentionally place any
  personal data in these reports.

## How we use this data

We use this anonymous data **only** to understand which features are used and to find
and fix bugs and crashes, and we analyze it **in aggregate**. We do **not** sell, rent,
share, or trade it, we do **not** use it for advertising, and we do **not** build
profiles of individual users.

## Where the data goes

Data is sent over an encrypted **HTTPS** connection to our own analytics endpoint at
`bnn.citron.dev`, operated by TomClaw Software. It is not shared with any third‑party
analytics or advertising provider.

## Storage and retention

Events and crash reports are first stored on your device and sent in small batches when
a network connection is available (offline‑first). On our server, anonymous data is kept
only as long as needed for aggregate analysis and is then discarded. Because the data is
not linked to your identity, we cannot single you out within it.

## Permissions

The app requests a single permission — `INTERNET` — used only to send the anonymous
usage and crash data described above.

## Your choices

Because we collect no personal data and there is no account, there is nothing tied to
you to retrieve or erase. To stop all data collection and reset the anonymous
installation ID, clear the app's data in your Android settings or uninstall the app.

## Children

The app is not directed at children and does not knowingly collect personal information
from anyone, including children.

## Changes to this policy

We may update this policy from time to time. Material changes will be reflected here
with a new "Last updated" date.

## Contact

Questions about this policy? Contact TomClaw Software at **inbox@tomclaw.com**.
