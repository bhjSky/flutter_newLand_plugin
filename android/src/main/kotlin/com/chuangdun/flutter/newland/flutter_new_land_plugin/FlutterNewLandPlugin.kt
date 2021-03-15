package com.chuangdun.flutter.newland.flutter_new_land_plugin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.NonNull
import com.chuangdun.flutter.newland.device.ConnectionCallback
import com.chuangdun.flutter.newland.device.N900Manager
import com.chuangdun.flutter.newland.utils.BigDecimalUtils
import com.google.common.base.Preconditions
import com.google.common.base.Strings
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.newland.mtype.module.common.printer.PrintContext
import com.newland.mtype.module.common.printer.Printer
import com.newland.mtype.module.common.printer.PrinterStatus
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.math.BigDecimal
import java.nio.charset.Charset
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


/** FlutterNewLandPlugin */
private const val TAG = "FlutterNewLandPlugin"
private const val REQUEST_SWIPE = 444
private const val REQUEST_PAY = 222
private const val SUCCESS = "00"
class FlutterNewLandPlugin: FlutterPlugin, MethodCallHandler, ActivityAware ,ConnectionCallback{
  private lateinit var channel : MethodChannel
  private lateinit var result: Result
  private lateinit var activity: Activity
  private lateinit var context: Context
  private var printer: Printer? = null
  private var mN900Manager: N900Manager? = null

  private val factory = ThreadFactoryBuilder().setNameFormat("print-pool-%d").build()
//  private val singleThreadPool: ExecutorService = ThreadPoolExecutor(1, 1, 0L,
//          TimeUnit.MILLISECONDS, LinkedBlockingDeque(1024), factory)


  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_new_land_plugin")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    this.result = result
    when(call.method){
      "readBankCard" -> {
        val intent = Intent()
        intent.action = activity.getString(R.string.ACTION_SWIPE)
        intent.putExtra("transType", REQUEST_SWIPE)
        activity.startActivityForResult(intent, REQUEST_SWIPE)
      }
      "init" -> {
        mN900Manager = N900Manager(context, this)
        mN900Manager!!.connect()
      }
      "startPrint"-> {
        val arguments = call.arguments as Map<*, *>
        val script = arguments["text"] as String
        try {
          printer!!.init()
          Preconditions.checkArgument(printer!!.status == PrinterStatus.NORMAL)
          val charset = Charset.forName("gbk")
          val code = printer!!.printByScript(PrintContext.defaultContext(), script.toByteArray(charset), 60, TimeUnit.SECONDS)
          Log.i(TAG, "-------------------------打印结果:$code")
          result.success(true)
        } catch (e0: java.lang.IllegalArgumentException) {
          try {
            Preconditions.checkArgument(printer!!.status == PrinterStatus.OUTOF_PAPER)
            result.error("PAPER_GONE","您的打印纸不够了,请重新放入打印纸后再试!",null)
          } catch (e1: java.lang.IllegalArgumentException) {
            result.error("PRINT_FAIL","打印失败!打印机状态不正常!",null)
          }
        }
      }
      "closePrint" -> {
        mN900Manager!!.disconnect()
      }
      "newLandPay" -> {
        val arguments = call.arguments as Map<*, *>
        val trans: Long = BigDecimalUtils.scale2RoundHalfUp(
                BigDecimal(arguments["amount"] as String).multiply(BigDecimal("100.00"))).toLong()
        val orderNo: String = arguments["documentNumber"] as String
        var orderTime: String? = arguments["documentTime"] as String
        orderTime = try {
          val temporary = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).parse(orderTime)
          SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(temporary)
        } catch (e: ParseException) {
          SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(Date())
        }
        val link: String = activity.getString(R.string.URI_PAYMENT, "acquire", orderNo, orderTime, trans)
        Log.d(TAG, "调用收单接口: $link")
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        activity.startActivityForResult(intent, REQUEST_PAY)
      }
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onDetachedFromActivity() {
    TODO("Not yet implemented")
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    TODO("Not yet implemented")
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    binding.addActivityResultListener(PluginRegistry.ActivityResultListener { requestCode, resultCode, data ->
      when (requestCode) {
        REQUEST_SWIPE -> {
          val bankNo = data.getStringExtra("cardPan")
          try {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(bankNo), "读取卡号失败.")
            this.result.success(bankNo)
          } catch (e: IllegalArgumentException) {
            Log.w(TAG,e.message)
          }
        }
        REQUEST_PAY -> {
          val responseCode = data.getStringExtra("responseCode")
          val paymentResult = mutableMapOf<String, Any>()
          paymentResult["responseCode"] = responseCode
          try {
            Preconditions.checkArgument(SUCCESS == responseCode)
            paymentResult["message"] = data.getStringExtra("message")
            paymentResult["outOrderNo"] = data.getStringExtra("outOrderNo")
            paymentResult["outOrderTime"] = data.getStringExtra("outOrderTime")
            paymentResult["referenceNo"] = data.getStringExtra("referenceNo")
            paymentResult["orderNo"] = data.getStringExtra("orderNo")
            paymentResult["amount"] = data.getLongExtra("amount", 0L)
            paymentResult["transAmount"] = data.getLongExtra("transAmount", 0L)
            paymentResult["transTime"] = data.getStringExtra("transTime")
            paymentResult["cardNo"] = data.getStringExtra("cardNo")
            paymentResult["cardType"] = data.getStringExtra("cardType")
            paymentResult["authCode"] = data.getStringExtra("authCode")
            paymentResult["batcherNo"] = data.getStringExtra("batcherNo")
            paymentResult["marchantId"] = data.getStringExtra("marchantId")
            paymentResult["teminalId"] = data.getStringExtra("teminalId")
            paymentResult["voucherNo"] = data.getStringExtra("voucherNo")
            paymentResult["type"] = 1
            result.success(paymentResult)
          } catch (e: java.lang.IllegalArgumentException) {
            paymentResult["type"] = 2
            result.success(paymentResult)
          }
        }
      }
      false
    })
  }

  override fun onDetachedFromActivityForConfigChanges() {
    TODO("Not yet implemented")
  }

  override fun onConnecting() {
  }

  override fun onConnected() {
    if (mN900Manager != null) {
      printer = mN900Manager!!.printerModule
      result.success(true)
    }
  }

  override fun onDisconnected() {
  }

  override fun onError(error: String?) {
    result.error("INIT_ERROR", error,null)
  }
}
