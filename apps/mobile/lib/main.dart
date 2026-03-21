import 'package:flutter/material.dart';
import 'api_client.dart';
import 'iap_service.dart';
import 'models.dart';
import 'app_config.dart';
import 'telegram/telegram_controller.dart';
import 'tdlib/tdlib_models.dart';

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
  late final TelegramController _telegramController;

  @override
  void initState() {
    super.initState();
    _telegramController = TelegramController();
    _telegramController.initialize();
  }

  @override
  void dispose() {
    _telegramController.dispose();
    super.dispose();
  }

  List<Widget> _buildPages() {
    return [
      ChatsPage(controller: _telegramController),
      const AssistantPage(),
      const SpgIdPage(),
      const PremiumPage(),
      const WalletPage(),
      const SettingsPage(),
    ];
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final isWide = constraints.maxWidth >= 900;
        final pages = _buildPages();
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
                Expanded(child: pages[_index]),
              ],
            ),
          );
        }
        return Scaffold(
          body: pages[_index],
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

class ChatsPage extends StatefulWidget {
  const ChatsPage({super.key, required this.controller});

  final TelegramController controller;

  @override
  State<ChatsPage> createState() => _ChatsPageState();
}

class _ChatsPageState extends State<ChatsPage> {
  final TextEditingController _phoneController = TextEditingController();
  final TextEditingController _codeController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _firstNameController = TextEditingController();
  final TextEditingController _lastNameController = TextEditingController();
  final TextEditingController _messageController = TextEditingController();

  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_onUpdate);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onUpdate);
    _phoneController.dispose();
    _codeController.dispose();
    _passwordController.dispose();
    _firstNameController.dispose();
    _lastNameController.dispose();
    _messageController.dispose();
    super.dispose();
  }

  void _onUpdate() => setState(() {});

  @override
  Widget build(BuildContext context) {
    final auth = widget.controller.authState;
    if (auth.status != 'READY') {
      return _ScaffoldPage(
        title: 'Chats',
        subtitle: 'Telegram login required',
        child: _buildAuthPanel(auth),
      );
    }
    return _ScaffoldPage(
      title: 'Chats',
      subtitle: 'Telegram connected',
      child: _buildChatPanel(),
    );
  }

  Widget _buildAuthPanel(auth) {
    final status = auth.status;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _Card(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Status: $status', style: const TextStyle(fontWeight: FontWeight.w600)),
              if (auth.message != null) ...[
                const SizedBox(height: 6),
                Text(auth.message!, style: Theme.of(context).textTheme.bodyMedium),
              ],
            ],
          ),
        ),
        const SizedBox(height: 12),
        if (status == 'WAIT_PHONE') ...[
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Phone Login', style: TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                TextField(
                  controller: _phoneController,
                  decoration: const InputDecoration(labelText: '+91…'),
                ),
                const SizedBox(height: 8),
                ElevatedButton(
                  onPressed: () => widget.controller.sendPhoneNumber(_phoneController.text.trim()),
                  child: const Text('Send Code'),
                ),
              ],
            ),
          ),
        ],
        if (status == 'WAIT_CODE') ...[
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Verify Code', style: TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                TextField(
                  controller: _codeController,
                  decoration: const InputDecoration(labelText: 'Code'),
                ),
                const SizedBox(height: 8),
                ElevatedButton(
                  onPressed: () => widget.controller.sendOtp(_codeController.text.trim()),
                  child: const Text('Verify'),
                ),
              ],
            ),
          ),
        ],
        if (status == 'WAIT_PASSWORD') ...[
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Two-factor Password', style: TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                TextField(
                  controller: _passwordController,
                  obscureText: true,
                  decoration: const InputDecoration(labelText: 'Password'),
                ),
                const SizedBox(height: 8),
                ElevatedButton(
                  onPressed: () => widget.controller.sendPassword(_passwordController.text.trim()),
                  child: const Text('Submit'),
                ),
              ],
            ),
          ),
        ],
        if (status == 'WAIT_DEVICE_CONFIRMATION') ...[
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Confirm on another device', style: TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                Text(auth.message ?? 'Open Telegram and confirm this login.'),
              ],
            ),
          ),
        ],
        if (status == 'WAIT_REGISTRATION') ...[
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Create Telegram account', style: TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                TextField(
                  controller: _firstNameController,
                  decoration: const InputDecoration(labelText: 'First name'),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: _lastNameController,
                  decoration: const InputDecoration(labelText: 'Last name'),
                ),
                const SizedBox(height: 8),
                ElevatedButton(
                  onPressed: () => widget.controller.registerUser(
                    _firstNameController.text.trim(),
                    _lastNameController.text.trim(),
                  ),
                  child: const Text('Register'),
                ),
              ],
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildChatPanel() {
    final chats = widget.controller.chats;
    final activeId = widget.controller.activeChatId;
    final messages = activeId != null ? (widget.controller.messages[activeId] ?? []) : const [];

    return LayoutBuilder(
      builder: (context, constraints) {
        final isWide = constraints.maxWidth >= 900;
        final chatList = _Card(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Chats', style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              if (chats.isEmpty)
                const Text('No chats yet.'),
              ...chats.map((chat) {
                final selected = chat.id == activeId;
                return ListTile(
                  selected: selected,
                  title: Text(chat.title),
                  subtitle: Text(chat.lastMessage, maxLines: 1, overflow: TextOverflow.ellipsis),
                  onTap: () async {
                    widget.controller.setActiveChat(chat.id);
                    await widget.controller.fetchMessages(chat.id);
                  },
                );
              }).toList(),
            ],
          ),
        );

        final messagePanel = _Card(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                chats.firstWhere(
                  (c) => c.id == activeId,
                  orElse: () => const TdlibChatSummary(id: 0, title: 'Select a chat', lastMessage: '', unreadCount: 0),
                ).title,
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
              const SizedBox(height: 8),
              SizedBox(
                height: 320,
                child: ListView.builder(
                  reverse: true,
                  itemCount: messages.length,
                  itemBuilder: (context, index) {
                    final msg = messages[index];
                    final align = msg.isOutgoing ? Alignment.centerRight : Alignment.centerLeft;
                    final color = msg.isOutgoing ? const Color(0xFF1B8E7A) : const Color(0xFF1C2942);
                    return Align(
                      alignment: align,
                      child: Container(
                        margin: const EdgeInsets.symmetric(vertical: 4),
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: color,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(msg.text),
                      ),
                    );
                  },
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _messageController,
                      decoration: const InputDecoration(labelText: 'Message'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton(
                    onPressed: activeId == null
                        ? null
                        : () async {
                            final text = _messageController.text.trim();
                            if (text.isEmpty) return;
                            await widget.controller.sendMessage(activeId, text);
                            _messageController.clear();
                            await widget.controller.fetchMessages(activeId);
                          },
                    child: const Text('Send'),
                  ),
                ],
              ),
            ],
          ),
        );

        if (isWide) {
          return Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(child: chatList),
              const SizedBox(width: 12),
              Expanded(child: messagePanel),
            ],
          );
        }
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            chatList,
            const SizedBox(height: 12),
            messagePanel,
          ],
        );
      },
    );
  }
}

