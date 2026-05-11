import 'package:flutter_test/flutter_test.dart';
import 'package:hummor_app/main.dart';

void main() {
  testWidgets('shows Hello, world!', (WidgetTester tester) async {
    await tester.pumpWidget(const HummorApp());
    expect(find.text('Hello, world!'), findsOneWidget);
  });
}
