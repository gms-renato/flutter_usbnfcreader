import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'usbnfcreader_method_channel.dart';

abstract class UsbnfcreaderPlatform extends PlatformInterface {
  /// Constructs a UsbnfcreaderPlatform.
  UsbnfcreaderPlatform() : super(token: _token);

  static final Object _token = Object();

  static UsbnfcreaderPlatform _instance = MethodChannelUsbnfcreader();

  /// The default instance of [UsbnfcreaderPlatform] to use.
  ///
  /// Defaults to [MethodChannelUsbnfcreader].
  static UsbnfcreaderPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [UsbnfcreaderPlatform] when
  /// they register themselves.
  static set instance(UsbnfcreaderPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