class AssistantPage extends StatefulWidget {
  const AssistantPage({super.key});

  @override
  State<AssistantPage> createState() => _AssistantPageState();
}

class _AssistantPageState extends State<AssistantPage> {
  final TextEditingController _controller = TextEditingController();
  final List<Map<String, String>> _thread = [
    {'role': 'assistant', 'content': 'Hi! Want a summary of your last chat?'},
  ];
  bool _busy = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _send(String intent) async {
    final text = _controller.text.trim();
    if (text.isEmpty) return;
    setState(() {
      _busy = true;
      _thread.add({'role': 'user', 'content': text});
      _controller.clear();
    });
    try {
      final reply = await apiClient.assistantChat(
        messages: _thread,
        intent: intent,
      );
      setState(() {
        _thread.add({'role': 'assistant', 'content': reply['reply']?.toString() ?? '—'});
      });
    } catch (error) {
      setState(() {
        _thread.add({'role': 'assistant', 'content': 'Assistant error: $error'});
      });
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

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
            children: [
              _Chip('Summarize', onTap: () => _send('summarize')),
              _Chip('Translate', onTap: () => _send('translate')),
              _Chip('Smart Replies', onTap: () => _send('smart_reply')),
              _Chip('Action Items', onTap: () => _send('general')),
            ],
          ),
          const SizedBox(height: 16),
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Assistant Chat', style: TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                SizedBox(
                  height: 240,
                  child: ListView.builder(
                    itemCount: _thread.length,
                    itemBuilder: (context, index) {
                      final item = _thread[index];
                      final isUser = item['role'] == 'user';
                      return Align(
                        alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
                        child: Container(
                          margin: const EdgeInsets.symmetric(vertical: 4),
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: isUser ? const Color(0xFF1B8E7A) : const Color(0xFF1C2942),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(item['content'] ?? ''),
                        ),
                      );
                    },
                  ),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _controller,
                        decoration: const InputDecoration(labelText: 'Ask the assistant'),
                      ),
                    ),
                    const SizedBox(width: 8),
                    ElevatedButton(
                      onPressed: _busy ? null : () => _send('general'),
                      child: Text(_busy ? '...' : 'Send'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class SpgIdPage extends StatefulWidget {
  const SpgIdPage({super.key});

  @override
  State<SpgIdPage> createState() => _SpgIdPageState();
}

class _SpgIdPageState extends State<SpgIdPage> {
  Map<String, dynamic>? _profile;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    try {
      final data = await apiClient.getProfile();
      setState(() {
        _profile = data;
        _loading = false;
      });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  Future<void> _mint() async {
    setState(() => _loading = true);
    try {
      final data = await apiClient.mintSpgId();
      setState(() {
        _profile = {...?_profile, 'spgId': data['spgId']};
        _loading = false;
      });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final spgId = _profile?['spgId']?.toString() ?? 'SPG-UNMINTED';
    return _ScaffoldPage(
      title: 'SP NET GRAM ID',
      subtitle: 'Your portable identity',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(spgId, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w700)),
                const SizedBox(height: 8),
                Text(_loading ? 'Loading…' : 'Handle: @spnetgram'),
                const Text('Badges: Alpha · Builder'),
              ],
            ),
          ),
          const SizedBox(height: 12),
          ElevatedButton(
            onPressed: _loading ? null : _mint,
            child: const Text('Mint New ID'),
          ),
        ],
      ),
    );
  }
}

