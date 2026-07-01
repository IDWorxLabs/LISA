# LISA Testing Guide

**Version:** 1.1 local testing build  
**Audience:** Family, friends, and caregivers helping with real-world testing

## Before you start

1. Complete the **first-launch onboarding** on the device.
2. Grant **camera permission** when prompted (or via Settings if denied).
3. Open **Menu → Testing Checklist** and work through every item.
4. Read **About LISA** for product context, privacy, and safety information.

## Recommended test order

### 1. Camera and face detection

- Open LISA and confirm the front camera preview appears.
- Position the user's face in frame until LISA shows that a face is detected.

### 2. Wink detection

- Test **left wink** and **right wink** separately.
- Enable **Developer Tools → Developer Mode** if you need live detection values.
- Adjust **sensitivity** if winks are missed or over-counted.

### 3. Basic phrases

- Test **Yes** (L2 R0) and **No** (L0 R2) sequences.
- Wait for the **confirmation countdown** before speech.
- **Left wink during countdown** = cancel; **right wink** = speak immediately.

### 4. Emergency (supervised only)

- Only test emergency with caregivers present and informed.
- Emergency sequence: **L6 R0** (no confirmation delay).
- Confirm the alarm sounds and the overlay shows **Would notify: [names]** for linked caregivers.
- Press **Reset** and confirm the alarm stops.

### 5. Profiles and caregivers

- **My Communication:** confirm or create the Primary User profile.
- **Caregiver Linking:** add test caregivers and set emergency permissions.
- Remember: **no real notifications** are sent in this build.

## Feedback

Use **Menu → Feedback** to record:

- What worked well
- What was confusing
- Whether winks were detected correctly
- Whether speech happened at the right time

Feedback is stored **locally on the device**.

## What this build does not include

- Cloud accounts or profile sync
- Real caregiver phone alerts (SMS, push, or call)
- Certified medical device validation
- iOS version

See **Menu → Release Notes** for the full 1.1 feature list.

## Reporting issues

Keep notes from the Feedback screen. Formal support channels are coming soon (see About LISA).

## Safety reminder

**Test with supervision. Do not rely on LISA as the only emergency system yet.**
