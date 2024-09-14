import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
// import 'package:usbnfcreader/usbnfcreader_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  // MethodChannelUsbnfcreader platform = MethodChannelUsbnfcreader();
  const MethodChannel channel = MethodChannel('usbnfcreader');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });
}
