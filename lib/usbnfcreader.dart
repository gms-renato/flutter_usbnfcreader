import 'package:flutter/services.dart';
import 'package:usbnfcreader/usbnfcreader_method_channel.dart';

typedef NfcTagCallback = Future<void> Function(NfcTag tag);
typedef NfcReaderStateCallback = void Function();

class Usbnfcreader {
  static Usbnfcreader? _instance;
  static Usbnfcreader get instance => _instance ??= Usbnfcreader._();

  Usbnfcreader._() {
    channel.setMethodCallHandler(_handleMethodCall);
  }
  NfcTagCallback? _onDiscovered;
  NfcReaderStateCallback? _onReaderAttached;
  NfcReaderStateCallback? _onReaderDetached;

  void startSession(
      {required NfcTagCallback onDiscovered,
      bool autoConnect = true,
      NfcReaderStateCallback? onReaderAttached,
      NfcReaderStateCallback? onReaderDetached}) {
    _onDiscovered = onDiscovered;
    _onReaderAttached = onReaderAttached;
    _onReaderDetached = onReaderDetached;
    channel.invokeMethod('startSession', {'autoConnect': autoConnect});
  }

  void stopSession() {
    _onDiscovered = null;
    channel.invokeMethod('stopSession');
  }

  void alertSuccess() {
    channel.invokeMethod('alertSuccess');
  }

  void alertError() {
    channel.invokeMethod('alertError');
  }

  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onDiscovered':
        _handleOnDiscovered(call);
        break;
      case 'onReaderAttached':
        _onReaderAttached?.call();
        break;
      case 'onReaderDetached':
        _onReaderDetached?.call();
        break;
      default:
        throw ('Not implemented: ${call.method}');
    }
  }

  // _handleOnDiscovered
  void _handleOnDiscovered(MethodCall call) async {
    final Map<String, dynamic> arguments =
        Map<String, dynamic>.from(call.arguments);
    final String idHex = arguments['idHex'] as String;
    final List<int> idNumber = (arguments['idNumber'] as String)
        .split(',')
        .map((e) => int.parse(e.trim()))
        .toList();

    final NfcTag tag = NfcTag(hexId: idHex, id: idNumber);

    await _onDiscovered?.call(tag);
  }
}

class NfcTag {
  final String hexId;
  final List<int> id;
  const NfcTag({
    required this.hexId,
    required this.id,
  });
}
