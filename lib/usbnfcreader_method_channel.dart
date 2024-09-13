import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'usbnfcreader_platform_interface.dart';

/// An implementation of [UsbnfcreaderPlatform] that uses method channels.
class MethodChannelUsbnfcreader extends UsbnfcreaderPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('usbnfcreader');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
