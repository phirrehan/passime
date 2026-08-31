# PassIme

A lightweight Android input method designed to insert credentials from a
local password manager without requiring Accessibility or Autofill access.

## Features

- Android IME
- Inserts text using `InputConnection.commitText()`
- No Accessibility Service
- No Autofill Service
- Works with a local password-management workflow
- Termux integration
- GPG/pass-compatible workflow

## Security

PassIme does not use an Accessibility Service to interact with other apps.

Credentials are intended to remain in memory only for the short period
required to insert them into the focused input field.

The release APK is signed with a release key. The signing key itself is
not included in this repository.

## Installation

Download the latest APK from the
[Releases](../../releases) page and install it.

Then enable PassIme under Android's keyboard settings.

## Development

Clone the repository and build with:

    ./gradlew assembleRelease
