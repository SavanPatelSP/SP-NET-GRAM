package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SpNetGramApi;
import org.telegram.messenger.SpNetGramConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ListView.AdapterWithDiffUtils;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Objects;

public class SpNetGramSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter adapter;

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_CHECK = 2;
    private static final int VIEW_TYPE_INFO = 3;

    private static final int ID_ACCESS_STATUS = 0;
    private static final int ID_ASSISTANT = 1;
    private static final int ID_SPG_ID = 2;
    private static final int ID_PREMIUM = 3;
    private static final int ID_SP_COIN = 4;
    private static final int ID_GEMS = 5;
    private static final int ID_REDEEM_LICENSE = 6;

    private static final int ID_GHOST = 10;
    private static final int ID_ANTI_REVOKE = 11;
    private static final int ID_HIDE_TYPING = 12;
    private static final int ID_NO_READ = 13;
    private static final int ID_QUICK_ACTIONS = 20;
    private static final int ID_TRANSLATE_BAR = 21;
    private static final int ID_EDIT_HISTORY = 22;
    private static final int ID_SPAM_FILTER = 23;
    private static final int ID_HIDE_SPONSORED = 24;
    private static final int ID_UI_REDESIGN = 25;
    private static final int ID_ENABLE_HIDDEN = 26;
    private static final int ID_ENABLE_LOCKS = 27;
    private static final int ID_MANAGE_HIDDEN = 30;

    private final ArrayList<ItemInner> oldItems = new ArrayList<>();
    private final ArrayList<ItemInner> items = new ArrayList<>();

    private static final int ACCESS_UNKNOWN = 0;
    private static final int ACCESS_ACTIVE = 1;
    private static final int ACCESS_LOCKED = 2;
    private int accessState = ACCESS_UNKNOWN;
    private String accessValueText = null;

    private final Theme.ResourcesProvider spnetResources = key -> {
        if (key == Theme.key_windowBackgroundGray) {
            return SpNetGramUi.COLOR_BG;
        } else if (key == Theme.key_windowBackgroundWhite) {
            return SpNetGramUi.COLOR_SURFACE;
        } else if (key == Theme.key_windowBackgroundWhiteBlackText) {
            return SpNetGramUi.COLOR_TEXT;
        } else if (key == Theme.key_windowBackgroundWhiteGrayText || key == Theme.key_windowBackgroundWhiteGrayText2) {
            return SpNetGramUi.COLOR_MUTED;
        } else if (key == Theme.key_windowBackgroundWhiteBlueText) {
            return SpNetGramUi.COLOR_ACCENT_2;
        } else if (key == Theme.key_windowBackgroundWhiteValueText) {
            return SpNetGramUi.COLOR_GOLD;
        } else if (key == Theme.key_windowBackgroundWhiteBlueHeader) {
            return SpNetGramUi.COLOR_ACCENT;
        } else if (key == Theme.key_windowBackgroundWhiteGrayIcon) {
            return SpNetGramUi.COLOR_MUTED;
        } else if (key == Theme.key_windowBackgroundWhiteLinkText) {
            return SpNetGramUi.COLOR_ACCENT_2;
        } else if (key == Theme.key_switchTrack || key == Theme.key_switchTrackChecked ||
                key == Theme.key_switch2Track || key == Theme.key_switch2TrackChecked) {
            return SpNetGramUi.COLOR_ACCENT;
        } else if (key == Theme.key_switchThumb || key == Theme.key_switchThumbChecked ||
                key == Theme.key_switch2Thumb || key == Theme.key_switch2ThumbChecked) {
            return SpNetGramUi.COLOR_GOLD;
        }
        return Theme.getColor(key);
    };

    @Override
    public boolean onFragmentCreate() {
        return super.onFragmentCreate();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAccessStatus();
    }

    @Override
    public View createView(Context context) {
        SpNetGramUi.applyActionBar(actionBar);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.SpNetGramSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(SpNetGramUi.COLOR_BG);

        listView = new RecyclerListView(context);
        listView.setSections();
        actionBar.setAdaptiveBackground(listView);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        });
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutAnimation(null);
        listView.setAdapter(adapter = new ListAdapter());
        listView.setBackgroundColor(SpNetGramUi.COLOR_BG);
        listView.setClipToPadding(false);
        listView.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setDurations(350);
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDelayAnimations(false);
        itemAnimator.setSupportsChangeAnimations(false);
        listView.setItemAnimator(itemAnimator);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= items.size()) {
                return;
            }
            ItemInner item = items.get(position);
            if (item.viewType == VIEW_TYPE_TEXT) {
                switch (item.id) {
                    case ID_ACCESS_STATUS:
                        presentFragment(new SpNetGramLicenseGateActivity(false));
                        break;
                    case ID_ASSISTANT:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_ASSISTANT));
                        break;
                    case ID_SPG_ID:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_SPG_ID));
                        break;
                    case ID_PREMIUM:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_PREMIUM));
                        break;
                    case ID_SP_COIN:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_SP_COIN));
                        break;
                    case ID_GEMS:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_GEMS));
                        break;
                    case ID_REDEEM_LICENSE:
                        presentFragment(new SpNetGramLicenseGateActivity(false));
                        break;
                    case ID_MANAGE_HIDDEN:
                        presentFragment(new SpNetGramHiddenChatsActivity());
                        break;
                }
                return;
            }
            if (item.viewType == VIEW_TYPE_CHECK && view instanceof TextCheckCell) {
                TextCheckCell cell = (TextCheckCell) view;
                boolean newValue = !cell.getCheckBox().isChecked();
                cell.getCheckBox().setChecked(newValue, true);
                if (item.id == ID_GHOST) {
                    SpNetGramConfig.setGhostMode(newValue);
                } else if (item.id == ID_ANTI_REVOKE) {
                    SpNetGramConfig.setAntiRevokeEnabled(newValue);
                } else if (item.id == ID_HIDE_TYPING) {
                    SpNetGramConfig.setHideTypingEnabled(newValue);
                } else if (item.id == ID_NO_READ) {
                    SpNetGramConfig.setNoReadReceiptsEnabled(newValue);
                } else if (item.id == ID_QUICK_ACTIONS) {
                    SpNetGramConfig.setQuickActionsEnabled(newValue);
                } else if (item.id == ID_TRANSLATE_BAR) {
                    SpNetGramConfig.setTranslateBarEnabled(newValue);
                } else if (item.id == ID_EDIT_HISTORY) {
                    SpNetGramConfig.setEditHistoryEnabled(newValue);
                } else if (item.id == ID_SPAM_FILTER) {
                    SpNetGramConfig.setSpamFilterEnabled(newValue);
                } else if (item.id == ID_HIDE_SPONSORED) {
                    SpNetGramConfig.setHideSponsoredEnabled(newValue);
                } else if (item.id == ID_UI_REDESIGN) {
                    SpNetGramConfig.setUiRedesignEnabled(newValue);
                } else if (item.id == ID_ENABLE_HIDDEN) {
                    SpNetGramConfig.setHiddenChatsEnabled(newValue);
                } else if (item.id == ID_ENABLE_LOCKS) {
                    SpNetGramConfig.setChatLocksEnabled(newValue);
                }
            }
        });

        updateItems(false);
        refreshAccessStatus();

        return fragmentView;
    }

    private boolean isToggleEnabled(int id) {
        if (id == ID_GHOST) {
            return SpNetGramConfig.isGhostMode();
        } else if (id == ID_ANTI_REVOKE) {
            return SpNetGramConfig.isAntiRevokeEnabled();
        } else if (id == ID_HIDE_TYPING) {
            return SpNetGramConfig.isHideTypingEnabled();
        } else if (id == ID_NO_READ) {
            return SpNetGramConfig.isNoReadReceiptsEnabled();
        } else if (id == ID_QUICK_ACTIONS) {
            return SpNetGramConfig.isQuickActionsEnabled();
        } else if (id == ID_TRANSLATE_BAR) {
            return SpNetGramConfig.isTranslateBarEnabled();
        } else if (id == ID_EDIT_HISTORY) {
            return SpNetGramConfig.isEditHistoryEnabled();
        } else if (id == ID_SPAM_FILTER) {
            return SpNetGramConfig.isSpamFilterEnabled();
        } else if (id == ID_HIDE_SPONSORED) {
            return SpNetGramConfig.isHideSponsoredEnabled();
        } else if (id == ID_UI_REDESIGN) {
            return SpNetGramConfig.isUiRedesignEnabled();
        } else if (id == ID_ENABLE_HIDDEN) {
            return SpNetGramConfig.isHiddenChatsEnabled();
        } else if (id == ID_ENABLE_LOCKS) {
            return SpNetGramConfig.isChatLocksEnabled();
        }
        return false;
    }

    private void updateItems(boolean animated) {
        oldItems.clear();
        oldItems.addAll(items);
        items.clear();

        if (accessValueText == null) {
            accessValueText = LocaleController.getString(R.string.SpNetGramAccessUnknown);
        }

        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_ACCESS_STATUS, LocaleController.getString(R.string.SpNetGramAccess), accessValueText));
        items.add(new ItemInner(VIEW_TYPE_HEADER, 0, LocaleController.getString(R.string.SpNetGramSectionTitle)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_ASSISTANT, LocaleController.getString(R.string.SpNetGramAssistant)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_SPG_ID, LocaleController.getString(R.string.SpNetGramId)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_PREMIUM, LocaleController.getString(R.string.SpNetGramPremiumPlan)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_SP_COIN, LocaleController.getString(R.string.SpNetGramCoinAirdrop)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_GEMS, LocaleController.getString(R.string.SpNetGramGems)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_REDEEM_LICENSE, LocaleController.getString(R.string.SpNetGramRedeemLicense)));
        items.add(new ItemInner(VIEW_TYPE_INFO, 6, LocaleController.getString(R.string.SpNetGramFeaturesInfo)));

        items.add(new ItemInner(VIEW_TYPE_HEADER, 7, LocaleController.getString(R.string.SpNetGramPrivacySection)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_GHOST, LocaleController.getString(R.string.SpNetGramGhostMode), LocaleController.getString(R.string.SpNetGramGhostModeInfo)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_ANTI_REVOKE, LocaleController.getString(R.string.SpNetGramAntiRevoke), LocaleController.getString(R.string.SpNetGramAntiRevokeInfo)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_HIDE_TYPING, LocaleController.getString(R.string.SpNetGramHideTyping), LocaleController.getString(R.string.SpNetGramHideTypingInfo)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_NO_READ, LocaleController.getString(R.string.SpNetGramNoReadReceipts), LocaleController.getString(R.string.SpNetGramNoReadReceiptsInfo)));
        items.add(new ItemInner(VIEW_TYPE_INFO, 15, LocaleController.getString(R.string.SpNetGramPrivacyInfo)));

        items.add(new ItemInner(VIEW_TYPE_HEADER, 16, LocaleController.getString(R.string.SpNetGramChatSection)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_QUICK_ACTIONS, LocaleController.getString(R.string.SpNetGramQuickActions), LocaleController.getString(R.string.SpNetGramQuickActionsInfo)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_TRANSLATE_BAR, LocaleController.getString(R.string.SpNetGramTranslateBar), LocaleController.getString(R.string.SpNetGramTranslateBarInfo)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_EDIT_HISTORY, LocaleController.getString(R.string.SpNetGramEditHistory), LocaleController.getString(R.string.SpNetGramEditHistoryInfo)));
        items.add(new ItemInner(VIEW_TYPE_INFO, 20, LocaleController.getString(R.string.SpNetGramChatSectionInfo)));

        items.add(new ItemInner(VIEW_TYPE_HEADER, 21, LocaleController.getString(R.string.SpNetGramFiltersSection)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_SPAM_FILTER, LocaleController.getString(R.string.SpNetGramSpamFilter), LocaleController.getString(R.string.SpNetGramSpamFilterInfo)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_HIDE_SPONSORED, LocaleController.getString(R.string.SpNetGramHideSponsored), LocaleController.getString(R.string.SpNetGramHideSponsoredInfo)));
        items.add(new ItemInner(VIEW_TYPE_INFO, 24, LocaleController.getString(R.string.SpNetGramFiltersSectionInfo)));

        items.add(new ItemInner(VIEW_TYPE_HEADER, 25, LocaleController.getString(R.string.SpNetGramSecuritySection)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_ENABLE_HIDDEN, LocaleController.getString(R.string.SpNetGramHiddenChatsToggle), LocaleController.getString(R.string.SpNetGramHiddenChatsInfo)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_MANAGE_HIDDEN, LocaleController.getString(R.string.SpNetGramManageHiddenChats)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_ENABLE_LOCKS, LocaleController.getString(R.string.SpNetGramChatLocks), LocaleController.getString(R.string.SpNetGramChatLocksInfo)));
        items.add(new ItemInner(VIEW_TYPE_INFO, 28, LocaleController.getString(R.string.SpNetGramSecuritySectionInfo)));

        items.add(new ItemInner(VIEW_TYPE_HEADER, 29, LocaleController.getString(R.string.SpNetGramUiSection)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_UI_REDESIGN, LocaleController.getString(R.string.SpNetGramUiRedesign), LocaleController.getString(R.string.SpNetGramUiRedesignInfo)));
        items.add(new ItemInner(VIEW_TYPE_INFO, 32, LocaleController.getString(R.string.SpNetGramUiSectionInfo)));

        if (adapter == null) {
            return;
        }

        if (animated) {
            adapter.setItems(oldItems, items);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private static class ItemInner extends AdapterWithDiffUtils.Item {
        public final CharSequence text;
        public final CharSequence value;
        public final int id;

        ItemInner(int viewType, int id, CharSequence text) {
            this(viewType, id, text, null);
        }

        ItemInner(int viewType, int id, CharSequence text, CharSequence value) {
            super(viewType, false);
            this.id = id;
            this.text = text;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemInner item = (ItemInner) o;
            return id == item.id && viewType == item.viewType && Objects.equals(text, item.text) && Objects.equals(value, item.value);
        }
    }

    private class ListAdapter extends AdapterWithDiffUtils {
        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (position < 0 || position >= items.size()) {
                return VIEW_TYPE_INFO;
            }
            return items.get(position).viewType;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position < 0 || position >= items.size()) {
                return false;
            }
            int type = items.get(position).viewType;
            return type == VIEW_TYPE_TEXT || type == VIEW_TYPE_CHECK;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_HEADER) {
                view = new HeaderCell(getContext(), spnetResources);
            } else if (viewType == VIEW_TYPE_TEXT) {
                view = new TextSettingsCell(getContext(), spnetResources);
            } else if (viewType == VIEW_TYPE_CHECK) {
                view = new TextCheckCell(getContext(), spnetResources);
            } else {
                view = new TextInfoPrivacyCell(getContext(), spnetResources);
            }
            if (viewType == VIEW_TYPE_TEXT || viewType == VIEW_TYPE_CHECK) {
                view.setBackground(SpNetGramUi.createCardBackground(14, true));
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (position < 0 || position >= items.size()) {
                return;
            }
            ItemInner item = items.get(position);
            final boolean divider = position + 1 < items.size() && items.get(position + 1).viewType == item.viewType;
            if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
                ((HeaderCell) holder.itemView).setText(item.text);
            } else if (holder.getItemViewType() == VIEW_TYPE_TEXT) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                if (item.value != null) {
                    cell.setTextAndValue(item.text, item.value, divider);
                    if (item.id == ID_ACCESS_STATUS) {
                        int color = Theme.getColor(Theme.key_windowBackgroundWhiteValueText);
                        if (accessState == ACCESS_ACTIVE) {
                            color = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
                        } else if (accessState == ACCESS_LOCKED) {
                            color = Theme.getColor(Theme.key_text_RedBold);
                        }
                        cell.setTextValueColor(color);
                    }
                } else {
                    cell.setText(item.text, divider);
                }
            } else if (holder.getItemViewType() == VIEW_TYPE_CHECK) {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                String value = item.value != null ? item.value.toString() : "";
                cell.setTextAndValueAndCheck(item.text.toString(), value, isToggleEnabled(item.id), true, divider);
            } else {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                cell.setText(item.text);
                cell.setFixedSize(0);
            }
        }
    }

    private void refreshAccessStatus() {
        String token = SpNetGramConfig.getBackendToken();
        if (token == null || token.isEmpty()) {
            accessState = ACCESS_LOCKED;
            accessValueText = buildAccessValue(false, null);
            updateItems(true);
            return;
        }
        SpNetGramApi.accessStatus(token, json -> {
            if (json == null) {
                accessState = ACCESS_UNKNOWN;
                accessValueText = buildAccessValue(null, null);
            } else {
                boolean canUse = json.optBoolean("canUse", false);
                String expiresAt = null;
                if (json.has("premium")) {
                    try {
                        expiresAt = json.getJSONObject("premium").optString("expiresAt", null);
                    } catch (Exception ignore) {
                        expiresAt = null;
                    }
                }
                accessState = canUse ? ACCESS_ACTIVE : ACCESS_LOCKED;
                accessValueText = buildAccessValue(canUse, expiresAt);
            }
            updateItems(true);
        });
    }

    private String buildAccessValue(Boolean canUse, String expiresAt) {
        String status;
        if (canUse == null) {
            status = LocaleController.getString(R.string.SpNetGramAccessUnknown);
        } else if (canUse) {
            status = LocaleController.getString(R.string.SpNetGramAccessActive);
        } else {
            status = LocaleController.getString(R.string.SpNetGramAccessLocked);
        }

        String expiryText;
        if (expiresAt == null || expiresAt.isEmpty()) {
            expiryText = LocaleController.getString(R.string.SpNetGramAccessNoExpiry);
        } else {
            String date = expiresAt.length() >= 10 ? expiresAt.substring(0, 10) : expiresAt;
            expiryText = LocaleController.formatString(R.string.SpNetGramAccessExpires, date);
        }

        return status + " · " + expiryText;
    }
}
