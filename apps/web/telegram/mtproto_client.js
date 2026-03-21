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
    return { phoneCodeHash: result.phoneCodeHash };
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
    return result;
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
    (dialogs.users || []).forEach((user) => entities.set(user.id, user));
    (dialogs.chats || []).forEach((chat) => entities.set(chat.id, chat));

    const messageMap = new Map();
    (dialogs.messages || []).forEach((message) => messageMap.set(message.id, message));

    const chats = (dialogs.dialogs || []).map((dialog) => {
      const peerId = this._peerToId(dialog.peer);
      const entity = entities.get(peerId);
      const title = entity
        ? entity.title || [entity.firstName, entity.lastName].filter(Boolean).join(" ")
        : "Unknown";
      const lastMessage = messageMap.get(dialog.topMessage)?.message || "";
      const summary = {
        id: peerId,
        title,
        lastMessage,
        unreadCount: dialog.unreadCount || 0,
        peer: dialog.peer,
      };
      this.peerIndex.set(peerId, dialog.peer);
      return summary;
    });

    return chats;
  }

  async getMessages(chatId) {
    await this.connect();
    const peer = this.peerIndex.get(chatId) ?? chatId;
    const messages = await this.client.getMessages(peer, { limit: 30 });
    return messages.map((message) => ({
      id: message.id,
      chatId,
      sender: message.senderId?.toString() || "Unknown",
      text: message.message || "",
      timestamp: this._toDate(message.date),
      isOutgoing: Boolean(message.out),
    }));
  }

  async sendMessage(chatId, text) {
    await this.connect();
    const peer = this.peerIndex.get(chatId) ?? chatId;
    await this.client.sendMessage(peer, { message: text });
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
    return 0;
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
