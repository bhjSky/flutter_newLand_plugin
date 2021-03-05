
import 'dart:async';

import 'package:flutter/services.dart';


class PaymentResult {
  ///支付结果.
  final int type;
  final String responseCode;
  final String message;
  final String outOrderNo;
  final String outOrderTime;
  final String referenceNo;
  final String orderNo;
  final int amount;
  final int transAmount;
  final String transTime;
  final String cardNo;
  final String cardType;
  final String authCode;
  final String batcherNo;
  final String marchantId;
  final String teminalId;
  final String voucherNo;
  const PaymentResult({this.type, this.responseCode, this.message,
    this.outOrderNo, this.outOrderTime, this.referenceNo, this.orderNo, this.amount,
    this.transAmount, this.transTime,this.cardNo,this.cardType, this.authCode,
    this.batcherNo,this.marchantId,this.teminalId,this.voucherNo });
}

class FlutterNewLandPlugin {
  static const MethodChannel _channel =
      const MethodChannel('flutter_new_land_plugin');

  ///读取银行卡号
  Future<String> readBankCard() async {
    final dynamic result = await _channel.invokeMethod('readBankCard');
    return result;
  }

  ///开始打印.
  ///[text] 设置需要打印字符串.
  Future<void> startPrint(String text) async {
    await _channel.invokeMethod('startPrint', {"text": text});
  }

  ///关闭打印打印.
  Future<void> closePrint() async {
    await _channel.invokeMethod('closePrint');
  }

  ///初始化硬件.
  Future<void> init() async {
    await _channel.invokeMethod('init');
  }

  ///新大陆收单.
  /// [documentNumber] 订单编号.
  /// [documentTime] 订单时间.
  /// [amount] 支付金额.
  Future<PaymentResult> newLandPay(String documentNumber,String documentTime,String amount) async {
    final dynamic result = await _channel.invokeMethod('newLandPay', {"documentNumber": documentNumber,"documentTime":documentTime,"amount":amount});
    return PaymentResult(
      type: result['type'],
      responseCode: result['responseCode'],
      message: result['message'],
      outOrderNo: result['outOrderNo'],
      outOrderTime: result['outOrderTime'],
      referenceNo: result['referenceNo'],
      orderNo: result['orderNo'],
      amount: result['amount'],
      transAmount: result['transAmount'],
      transTime: result['transTime'],
      cardNo: result['cardNo'],
      cardType: result['cardType'],
      authCode: result['authCode'],
      batcherNo: result['batcherNo'],
      marchantId: result['marchantId'],
      teminalId: result['teminalId'],
      voucherNo: result['voucherNo'],
    );
  }
}
