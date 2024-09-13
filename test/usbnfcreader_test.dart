import 'package:flutter_test/flutter_test.dart';
import 'package:usbnfcreader/usbnfcreader.dart';
import 'package:usbnfcreader/usbnfcreader_platform_interface.dart';
import 'package:usbnfcreader/usbnfcreader_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockUsbnfcreaderPlatform
    with MockPlatformInterfaceMixin
    implements UsbnfcreaderPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final UsbnfcreaderPlatform initialPlatform = UsbnfcreaderPlatform.instance;

  test('$MethodChannelUsbnfcreader is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelUsbnfcreader>());
  });

  test('getPlatformVersion', () async {
    Usbnfcreader usbnfcreaderPlugin = Usbnfcreader();
    MockUsbnfcreaderPlatform fakePlatform = MockUsbnfcreaderPlatform();
    UsbnfcreaderPlatform.instance = fakePlatform;

    expect(await usbnfcreaderPlugin.getPlatformVersion(), '42');
  });
}
