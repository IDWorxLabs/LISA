# LISA Privacy Notice

**Version:** 1.1 (LISA V1)  
**Last updated:** July 2026

## Local-first design

LISA V1 is **local-first**. Communication profiles, custom phrases, settings, guided-learning progress, companion memory, and checklists are stored on your device using Android Keystore–backed encryption.

Android Auto Backup is **disabled** for LISA so these preferences are not included in cloud backup or device-transfer extraction rules.

## Camera processing

- The front camera is used to detect your face and intentional wink sequences.
- **All camera processing happens on the device** (Google ML Kit Face Detection).
- LISA does **not** record or store video or photo files.
- Facial landmarks are not collected; only eye-open probabilities are used for wink detection.

## Feedback

- Feedback you type in the form is kept **only while LISA remains open** (current app session).
- Feedback fields are **cleared on a fresh app launch**.
- **Review and Send** opens an in-app caregiver-assistance step before the system email chooser.
- An email account must already be set up in an email app on the device (for example Gmail or Outlook).
- A caregiver may be needed to choose the email app, select the account, review the message, and press Send (LISA wink control does not operate inside external email apps).
- Information leaves LISA **only if** the caregiver or user chooses to send the email.
- The email provider handles transmission.
- The pre-filled message includes app version, Android version, device model, operational diagnostics (build type, sensitivity, response time, glasses preference, communication language, camera/face/eyes readiness, and local date/time), and the feedback fields you entered. It does **not** include photos, camera frames, audio, biometric templates, stored phrases, contacts, or GPS.
- You can edit or delete any content before sending.
- LISA **cannot confirm** whether the message was sent.

## Emergency mode (V1)

Emergency mode plays a **local alarm sound**, repeats a **spoken help phrase**, and shows an **on-screen alert**.

In LISA V1 it does **not**:

- place phone calls
- send SMS / messages
- share location
- contact a caregiver remotely
- contact emergency services

Future caregiver-notification architecture may exist in the codebase as inactive V2 foundation code and is **not active** in LISA V1.

## Accounts and cloud

- **No cloud account** is required or used in V1.
- LISA does not upload profiles, phrases, or camera video to a LISA server.
- System Text-to-speech may use your device’s speech engine (outside LISA’s control).

## Data stored on device

Encrypted local preferences may include:

- Communication profiles (including display name and settings)
- Eye-calibration numeric thresholds (not images)
- Custom vocabulary / phrases
- Guided Learning progress
- Companion memory learning history
- Testing checklist progress
- Onboarding completion flags

Feedback form text is **not** stored in encrypted preferences across launches.

## Contact

Feedback: `lisa-feedback@asgarddynamics.io` (via your email app).
