import { Api, TelegramClient } from "telegram";
import { StringSession } from "telegram/sessions";

const SESSION_KEY = "spg_tg_session";

export class MtprotoClient {
  constructor({ apiId, apiHash, sessionKey = SESSION_KEY }) {
    this.apiId = apiId;
    this.apiHash = apiHash;
    this.sessionKey = sessionKey;
    this.client = null;
    this.connected = false;
    this.peerIndex = new Map();
  }

  async connect() {
    if (this.client) {
      return;
    }
    const stored = localStorage.getItem(this.sessionKey) || "";
    const session = new StringSession(stored);
    this.client = new TelegramClient(session, this.apiId, this.apiHash, {
      connectionRetries: 5,
    });
    await this.client.connect();
    this.connected = true;
  }

  async isAuthorized() {
    await this.connect();
    return this.client.checkAuthorization();
  }

  async getMe() {
    await this.connect();
    const user = await this.client.getMe();
    return this._userSummary(user);
  }

  async sendCode(phone) {
    await this.connect();
    const result = await this.client.sendCode(
      {
        apiId: this.apiId,
        apiHash: this.apiHash,
      },
      phone,
    );
    this.phoneCodeHash = result.phoneCodeHash;
    return {
      phoneCodeHash: result.phoneCodeHash,
      isCodeViaApp: result.isCodeViaApp,
    };
  }

  async signIn({ phone, code, phoneCodeHash }) {
    await this.connect();
    const result = await this.client.invoke(
      new Api.auth.SignIn({
        phoneNumber: phone,
        phoneCodeHash,
        phoneCode: code,
      }),
    );
    this._persistSession();
    return result.user ? this._userSummary(result.user) : result;
  }

  async signInWithPassword(password) {
    await this.connect();
    const user = await this.client.signInWithPassword(
      {
        apiId: this.apiId,
        apiHash: this.apiHash,
      },
      {
        password: async () => password,
        onError: async () => true,
      },
    );
    this._persistSession();
    return this._userSummary(user);
  }

  async getChats() {
    await this.connect();
    const dialogs = await this.client.invoke(
      new Api.messages.GetDialogs({
        offsetDate: 0,
        offsetId: 0,
        offsetPeer: new Api.InputPeerEmpty(),
        limit: 50,
        hash: BigInt(0),
      }),
    );

    const entities = new Map();
    (dialogs.users || []).forEach((user) => entities.set(user.id.toString(), user));
    (dialogs.chats || []).forEach((chat) => entities.set(chat.id.toString(), chat));

    const messageMap = new Map();
    (dialogs.messages || []).forEach((message) => messageMap.set(message.id, message));

    const chats = (dialogs.dialogs || [])
      .map((dialog) => {
        const peerId = this._peerToId(dialog.peer);
        const entity = entities.get(peerId.toString());
        const topMessage = messageMap.get(dialog.topMessage);
        const title = entity
          ? entity.title || [entity.firstName, entity.lastName].filter(Boolean).join(" ")
          : "Unknown chat";
        const summary = {
          id: peerId,
          title,
          lastMessage: topMessage?.message || "",
          unreadCount: dialog.unreadCount || 0,
          peer: dialog.peer,
          kind: this._peerKind(dialog.peer),
        };
        this.peerIndex.set(peerId.toString(), dialog.peer);
        return summary;
      })
      .filter((chat) => chat.id);

    return chats;
  }

  async getMessages(chatId) {
    await this.connect();
    const peer = this.peerIndex.get(chatId.toString()) ?? chatId;
    const messages = await this.client.getMessages(peer, { limit: 40 });
    return messages
      .map((message) => ({
        id: message.id,
        chatId,
        from: message.out ? "you" : "them",
        sender: message.senderId?.toString() || "",
        text: message.message || this._mediaLabel(message),
        timestamp: this._toDate(message.date),
      }))
      .reverse();
  }

  async sendMessage(chatId, text) {
    await this.connect();
    const peer = this.peerIndex.get(chatId.toString()) ?? chatId;
    await this.client.sendMessage(peer, { message: text });
  }

  async logout() {
    try {
      if (this.client) {
        await this.client.disconnect();
      }
    } finally {
      this.client = null;
      this.connected = false;
      localStorage.removeItem(this.sessionKey);
      this.peerIndex.clear();
    }
  }

  _peerToId(peer) {
    if (peer instanceof Api.PeerUser) {
      return peer.userId;
    }
    if (peer instanceof Api.PeerChat) {
      return peer.chatId;
    }
    if (peer instanceof Api.PeerChannel) {
      return peer.channelId;
    }
    return BigInt(0);
  }

  _peerKind(peer) {
    if (peer instanceof Api.PeerUser) return "User";
    if (peer instanceof Api.PeerChat) return "Group";
    if (peer instanceof Api.PeerChannel) return "Channel";
    return "Chat";
  }

  _mediaLabel(message) {
    if (message.photo) return "[Photo]";
    if (message.document) return "[File]";
    if (message.media) return "[Media]";
    return "";
  }

  _userSummary(user) {
    if (!user) return null;
    return {
      id: user.id?.toString?.() || "",
      firstName: user.firstName || "",
      lastName: user.lastName || "",
      username: user.username || "",
      phone: user.phone || "",
      displayName: [user.firstName, user.lastName].filter(Boolean).join(" ") || user.username || "Telegram user",
    };
  }

  _toDate(value) {
    if (value instanceof Date) {
      return value.toISOString();
    }
    if (typeof value === "number") {
      return new Date(value * 1000).toISOString();
    }
    return new Date().toISOString();
  }

  _persistSession() {
    const session = this.client?.session?.save?.();
    if (session) {
      localStorage.setItem(this.sessionKey, session);
    }
  }
}
