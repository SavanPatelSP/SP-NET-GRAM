import 'package:flutter/material.dart';
import 'api_client.dart';
import 'iap_service.dart';
import 'models.dart';
import 'app_config.dart';

final ApiClient apiClient = ApiClient(
  baseUrlProvider: () => AppConfig.backendUrl,
  tokenProvider: () => AppConfig.sessionToken,
);
final IapService iapService = IapService(apiClient: apiClient);

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  iapService.init();
  runApp(const SpNetGramApp());
}

class SpNetGramApp extends StatelessWidget {
  const SpNetGramApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SP NET GRAM',
      theme: ThemeData(
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF42D9C8),
          secondary: Color(0xFFF7B733),
          surface: Color(0xFF0F1A2B),
        ),
        scaffoldBackgroundColor: const Color(0xFF0B1221),
        textTheme: const TextTheme(
          titleLarge: TextStyle(fontWeight: FontWeight.w700),
          bodyMedium: TextStyle(color: Color(0xFFCAD6F0)),
        ),
        useMaterial3: true,
      ),
      home: const HomeShell(),
    );
  }
}

class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;

  final _pages = const [
    ChatsPage(),
    AssistantPage(),
    SpgIdPage(),
    PremiumPage(),
    WalletPage(),
    SettingsPage(),
  ];

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final isWide = constraints.maxWidth >= 900;
        if (isWide) {
          return Scaffold(
            body: Row(
              children: [
                NavigationRail(
                  backgroundColor: const Color(0xFF0B1221),
                  selectedIndex: _index,
                  onDestinationSelected: (value) => setState(() => _index = value),
                  labelType: NavigationRailLabelType.all,
                  destinations: const [
                    NavigationRailDestination(
                      icon: Icon(Icons.chat_bubble_outline),
                      label: Text('Chats'),
                    ),
                    NavigationRailDestination(
                      icon: Icon(Icons.auto_awesome),
                      label: Text('Assistant'),
                    ),
                    NavigationRailDestination(
                      icon: Icon(Icons.verified_user_outlined),
                      label: Text('SPG ID'),
                    ),
                    NavigationRailDestination(
                      icon: Icon(Icons.workspace_premium_outlined),
                      label: Text('Premium'),
                    ),
                    NavigationRailDestination(
                      icon: Icon(Icons.account_balance_wallet_outlined),
                      label: Text('Wallet'),
                    ),
                    NavigationRailDestination(
                      icon: Icon(Icons.settings_outlined),
                      label: Text('Settings'),
                    ),
                  ],
                ),
                const VerticalDivider(width: 1),
                Expanded(child: _pages[_index]),
              ],
            ),
          );
        }
        return Scaffold(
          body: _pages[_index],
          bottomNavigationBar: BottomNavigationBar(
            currentIndex: _index,
            onTap: (value) => setState(() => _index = value),
            type: BottomNavigationBarType.fixed,
            items: const [
              BottomNavigationBarItem(icon: Icon(Icons.chat_bubble_outline), label: 'Chats'),
              BottomNavigationBarItem(icon: Icon(Icons.auto_awesome), label: 'Assistant'),
              BottomNavigationBarItem(icon: Icon(Icons.verified_user_outlined), label: 'SPG ID'),
              BottomNavigationBarItem(icon: Icon(Icons.workspace_premium_outlined), label: 'Premium'),
              BottomNavigationBarItem(icon: Icon(Icons.account_balance_wallet_outlined), label: 'Wallet'),
              BottomNavigationBarItem(icon: Icon(Icons.settings_outlined), label: 'Settings'),
            ],
          ),
        );
      },
    );
  }
}

class ChatsPage extends StatelessWidget {
  const ChatsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return _ScaffoldPage(
      title: 'Chats',
      subtitle: 'Nova Squad · 8 members',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _SectionTitle('Pinned'),
          const SizedBox(height: 8),
          _ChatTile('Nova Squad', 'We ship the beta tonight.'),
          _ChatTile('SP NET Ops', 'Airdrop schedule updated.'),
          const SizedBox(height: 16),
          _SectionTitle('All Chats'),
          _ChatTile('Design Crew', 'Assistant card styles ready.'),
          _ChatTile('Launch Room', 'Countdown: T-5 days.'),
        ],
      ),
    );
  }
}

class AssistantPage extends StatelessWidget {
  const AssistantPage({super.key});

  @override
  Widget build(BuildContext context) {
    return _ScaffoldPage(
      title: 'Assistant',
      subtitle: 'Summaries · Smart Replies · Translate',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: const [
              _Chip('Summarize'),
              _Chip('Translate'),
              _Chip('Rewrite'),
              _Chip('Smart Replies'),
              _Chip('Action Items'),
            ],
          ),
          const SizedBox(height: 16),
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: const [
                Text('Assistant Chat', style: TextStyle(fontWeight: FontWeight.w600)),
                SizedBox(height: 8),
                Text('Ask about any thread or request a summary.'),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class SpgIdPage extends StatelessWidget {
  const SpgIdPage({super.key});

  @override
  Widget build(BuildContext context) {
    return _ScaffoldPage(
      title: 'SP NET GRAM ID',
      subtitle: 'Your portable identity',
      child: _Card(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            Text('SPG-4F9A-88X2', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w700)),
            SizedBox(height: 8),
            Text('Handle: @spnetgram'),
            Text('Badges: Alpha · Builder'),
          ],
        ),
      ),
    );
  }
}