class PremiumPage extends StatefulWidget {
  const PremiumPage({super.key});

  @override
  State<PremiumPage> createState() => _PremiumPageState();
}

class _PremiumPageState extends State<PremiumPage> {
  List<PremiumPlan> _plans = const [];
  String _currentPlan = 'free';
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final data = await apiClient.getPremiumPlans();
      final status = AppConfig.sessionToken.isEmpty
          ? {'planId': 'free'}
          : await apiClient.getPremiumStatus();
      final platform = Theme.of(context).platform;
      final key = platform == TargetPlatform.iOS ? 'ios' : 'android';
      final plans = (data['plans'] as List<dynamic>? ?? []).map((plan) {
        final map = plan as Map<String, dynamic>;
        final productId = (map['productIds'] as Map?)?[key]?.toString();
        return PremiumPlan(
          id: map['id']?.toString() ?? '',
          name: map['name']?.toString() ?? '',
          subtitle: (map['perks'] as List?)?.join(' · ') ?? '',
          price: map['price'] is num ? '\$${map['price']}' : map['price']?.toString() ?? '',
          productId: productId,
        );
      }).toList();
      setState(() {
        _plans = plans;
        _currentPlan = status['planId']?.toString() ?? 'free';
        _loading = false;
      });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  Future<void> _activate(PremiumPlan plan) async {
    if (plan.productId != null) {
      await iapService.startPurchase(context, plan);
      return;
    }
    final platform = Theme.of(context).platform == TargetPlatform.iOS ? 'ios' : 'android';
    await apiClient.activatePlan(planId: plan.id, platform: platform);
    await _load();
  }

  @override
  Widget build(BuildContext context) {
    final plans = _plans.isEmpty
        ? const [
            PremiumPlan(
              id: 'free',
              name: 'Free',
              subtitle: 'Basic chat + limited assistant',
              price: 'Free',
              productId: null,
            ),
          ]
        : _plans;

    return _ScaffoldPage(
      title: 'Premium Plans',
      subtitle: 'Unlock assistant boosts + perks',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Current: ${_currentPlan.toUpperCase()}'),
          const SizedBox(height: 12),
          if (_loading) const Text('Loading…'),
          ...plans.map((plan) => _PlanCard(
                plan: plan,
                onSelect: () => _activate(plan),
              )),
        ],
      ),
    );
  }
}

class WalletPage extends StatefulWidget {
  const WalletPage({super.key});

  @override
  State<WalletPage> createState() => _WalletPageState();
}

