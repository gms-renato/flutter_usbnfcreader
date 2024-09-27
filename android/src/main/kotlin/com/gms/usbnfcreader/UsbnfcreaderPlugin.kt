package com.gms.usbnfcreader

import androidx.annotation.NonNull

import android.app.Activity
import android.content.IntentFilter
import android.content.Intent
import android.hardware.usb.UsbManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
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
class UsbnfcreaderPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  companion object {
    private const val ACTION_USB_PERMISSION = "ACTION_USB_PERMISSION"
  }
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var activity: Activity
  private lateinit var reader: Reader
  private lateinit var usbManager: UsbManager
  private lateinit var context: Context
  private var TAG = "USB_NFC_READER"
  private var turnOffBuzzer = false;

  fun hexToDecimal(hex: String): Int {
    return hex.toInt(16)
  }

  private fun bytesToHex(bytes: ByteArray): String {
    return bytes.joinToString("") { String.format("%02x", it) }
  }

  private fun bytesToDecimalArray(bytes: ByteArray): Array<Int> {
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
        val id = device.getDeviceId()
        val name = device.getDeviceName()
        val productName = device.getProductName()
        val vendorId = device.getVendorId()
        Log.d(TAG, "ID: " + id)
        Log.d(TAG, "Manufacturer: " + name)
        Log.d(TAG, "productName: " + productName)
        Log.d(TAG, "Vendor ID: " + vendorId)
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
        Log.d(TAG,"Reader detached")
        Log.d(TAG,"Connection to reader is disconnected")
        reader.close()
        channel.invokeMethod("onReaderDetached", null)

        val filterAttached = IntentFilter()
        filterAttached.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        context.registerReceiver(receiverAttached, filterAttached)
      }
    }
  }

  private val receiverAttached = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      Log.d(TAG, "USB Attached")
      channel.invokeMethod("onReaderAttached", null)
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

  private fun silentBuzzer(reader: Reader) {
    val command = byteArrayOf(0xFF.toByte(), 0x00.toByte(), 0x52.toByte(), 0x00.toByte(), 0x00.toByte())
    val response = ByteArray(256)
    reader.transmit(0, command, command.size, response, response.size)
  }

  /**
   * Only works if tag is connected
   */
  private fun alertSuccess(reader: Reader) {
    try {
      val binaryValue = "10001110".toInt(2)
      val command = byteArrayOf(0xFF.toByte(), 0x00.toByte(), 0x40.toByte(), binaryValue.toByte(), 0x04.toByte(), 0x01.toByte(), 0x01.toByte(), 0x01.toByte(), 0x02.toByte())
      val response = ByteArray(256)
      reader.transmit(0, command, command.size, response, response.size)
    } catch (exc: Exception) {
      Log.e(TAG, "Failed at alertSuccess")
      exc.message?.let { Log.e(TAG, it) }
    }
  }

  /**
   * Only works if tag is connected
   */
  private fun alertError(reader: Reader) {
    try {
      val binaryValue = "01001101".toInt(2)
      val command = byteArrayOf(0xFF.toByte(), 0x00.toByte(), 0x40.toByte(), binaryValue.toByte(), 0x04.toByte(), 0x01.toByte(), 0x01.toByte(), 0x02.toByte(), 0x02.toByte())
      val response = ByteArray(256)
      reader.transmit(0, command, command.size, response, response.size)
    } catch (exc: Exception) {
      Log.e(TAG, "Failed at alertSuccess")
      exc.message?.let { Log.e(TAG, it) }
    }
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    if (call.method == "startSession") {
      reader.setOnStateChangeListener { _, _, currState ->
        if (currState == Reader.CARD_PRESENT) {
          Log.d(TAG, "Found a card")
          val command = byteArrayOf(0xFF.toByte(), 0xCA.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte())
          Log.d(TAG, "Getting id...")
          reader.power(0, Reader.CARD_WARM_RESET)
          reader.setProtocol(0, Reader.PROTOCOL_T0 or Reader.PROTOCOL_T1)
          if (turnOffBuzzer) {
            silentBuzzer(reader)
          }
          val response = ByteArray(256)
          val responseLength: Int = reader.transmit(0, command, command.size, response, response.size)
          if (responseLength >= 2) {
            val idBytes = response.copyOf(responseLength - 2)
            val idNumber = bytesToDecimalArray(idBytes)
            var idHex = bytesToHex(idBytes)

            activity.runOnUiThread {
              val result = mapOf(
                "idNumber" to idNumber.joinToString(","),
                "idHex" to idHex
              )
              channel.invokeMethod("onDiscovered", result)
            }
            Log.d(TAG, "Success getting id : ${idNumber.joinToString(",")}")
          } else {
            Log.d(TAG, "Failed getting id")
          }
        }
      }
      startNFCScanner()
    } else if (call.method == "stopSession") {
      reader.close();
    } else if (call.method == "alertSuccess") {
      alertSuccess(reader)
    } else if (call.method == "alertError") {
      alertError(reader)
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

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivity() {
    // no op
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {
    // no op
  }
}
