package com.gms.usbnfcreader

import androidx.annotation.NonNull

import android.content.IntentFilter
import android.content.Intent
import android.hardware.usb.UsbManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import com.acs.smartcard.Reader
import io.flutter.plugin.common.MethodChannel.Result
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import java.lang.Exception
import java.nio.ByteBuffer

/** UsbnfcreaderPlugin */
class UsbnfcreaderPlugin: FlutterPlugin, MethodCallHandler {
  companion object {
    private const val ACTION_USB_PERMISSION = "ACTION_USB_PERMISSION"
  }
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var reader: Reader
  private lateinit var usbManager: UsbManager
  private lateinit var context: Context
  private var TAG = "USB_NFC_READER";

  fun hexToDecimal(hex: String): Int {
    return hex.toInt(16)
  }

  private fun bytesToHexArray(bytes: ByteArray): Array<Int> {
    var list = arrayOf<Int>()
    for (b in bytes) {
      val currentValue = String.format("%02x", b)
      list += hexToDecimal(currentValue)
    }
    return list
  }

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "usbnfcreader")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
    usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    reader = Reader(usbManager)
  }

  private val receiverPermission = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice
      if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
        Log.d(TAG, "Permission granted, opening connection to reader ...")
        reader.open(device)
        Log.d(TAG, "Reader is connected")
      } else {
        Log.d(TAG, "Permission denied, cannot open connection")
      }
    }
  }

  private val receiverDetached = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice
      if (device != null && device == reader.device) {
        reader.close()
        Log.d(TAG,"Reader detached")
        Log.d(TAG,"Connection to reader is disconnected")

        val filterAttached = IntentFilter()
        filterAttached.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        context.registerReceiver(receiverAttached, filterAttached)
      }
    }
  }

  private val receiverAttached = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      Log.d(TAG, "USB Attached")
      startNFCScanner()
    }
  }

  private fun startNFCScanner() {
      val device = usbManager.deviceList.values.firstOrNull()
      if (device != null) {
        Log.d(TAG, "device detected")
        val filterPermission = IntentFilter()
        filterPermission.addAction(ACTION_USB_PERMISSION)
        context.registerReceiver(receiverPermission, filterPermission)

        val filterDetached = IntentFilter()
        filterDetached.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        context.registerReceiver(receiverDetached, filterDetached)

        val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), 0)
        usbManager.requestPermission(
          device,
          permissionIntent
        )
      } else {
        Log.d("startNFCScanner", "no device detected")
        val filterAttached = IntentFilter()
        filterAttached.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        context.registerReceiver(receiverAttached, filterAttached)
      }
  }

  fun toInt32(bytes:ByteArray):Int {
    if (bytes.size != 4) {
      throw Exception("wrong len")
    }
    bytes.reverse()
    return ByteBuffer.wrap(bytes).int
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    if (call.method == "startScanner") {
      startNFCScanner()
      reader.setOnStateChangeListener { _, _, currState ->
        if (currState == Reader.CARD_PRESENT) {
          Log.d(TAG, "Found a card")
          val command = byteArrayOf(0xFF.toByte(), 0xCA.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte())
          Log.d(TAG, "Getting id...")
          reader.power(0, Reader.CARD_WARM_RESET)
          reader.setProtocol(0, Reader.PROTOCOL_T0 or Reader.PROTOCOL_T1)

          val response = ByteArray(256)
          val responseLength: Int = reader.transmit(0, command, command.size, response, response.size)
          if (responseLength >= 2) {
            val idBytes = response.copyOf(responseLength - 2)
            val idNumber = bytesToHexArray(idBytes)
              Log.d(TAG, "Success getting id : ${idNumber.joinToString(",")}")
          } else {
            Log.d(TAG, "Failed getting id")
          }
        }
      }
      result.success("ok")
    } else if (call.method == "stopScanner") {
      reader.close();
    } else {
      result.notImplemented()
    }
  }

  private fun unregister() {
    try {
      context.unregisterReceiver(receiverAttached)
    } catch (e: java.lang.Exception) {}
    try {
      context.unregisterReceiver(receiverDetached)
    } catch (e: java.lang.Exception) {}
    try {
      context.unregisterReceiver(receiverPermission)
    } catch (e: java.lang.Exception) {}
    try {
      reader.close()
    } catch (e: Exception) {}
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
    unregister()
  }
}
