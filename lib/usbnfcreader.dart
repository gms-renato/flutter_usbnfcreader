import 'package:flutter/services.dart';
import 'package:usbnfcreader/usbnfcreader_method_channel.dart';

typedef NfcTagCallback = Future<void> Function(NfcTag tag);

class Usbnfcreader {
  static Usbnfcreader? _instance;
  static Usbnfcreader get instance => _instance ??= Usbnfcreader._();

  Usbnfcreader._() {
    channel.setMethodCallHandler(_handleMethodCall);
  }
  NfcTagCallback? _onDiscovered;

  void startSession({required NfcTagCallback onDiscovered}) {
    _onDiscovered = onDiscovered;
    channel.invokeMethod('startSession');
  }

  void stopSession() {
    _onDiscovered = null;
    channel.invokeMethod('stopSession');
  }

  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onDiscovered':
        _handleOnDiscovered(call);
        break;
      case 'onReaderAttached':
        break;
      case 'onReaderDetached':
        break;
      default:
        throw ('Not implemented: ${call.method}');
    }
  }

  // _handleOnDiscovered
  void _handleOnDiscovered(MethodCall call) async {
    // final tag = $GetNfcTag(Map.from(call.arguments));
    // await _onDiscovered?.call(tag);
  }
}

class NfcTag {
  final String id;
  final List<int> bytes;
  const NfcTag({
    required this.id,
    required this.bytes,
  });
}
