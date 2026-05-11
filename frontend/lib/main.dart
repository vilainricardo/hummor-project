import 'package:flutter/material.dart';

void main() {
  runApp(const HummorApp());
}

class HummorApp extends StatelessWidget {
  const HummorApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Hummor',
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        body: Center(
          child: Text(
            'Hello, world!',
            style: Theme.of(context).textTheme.headlineMedium,
          ),
        ),
      ),
    );
  }
}
