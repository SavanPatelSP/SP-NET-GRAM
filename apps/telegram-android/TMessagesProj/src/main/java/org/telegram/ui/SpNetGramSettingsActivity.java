package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
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

    private static final String PREFS_NAME = "spnetgram_settings";
    private static final String KEY_GHOST_MODE = "ghost_mode";
    private static final String KEY_ANTI_REVOKE = "anti_revoke";
    private static final String KEY_HIDE_TYPING = "hide_typing";
    private static final String KEY_NO_READ = "no_read_receipts";

    private RecyclerListView listView;
    private ListAdapter adapter;
    private SharedPreferences prefs;

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_CHECK = 2;
    private static final int VIEW_TYPE_INFO = 3;

    private static final int ID_ASSISTANT = 1;
    private static final int ID_SPG_ID = 2;
    private static final int ID_PREMIUM = 3;
    private static final int ID_SP_COIN = 4;
    private static final int ID_GEMS = 5;

    private static final int ID_GHOST = 10;
    private static final int ID_ANTI_REVOKE = 11;
    private static final int ID_HIDE_TYPING = 12;
    private static final int ID_NO_READ = 13;

    private final ArrayList<ItemInner> oldItems = new ArrayList<>();
    private final ArrayList<ItemInner> items = new ArrayList<>();

    @Override
    public boolean onFragmentCreate() {
        prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getString(R.string.SpNetGramSettings));
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
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

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
                }
                return;
            }
            if (item.viewType == VIEW_TYPE_CHECK && view instanceof TextCheckCell) {
                TextCheckCell cell = (TextCheckCell) view;
                boolean newValue = !cell.getCheckBox().isChecked();
                cell.getCheckBox().setChecked(newValue, true);
                if (item.id == ID_GHOST) {
                    prefs.edit().putBoolean(KEY_GHOST_MODE, newValue).apply();
                } else if (item.id == ID_ANTI_REVOKE) {
                    prefs.edit().putBoolean(KEY_ANTI_REVOKE, newValue).apply();
                } else if (item.id == ID_HIDE_TYPING) {
                    prefs.edit().putBoolean(KEY_HIDE_TYPING, newValue).apply();
                } else if (item.id == ID_NO_READ) {
                    prefs.edit().putBoolean(KEY_NO_READ, newValue).apply();
                }
            }
        });

        updateItems(false);

        return fragmentView;
    }

    private boolean isEnabled(int id) {
        if (id == ID_GHOST) {
            return prefs.getBoolean(KEY_GHOST_MODE, false);
        } else if (id == ID_ANTI_REVOKE) {
            return prefs.getBoolean(KEY_ANTI_REVOKE, false);
        } else if (id == ID_HIDE_TYPING) {
            return prefs.getBoolean(KEY_HIDE_TYPING, false);
        } else if (id == ID_NO_READ) {
            return prefs.getBoolean(KEY_NO_READ, false);
        }
        return false;
    }

    private void updateItems(boolean animated) {
        oldItems.clear();
        oldItems.addAll(items);
        items.clear();

        items.add(new ItemInner(VIEW_TYPE_HEADER, 0, getString(R.string.SpNetGramSectionTitle)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_ASSISTANT, getString(R.string.SpNetGramAssistant)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_SPG_ID, getString(R.string.SpNetGramId)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_PREMIUM, getString(R.string.SpNetGramPremiumPlan)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_SP_COIN, getString(R.string.SpNetGramCoinAirdrop)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_GEMS, getString(R.string.SpNetGramGems)));
        items.add(new ItemInner(VIEW_TYPE_INFO, 6, getString(R.string.SpNetGramFeaturesInfo)));

        items.add(new ItemInner(VIEW_TYPE_HEADER, 7, getString(R.string.SpNetGramPrivacySection)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_GHOST, getString(R.string.SpNetGramGhostMode), getString(R.string.SpNetGramGhostModeInfo)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_ANTI_REVOKE, getString(R.string.SpNetGramAntiRevoke), getString(R.string.SpNetGramAntiRevokeInfo)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_HIDE_TYPING, getString(R.string.SpNetGramHideTyping), getString(R.string.SpNetGramHideTypingInfo)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ID_NO_READ, getString(R.string.SpNetGramNoReadReceipts), getString(R.string.SpNetGramNoReadReceiptsInfo)));
        items.add(new ItemInner(VIEW_TYPE_INFO, 15, getString(R.string.SpNetGramPrivacyInfo)));

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
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_HEADER) {
                view = new HeaderCell(getContext());
            } else if (viewType == VIEW_TYPE_TEXT) {
                view = new TextSettingsCell(getContext());
            } else if (viewType == VIEW_TYPE_CHECK) {
                view = new TextCheckCell(getContext());
            } else {
                view = new TextInfoPrivacyCell(getContext());
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
                cell.setText(item.text, divider);
            } else if (holder.getItemViewType() == VIEW_TYPE_CHECK) {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                String value = item.value != null ? item.value.toString() : "";
                cell.setTextAndValueAndCheck(item.text.toString(), value, isEnabled(item.id), true, divider);
            } else {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                cell.setText(item.text);
                cell.setFixedSize(0);
            }
        }
    }
}