class _WalletPageState extends State<WalletPage> {
  Map<String, dynamic>? _wallet;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadWallet();
  }

  Future<void> _loadWallet() async {
    try {
      final data = await apiClient.getWallet();
      setState(() {
        _wallet = data;
        _loading = false;
      });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  Future<void> _claimAirdrop() async {
    setState(() => _loading = true);
    try {
      await apiClient.claimAirdrop();
      await _loadWallet();
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final spCoin = _wallet?['spCoin']?.toString() ?? '—';
    final gems = _wallet?['gems']?.toString() ?? '—';
    final airdrop = _wallet?['airdrop'] as Map<String, dynamic>?;
    final canClaim = airdrop?['canClaim'] == true;
    final nextClaim = airdrop?['nextClaimAt']?.toString() ?? '—';

    return _ScaffoldPage(
      title: 'Wallet',
      subtitle: 'SP Coin + Gems',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _Card(child: Text('SP Coin Balance: $spCoin SP')),
          const SizedBox(height: 12),
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(canClaim ? 'Airdrop Available' : 'Next Airdrop: $nextClaim'),
                const SizedBox(height: 8),
                ElevatedButton(
                  onPressed: _loading || !canClaim ? null : _claimAirdrop,
                  child: Text(_loading ? '...' : 'Claim'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          _Card(child: Text('Gems Balance: $gems Gems')),
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
  late final TextEditingController _loginEmailController;
  late final TextEditingController _loginPasswordController;
  late final TextEditingController _registerNameController;
  late final TextEditingController _registerEmailController;
  late final TextEditingController _registerPasswordController;
  String _status = 'Not connected';

  @override
  void initState() {
    super.initState();
    _backendController = TextEditingController(text: AppConfig.backendUrl);
    _tokenController = TextEditingController(text: AppConfig.sessionToken);
    _loginEmailController = TextEditingController();
    _loginPasswordController = TextEditingController();
    _registerNameController = TextEditingController();
    _registerEmailController = TextEditingController();
    _registerPasswordController = TextEditingController();
  }

  @override
  void dispose() {
    _backendController.dispose();
    _tokenController.dispose();
    _loginEmailController.dispose();
    _loginPasswordController.dispose();
    _registerNameController.dispose();
    _registerEmailController.dispose();
    _registerPasswordController.dispose();
    super.dispose();
  }

  void _save() {
    AppConfig.backendUrl = _backendController.text.trim();
    AppConfig.sessionToken = _tokenController.text.trim();
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Settings saved.')),
    );
  }

  Future<void> _login() async {
    final email = _loginEmailController.text.trim();
    final password = _loginPasswordController.text.trim();
    if (email.isEmpty || password.isEmpty) return;
    try {
      final data = await apiClient.login(email: email, password: password);
      setState(() {
        AppConfig.sessionToken = data['token']?.toString() ?? '';
        _tokenController.text = AppConfig.sessionToken;
        _status = 'Connected';
      });
    } catch (_) {
      setState(() => _status = 'Login failed');
    }
  }

  Future<void> _register() async {
    final name = _registerNameController.text.trim();
    final email = _registerEmailController.text.trim();
    final password = _registerPasswordController.text.trim();
    if (name.isEmpty || email.isEmpty || password.isEmpty) return;
    try {
      await apiClient.register(displayName: name, email: email, password: password);
      setState(() => _status = 'Registered. You can login.');
    } catch (_) {
      setState(() => _status = 'Register failed');
    }
  }

  void _logout() {
    setState(() {
      AppConfig.sessionToken = '';
      _tokenController.text = '';
      _status = 'Disconnected';
    });
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
                const SizedBox(height: 6),
                Text(_status),
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
                const SizedBox(height: 8),
                OutlinedButton(onPressed: _logout, child: const Text('Logout')),
              ],
            ),
          ),
          const SizedBox(height: 12),
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Login', style: TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                TextField(
                  controller: _loginEmailController,
                  decoration: const InputDecoration(labelText: 'Email'),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: _loginPasswordController,
                  obscureText: true,
                  decoration: const InputDecoration(labelText: 'Password'),
                ),
                const SizedBox(height: 8),
                ElevatedButton(onPressed: _login, child: const Text('Login')),
              ],
            ),
          ),
          const SizedBox(height: 12),
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Register', style: TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                TextField(
                  controller: _registerNameController,
                  decoration: const InputDecoration(labelText: 'Display name'),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: _registerEmailController,
                  decoration: const InputDecoration(labelText: 'Email'),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: _registerPasswordController,
                  obscureText: true,
                  decoration: const InputDecoration(labelText: 'Password'),
                ),
                const SizedBox(height: 8),
                ElevatedButton(onPressed: _register, child: const Text('Register')),
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
  const _Chip(this.label, {this.onTap});

  final String label;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return ActionChip(
      onPressed: onTap,
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
