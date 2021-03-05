import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_new_land_plugin/flutter_new_land_plugin.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  FlutterNewLandPlugin flutterNewLandPlugin;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    flutterNewLandPlugin = FlutterNewLandPlugin();
  }

  @override
  void dispose() {
    // TODO: implement dispose
    super.dispose();
    flutterNewLandPlugin.closePrint();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Column(
          children: [
            Text(_platformVersion),
            FlatButton(
                onPressed: () async {
                  flutterNewLandPlugin.readBankCard().then((value){
                    setState(() {
                      _platformVersion = value;
                    });
                  });
                },
                child: Text("读卡")),
            FlatButton(
                onPressed: () async {
                  flutterNewLandPlugin.init().then((value){
                    flutterNewLandPlugin.startPrint("!hz l\n !asc l\n!yspace 20\n !gray 7\n*text c 销售清单 \n*line" + "\n").then((value){

                    }).catchError((error){
                      print(error);
                    });
                  }).catchError((error){
                    print(error);
                  });

                },
                child: Text("打印")),
          ],
        ),
      ),
    );
  }
}