class PremiumPage extends StatelessWidget {
  const PremiumPage({super.key});

  @override
  Widget build(BuildContext context) {
    final plans = [
      PremiumPlan(
        id: 'free',
        name: 'Free',
        subtitle: 'Basic chat + limited assistant',
        price: 'Free',
        productId: null,
      ),
      PremiumPlan(
        id: 'plus',
        name: 'Plus',
        subtitle: 'Unlimited assistant + SPG badge',
        price: '\$4.99 / mo',
        productId: 'spnetgram_plus_android',
      ),
      PremiumPlan(
        id: 'pro',
        name: 'Pro',
        subtitle: 'Priority features + airdrop boosts',
        price: '\$9.99 / mo',
        productId: 'spnetgram_pro_android',
      ),
    ];

    return _ScaffoldPage(
      title: 'Premium Plans',
      subtitle: 'Unlock assistant boosts + perks',
      child: Column(
        children: plans
            .map((plan) => _PlanCard(
                  plan: plan,
                  onSelect: () => iapService.startPurchase(context, plan),
                ))
            .toList(),
      ),
    );
  }
}

class WalletPage extends StatelessWidget {
  const WalletPage({super.key});

  @override
  Widget build(BuildContext context) {
    return _ScaffoldPage(
      title: 'Wallet',
      subtitle: 'SP Coin + Gems',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: const [
          _Card(
            child: Text('SP Coin Balance: 12,480 SP'),
          ),
          SizedBox(height: 12),
          _Card(
            child: Text('Next Airdrop: in 04:12:33'),
          ),
          SizedBox(height: 12),
          _Card(
            child: Text('Gems Balance: 580 Gems'),
          ),
        ],
      ),
    );
  }
}

class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  late final TextEditingController _backendController;
  late final TextEditingController _tokenController;

  @override
  void initState() {
    super.initState();
    _backendController = TextEditingController(text: AppConfig.backendUrl);
    _tokenController = TextEditingController(text: AppConfig.sessionToken);
  }

  @override
  void dispose() {
    _backendController.dispose();
    _tokenController.dispose();
    super.dispose();
  }

  void _save() {
    AppConfig.backendUrl = _backendController.text.trim();
    AppConfig.sessionToken = _tokenController.text.trim();
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Settings saved.')),
    );
  }

  @override
  Widget build(BuildContext context) {
    return _ScaffoldPage(
      title: 'Settings',
      subtitle: 'Account · Privacy · Appearance',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Backend & Session', style: TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                TextField(
                  controller: _backendController,
                  decoration: const InputDecoration(labelText: 'Backend URL'),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: _tokenController,
                  decoration: const InputDecoration(labelText: 'Session Token'),
                ),
                const SizedBox(height: 12),
                ElevatedButton(onPressed: _save, child: const Text('Save')),
              ],
            ),
          ),
          const SizedBox(height: 12),
          const _Card(child: Text('Notifications')),
          const SizedBox(height: 12),
          const _Card(child: Text('Appearance')),
        ],
      ),
    );
  }
}

class _ScaffoldPage extends StatelessWidget {
  const _ScaffoldPage({
    required this.title,
    required this.subtitle,
    required this.child,
  });

  final String title;
  final String subtitle;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 4),
            Text(subtitle, style: Theme.of(context).textTheme.bodyMedium),
            const SizedBox(height: 20),
            Expanded(
              child: SingleChildScrollView(
                child: child,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ChatTile extends StatelessWidget {
  const _ChatTile(this.title, this.preview);

  final String title;
  final String preview;

  @override
  Widget build(BuildContext context) {
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          Text(preview, style: Theme.of(context).textTheme.bodyMedium),
        ],
      ),
    );
  }
}

class _PlanCard extends StatelessWidget {
  const _PlanCard({required this.plan, required this.onSelect});

  final PremiumPlan plan;
  final VoidCallback onSelect;

  @override
  Widget build(BuildContext context) {
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(plan.name, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 18)),
          const SizedBox(height: 6),
          Text(plan.subtitle, style: Theme.of(context).textTheme.bodyMedium),
          const SizedBox(height: 8),
          Text(plan.price, style: const TextStyle(fontWeight: FontWeight.w600)),
          const SizedBox(height: 10),
          Align(
            alignment: Alignment.centerLeft,
            child: ElevatedButton(
              onPressed: plan.id == 'free' ? null : onSelect,
              child: Text(plan.id == 'free' ? 'Current' : 'Upgrade'),
            ),
          ),
        ],
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(text, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16));
  }
}

class _Chip extends StatelessWidget {
  const _Chip(this.label);

  final String label;

  @override
  Widget build(BuildContext context) {
    return Chip(
      label: Text(label),
      backgroundColor: const Color(0xFF14263B),
      labelStyle: const TextStyle(color: Color(0xFFCAD6F0)),
    );
  }
}

class _Card extends StatelessWidget {
  const _Card({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF101F33),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0x222F415F)),
      ),
      child: child,
    );
  }
}
