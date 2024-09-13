
import 'usbnfcreader_platform_interface.dart';

class Usbnfcreader {
  Future<String?> getPlatformVersion() {
    return UsbnfcreaderPlatform.instance.getPlatformVersion();
  }
}
