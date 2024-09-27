import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:usbnfcreader/usbnfcreader.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _usbnfcreaderPlugin = Usbnfcreader.instance;
  String _lastScannedTag = 'Belum ada tag yang dipindai';

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      _usbnfcreaderPlugin.startSession(
        onDiscovered: (tag) async {
          debugPrint("onDiscovered");
          debugPrint(tag.hexId);
          debugPrint(tag.id.toString());
        },
        onReaderAttached: () {
          debugPrint("onReaderAttached");
        },
        onReaderDetached: () {
          debugPrint("onReaderDetached");
        },
      );
      // _usbnfcreaderPlugin.stopSession();
    } on PlatformException {
      debugPrint("Failed to start NFC Scanner.");
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;
  }

  void _onAlertSuccessButtonPressed() {
    _usbnfcreaderPlugin.alertSuccess();
  }

  void _onAlertErrorButtonPressed() {
    _usbnfcreaderPlugin.alertError();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Contoh Aplikasi Plugin'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(_lastScannedTag),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: _onAlertSuccessButtonPressed,
                child: const Text('Alert Success'),
              ),
              ElevatedButton(
                onPressed: _onAlertErrorButtonPressed,
                child: const Text('Alert Error'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
