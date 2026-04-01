package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.HeaderCell;
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

    private static final int COLOR_BG = 0xFF151515;
    private static final int COLOR_CELL = 0xFF1D1D1D;
    private static final int COLOR_TEXT = 0xFFEDEDED;
    private static final int COLOR_MUTED = 0xFF9AA0A6;
    private static final int COLOR_HEADER = 0xFF4FB1FF;
    private static final int COLOR_ICON = 0xFF9AA0A6;
    private static final int COLOR_DIVIDER = 0xFF242424;
    private static final int COLOR_SELECTOR = 0x224FB1FF;
    private static final int COLOR_DANGER = 0xFFE85A5A;

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_INFO = 3;
    private static final int VIEW_TYPE_DETAIL = 4;

    private static final int ID_GENERAL = 1;
    private static final int ID_APPEARANCE = 2;
    private static final int ID_CHATS = 3;
    private static final int ID_PRIVACY = 4;
    private static final int ID_WALLET = 5;
    private static final int ID_CAMERA = 6;
    private static final int ID_EXPERIMENTAL = 7;
    private static final int ID_UPDATES = 8;
    private static final int ID_OFFICIAL_CHANNEL = 10;
    private static final int ID_OFFICIAL_GROUP = 11;
    private static final int ID_SOURCE_CODE = 12;
    private static final int ID_TRANSLATE = 13;
    private static final int ID_DONATE = 14;
    private static final int ID_COPY_REPORT = 15;

    private final ArrayList<ItemInner> oldItems = new ArrayList<>();
    private final ArrayList<ItemInner> items = new ArrayList<>();

    private final Theme.ResourcesProvider spnetResources = key -> {
        if (key == Theme.key_windowBackgroundGray) {
            return COLOR_BG;
        } else if (key == Theme.key_windowBackgroundWhite) {
            return COLOR_CELL;
        } else if (key == Theme.key_windowBackgroundWhiteBlackText) {
            return COLOR_TEXT;
        } else if (key == Theme.key_windowBackgroundWhiteGrayText || key == Theme.key_windowBackgroundWhiteGrayText2) {
            return COLOR_MUTED;
        } else if (key == Theme.key_windowBackgroundWhiteBlueText) {
            return COLOR_HEADER;
        } else if (key == Theme.key_windowBackgroundWhiteValueText) {
            return COLOR_HEADER;
        } else if (key == Theme.key_windowBackgroundWhiteBlueHeader) {
            return COLOR_HEADER;
        } else if (key == Theme.key_windowBackgroundWhiteGrayIcon) {
            return COLOR_ICON;
        } else if (key == Theme.key_windowBackgroundWhiteLinkText) {
            return COLOR_HEADER;
        } else if (key == Theme.key_listSelector || key == Theme.key_settings_listSelector) {
            return COLOR_SELECTOR;
        } else if (key == Theme.key_divider) {
            return COLOR_DIVIDER;
        } else if (key == Theme.key_text_RedBold) {
            return COLOR_DANGER;
        } else if (key == Theme.key_switchTrack || key == Theme.key_switchTrackChecked ||
                key == Theme.key_switch2Track || key == Theme.key_switch2TrackChecked) {
            return COLOR_HEADER;
        } else if (key == Theme.key_switchTrackBlue || key == Theme.key_switchTrackBlueChecked ||
                key == Theme.key_switchTrackBlueThumb || key == Theme.key_switchTrackBlueThumbChecked) {
            return COLOR_HEADER;
        }
        return Theme.getColor(key);
    };

    @Override
    public boolean onFragmentCreate() {
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        if (actionBar != null) {
            actionBar.setBackgroundColor(COLOR_BG);
            actionBar.setTitleColor(COLOR_TEXT);
            actionBar.setItemsColor(COLOR_TEXT, false);
            actionBar.setItemsBackgroundColor(0x22FFFFFF, false);
        }
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
        frameLayout.setBackgroundColor(COLOR_BG);

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
        listView.setBackgroundColor(COLOR_BG);
        listView.setClipToPadding(false);
        listView.setPadding(0, 0, 0, AndroidUtilities.dp(8));
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
            if (item.viewType == VIEW_TYPE_TEXT || item.viewType == VIEW_TYPE_DETAIL) {
                switch (item.id) {
                    case ID_GENERAL:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_GENERAL));
                        break;
                    case ID_APPEARANCE:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_APPEARANCE));
                        break;
                    case ID_CHATS:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_CHATS));
                        break;
                    case ID_PRIVACY:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_PRIVACY));
                        break;
                    case ID_WALLET:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_WALLET));
                        break;
                    case ID_CAMERA:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_CAMERA));
                        break;
                    case ID_EXPERIMENTAL:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_EXPERIMENTAL));
                        break;
                    case ID_UPDATES:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_UPDATES));
                        break;
                    case ID_OFFICIAL_CHANNEL:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_OFFICIAL_CHANNEL));
                        break;
                    case ID_OFFICIAL_GROUP:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_OFFICIAL_GROUP));
                        break;
                    case ID_SOURCE_CODE:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_SOURCE_CODE));
                        break;
                    case ID_TRANSLATE:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_TRANSLATE));
                        break;
                    case ID_DONATE:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_DONATE));
                        break;
                    case ID_COPY_REPORT:
                        presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_COPY_REPORT));
                        break;
                }
                return;
            }
        });

        updateItems(false);

        return fragmentView;
    }

    private void updateItems(boolean animated) {
        oldItems.clear();
        oldItems.addAll(items);
        items.clear();

        items.add(new ItemInner(VIEW_TYPE_HEADER, 0, LocaleController.getString(R.string.SpNetGramSectionTitle)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_GENERAL, LocaleController.getString(R.string.SpNetGramGeneral)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_APPEARANCE, LocaleController.getString(R.string.SpNetGramAppearance)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_CHATS, LocaleController.getString(R.string.SpNetGramChats)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_PRIVACY, LocaleController.getString(R.string.SpNetGramPrivacyPower)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_WALLET, LocaleController.getString(R.string.SpNetGramWallet)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_CAMERA, LocaleController.getString(R.string.SpNetGramCameraMedia)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_EXPERIMENTAL, LocaleController.getString(R.string.SpNetGramExperimental)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_UPDATES, LocaleController.getString(R.string.SpNetGramUpdates)));

        items.add(new ItemInner(VIEW_TYPE_HEADER, 6, LocaleController.getString(R.string.SpNetGramInfoSection)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_OFFICIAL_CHANNEL, LocaleController.getString(R.string.SpNetGramOfficialChannel), LocaleController.getString(R.string.SpNetGramOfficialChannelValue)));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ID_OFFICIAL_GROUP, LocaleController.getString(R.string.SpNetGramOfficialGroup), LocaleController.getString(R.string.SpNetGramOfficialGroupValue)));
        items.add(new ItemInner(VIEW_TYPE_DETAIL, ID_SOURCE_CODE, LocaleController.getString(R.string.SpNetGramSourceCode), LocaleController.getString(R.string.SpNetGramSourceCodeInfo)));
        items.add(new ItemInner(VIEW_TYPE_DETAIL, ID_TRANSLATE, LocaleController.getString(R.string.SpNetGramTranslate), LocaleController.getString(R.string.SpNetGramTranslateInfo)));
        items.add(new ItemInner(VIEW_TYPE_DETAIL, ID_DONATE, LocaleController.getString(R.string.SpNetGramDonate), LocaleController.getString(R.string.SpNetGramDonateInfo)));
        items.add(new ItemInner(VIEW_TYPE_DETAIL, ID_COPY_REPORT, LocaleController.getString(R.string.SpNetGramCopyReport), LocaleController.getString(R.string.SpNetGramCopyReportInfo)));

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
            return type == VIEW_TYPE_TEXT || type == VIEW_TYPE_DETAIL;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_HEADER) {
                view = new HeaderCell(getContext(), spnetResources);
            } else if (viewType == VIEW_TYPE_TEXT) {
                view = new TextSettingsCell(getContext(), spnetResources);
            } else if (viewType == VIEW_TYPE_DETAIL) {
                view = new DetailCell(getContext());
            } else {
                view = new TextInfoPrivacyCell(getContext(), spnetResources);
            }
            if (viewType == VIEW_TYPE_TEXT || viewType == VIEW_TYPE_DETAIL) {
                view.setBackground(Theme.getSelectorDrawable(true, spnetResources));
            } else {
                view.setBackgroundColor(COLOR_BG);
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
                cell.setIcon(getIconForItem(item.id));
                if (item.value != null) {
                    cell.setTextAndValue(item.text, item.value, divider);
                    cell.setTextValueColor(COLOR_HEADER);
                } else {
                    cell.setText(item.text, divider);
                }
            } else if (holder.getItemViewType() == VIEW_TYPE_DETAIL) {
                DetailCell cell = (DetailCell) holder.itemView;
                cell.setTextAndValueAndIcon(item.text, item.value, getIconForItem(item.id), divider);
            } else {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                cell.setText(item.text);
                cell.setFixedSize(0);
            }
        }
    }

    private int getIconForItem(int id) {
        switch (id) {
            case ID_GENERAL:
                return R.drawable.settings_account;
            case ID_APPEARANCE:
                return R.drawable.menu_night_mode_24;
            case ID_CHATS:
                return R.drawable.settings_chat;
            case ID_PRIVACY:
                return R.drawable.settings_privacy;
            case ID_WALLET:
                return R.drawable.settings_wallet;
            case ID_CAMERA:
                return R.drawable.video_settings;
            case ID_EXPERIMENTAL:
                return R.drawable.settings_features;
            case ID_UPDATES:
                return R.drawable.menu_download_off_24;
            case ID_OFFICIAL_CHANNEL:
                return R.drawable.settings_channel;
            case ID_OFFICIAL_GROUP:
                return R.drawable.settings_group;
            case ID_SOURCE_CODE:
                return R.drawable.menu_feature_code;
            case ID_TRANSLATE:
                return R.drawable.settings_language;
            case ID_DONATE:
                return R.drawable.settings_gift;
            case ID_COPY_REPORT:
                return R.drawable.settings_logs;
            default:
                return 0;
        }
    }

    private static class DetailCell extends FrameLayout {
        private final ImageView iconView;
        private final TextView titleView;
        private final TextView subtitleView;
        private final Paint dividerPaint = new Paint();
        private boolean needDivider;

        public DetailCell(Context context) {
            super(context);
            setWillNotDraw(false);

            iconView = new ImageView(context);
            iconView.setScaleType(ImageView.ScaleType.CENTER);
            addView(iconView, LayoutHelper.createFrame(52, 52, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 8, 6, 8, 0));

            titleView = new TextView(context);
            titleView.setTextColor(COLOR_TEXT);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            titleView.setLines(1);
            titleView.setMaxLines(1);
            titleView.setSingleLine(true);
            titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            titleView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
            addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 64, 10, 21, 0));

            subtitleView = new TextView(context);
            subtitleView.setTextColor(COLOR_MUTED);
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            subtitleView.setLines(1);
            subtitleView.setMaxLines(1);
            subtitleView.setSingleLine(true);
            subtitleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            subtitleView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            addView(subtitleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 64, 35, 21, 0));

            dividerPaint.setColor(COLOR_DIVIDER);
        }

        public void setTextAndValueAndIcon(CharSequence text, CharSequence value, int resId, boolean divider) {
            titleView.setText(text);
            subtitleView.setText(value);
            iconView.setImageResource(resId);
            iconView.setColorFilter(COLOR_ICON);
            needDivider = divider;
            setWillNotDraw(!divider);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(64) + (needDivider ? 1 : 0));
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (needDivider) {
                canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(20), getMeasuredHeight() - 1,
                        getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(20) : 0), getMeasuredHeight() - 1, dividerPaint);
            }
        }
    }
}
