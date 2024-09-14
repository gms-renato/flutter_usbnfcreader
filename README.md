# usbnfcreader

Only works for ACR122U USB NFC reader for Android

## Usage

**Handling Session**

```dart
// Check availability
 final usbnfcreader = await Usbnfcreader.instance

// Start Session
usbnfcreader.startSession(
  onDiscovered: (NfcTag tag) async {
    // Do something with an NfcTag instance.
  },
);

// Stop Session
usbnfcreader.stopSession();
```

**Following events are available:**

-   onDiscovered
-   onReaderAttached
-   onReaderDetached
