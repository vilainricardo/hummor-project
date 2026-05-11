import 'package:flutter_test/flutter_test.dart';
import 'package:hummor_app/screens/login_screen.dart';

void main() {
  testWidgets('LoginScreen renders welcome text', (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(home: LoginScreen()),
    );
    expect(find.text('Bem-vindo(a)!'), findsOneWidget);
    expect(find.text('Entrar'), findsOneWidget);
  });
}
