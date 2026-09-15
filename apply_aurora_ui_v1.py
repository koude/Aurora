from pathlib import Path
import re

root = Path.cwd()


def write(rel, text):
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text.strip() + "\n", encoding="utf-8")
    print("write", rel)


def replace_in_file(rel, replacements):
    p = root / rel
    text = p.read_text(encoding="utf-8")
    old_text = text
    for pattern, repl in replacements:
        text, count = re.subn(pattern, repl, text, flags=re.MULTILINE)
        if count == 0:
            raise SystemExit(f"Anchor not found in {rel}: {pattern}")
    if text != old_text:
        p.write_text(text, encoding="utf-8")
        print("update", rel)

# Material 3 Expressive-ish geometry while retaining CMFA's stable Views architecture.
replace_in_file("design/src/main/res/values/dimens.xml", [
    (r'<dimen name="toolbar_height">[^<]+</dimen>', '<dimen name="toolbar_height">72dp</dimen>'),
    (r'<dimen name="toolbar_elevation">[^<]+</dimen>', '<dimen name="toolbar_elevation">0dp</dimen>'),
    (r'<dimen name="large_item_padding_vertical">[^<]+</dimen>', '<dimen name="large_item_padding_vertical">20dp</dimen>'),
    (r'<dimen name="large_item_header_component_size">[^<]+</dimen>', '<dimen name="large_item_header_component_size">36dp</dimen>'),
    (r'<dimen name="large_action_card_radius">[^<]+</dimen>', '<dimen name="large_action_card_radius">28dp</dimen>'),
    (r'<dimen name="large_action_card_elevation">[^<]+</dimen>', '<dimen name="large_action_card_elevation">0dp</dimen>'),
    (r'<dimen name="large_action_card_min_height">[^<]+</dimen>', '<dimen name="large_action_card_min_height">104dp</dimen>'),
    (r'<dimen name="main_card_margin_vertical">[^<]+</dimen>', '<dimen name="main_card_margin_vertical">7dp</dimen>'),
    (r'<dimen name="main_label_margin_vertical">[^<]+</dimen>', '<dimen name="main_label_margin_vertical">4dp</dimen>'),
    (r'<dimen name="main_padding_horizontal">[^<]+</dimen>', '<dimen name="main_padding_horizontal">20dp</dimen>'),
    (r'<dimen name="main_top_banner_height">[^<]+</dimen>', '<dimen name="main_top_banner_height">112dp</dimen>'),
])

write("design/src/main/res/layout/common_activity_bar.xml", r'''<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="center_vertical"
    android:minHeight="@dimen/toolbar_height"
    android:orientation="horizontal">

    <ImageView
        android:id="@+id/activity_bar_close_view"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_marginStart="12dp"
        android:layout_marginEnd="12dp"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:clickable="true"
        android:contentDescription="@string/close"
        android:focusable="true"
        android:padding="12dp"
        android:src="@drawable/ic_baseline_arrow_back" />

    <TextView
        android:id="@+id/activity_bar_title_view"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:ellipsize="end"
        android:maxLines="1"
        android:textAppearance="@style/TextAppearance.MaterialComponents.Headline5"
        android:textStyle="bold" />
</LinearLayout>''')

write("design/src/main/res/layout/design_main.xml", r'''<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <data>
        <variable name="self" type="com.github.kr328.clash.design.MainDesign" />
        <variable name="clashRunning" type="boolean" />
        <variable name="forwarded" type="String" />
        <variable name="mode" type="String" />
        <variable name="profileName" type="String" />
        <variable name="colorClashStarted" type="int" />
        <variable name="colorClashStopped" type="int" />
        <variable name="hasProviders" type="boolean" />
        <import type="android.view.View" />
        <import type="com.github.kr328.clash.design.MainDesign.Request" />
    </data>

    <androidx.coordinatorlayout.widget.CoordinatorLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:paddingStart="@{self.surface.insets.start}"
        android:paddingEnd="@{self.surface.insets.end}">

        <ScrollView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:fillViewport="true"
            android:scrollbars="none">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:animateLayoutChanges="true"
                android:clipToPadding="false"
                android:orientation="vertical"
                android:paddingHorizontal="@dimen/main_padding_horizontal"
                android:paddingTop="@{(float) self.surface.insets.top + 16}"
                android:paddingBottom="@{(float) self.surface.insets.bottom + 24}">

                <!-- Large, confident M3-style header. -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:gravity="center_vertical"
                    android:minHeight="@dimen/main_top_banner_height"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="@string/application_name"
                        android:textAppearance="@style/TextAppearance.MaterialComponents.Headline4"
                        android:textStyle="bold" />

                    <ImageView
                        android:layout_width="52dp"
                        android:layout_height="52dp"
                        android:background="?attr/selectableItemBackgroundBorderless"
                        android:clickable="true"
                        android:contentDescription="@string/settings"
                        android:focusable="true"
                        android:onClick="@{() -> self.request(Request.OpenSettings)}"
                        android:padding="13dp"
                        android:src="@drawable/ic_baseline_settings" />
                </LinearLayout>

                <!-- Hero connection state. Existing CMFA actions and data bindings are preserved. -->
                <com.github.kr328.clash.design.view.LargeActionCard
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="10dp"
                    android:onClick="@{() -> self.request(Request.ToggleStatus)}"
                    android:theme="@style/AppThemeDark"
                    app:cardBackgroundColor="@{clashRunning ? colorClashStarted : colorClashStopped}"
                    app:icon="@{clashRunning ? @drawable/ic_outline_check_circle : @drawable/ic_outline_not_interested}"
                    app:subtext="@{clashRunning ? @string/format_traffic_forwarded(forwarded) : @string/tap_to_start}"
                    app:text="@{clashRunning ? @string/running : @string/stopped}" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="8dp"
                    android:layout_marginTop="16dp"
                    android:layout_marginBottom="8dp"
                    android:text="@string/aurora_section_connection"
                    android:textAppearance="@style/TextAppearance.MaterialComponents.Subtitle1"
                    android:textStyle="bold" />

                <com.github.kr328.clash.design.view.LargeActionCard
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginVertical="@dimen/main_card_margin_vertical"
                    android:onClick="@{() -> self.request(Request.OpenProxy)}"
                    android:visibility="@{clashRunning ? View.VISIBLE : View.GONE}"
                    app:icon="@drawable/ic_baseline_apps"
                    app:subtext="@{mode}"
                    app:text="@string/proxy" />

                <com.github.kr328.clash.design.view.LargeActionCard
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginVertical="@dimen/main_card_margin_vertical"
                    android:onClick="@{() -> self.request(Request.OpenProfiles)}"
                    app:icon="@drawable/ic_baseline_view_list"
                    app:subtext="@{profileName != null ? @string/format_profile_activated(profileName) : @string/not_selected}"
                    app:text="@string/profile" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="8dp"
                    android:layout_marginTop="20dp"
                    android:layout_marginBottom="4dp"
                    android:text="@string/aurora_section_tools"
                    android:textAppearance="@style/TextAppearance.MaterialComponents.Subtitle1"
                    android:textStyle="bold" />

                <com.github.kr328.clash.design.view.LargeActionLabel
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginVertical="@dimen/main_label_margin_vertical"
                    android:onClick="@{() -> self.request(Request.OpenProviders)}"
                    android:visibility="@{clashRunning &amp;&amp; hasProviders ? View.VISIBLE : View.GONE}"
                    app:icon="@drawable/ic_baseline_swap_vertical_circle"
                    app:subtext="@string/aurora_providers_summary"
                    app:text="@string/providers" />

                <com.github.kr328.clash.design.view.LargeActionLabel
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginVertical="@dimen/main_label_margin_vertical"
                    android:onClick="@{() -> self.request(Request.OpenLogs)}"
                    app:icon="@drawable/ic_baseline_assignment"
                    app:subtext="@string/aurora_logs_summary"
                    app:text="@string/logs" />

                <com.github.kr328.clash.design.view.LargeActionLabel
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginVertical="@dimen/main_label_margin_vertical"
                    android:onClick="@{() -> self.request(Request.OpenSettings)}"
                    app:icon="@drawable/ic_baseline_settings"
                    app:subtext="@string/aurora_settings_summary"
                    app:text="@string/settings" />

                <com.github.kr328.clash.design.view.LargeActionLabel
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginVertical="@dimen/main_label_margin_vertical"
                    android:onClick="@{() -> self.request(Request.OpenHelp)}"
                    app:icon="@drawable/ic_baseline_help_center"
                    app:subtext="@string/aurora_help_summary"
                    app:text="@string/help" />

                <com.github.kr328.clash.design.view.LargeActionLabel
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginVertical="@dimen/main_label_margin_vertical"
                    android:onClick="@{() -> self.request(Request.OpenAbout)}"
                    app:icon="@drawable/ic_baseline_info"
                    app:subtext="@string/aurora_about_summary"
                    app:text="@string/about" />
            </LinearLayout>
        </ScrollView>
    </androidx.coordinatorlayout.widget.CoordinatorLayout>
</layout>''')

write("design/src/main/res/layout/design_settings.xml", r'''<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <data>
        <variable name="self" type="com.github.kr328.clash.design.SettingsDesign" />
        <import type="com.github.kr328.clash.design.SettingsDesign.Request" />
    </data>

    <androidx.coordinatorlayout.widget.CoordinatorLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:paddingStart="@{self.surface.insets.start}"
        android:paddingEnd="@{self.surface.insets.end}">

        <com.github.kr328.clash.design.view.ObservableScrollView
            android:id="@+id/scroll_root"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:scrollbars="none">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:clipToPadding="false"
                android:orientation="vertical"
                android:paddingStart="20dp"
                android:paddingEnd="20dp"
                android:paddingTop="@{(float) self.surface.insets.top + @dimen/toolbar_height + 18}"
                android:paddingBottom="@{(float) self.surface.insets.bottom + 24}">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="8dp"
                    android:layout_marginBottom="10dp"
                    android:text="@string/aurora_settings_intro"
                    android:textAppearance="@style/TextAppearance.MaterialComponents.Body1" />

                <com.github.kr328.clash.design.view.LargeActionCard
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginVertical="7dp"
                    android:onClick="@{() -> self.request(Request.StartApp)}"
                    app:icon="@drawable/ic_baseline_settings"
                    app:subtext="@string/aurora_app_settings_summary"
                    app:text="@string/app" />

                <com.github.kr328.clash.design.view.LargeActionCard
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginVertical="7dp"
                    android:onClick="@{() -> self.request(Request.StartNetwork)}"
                    app:icon="@drawable/ic_baseline_dns"
                    app:subtext="@string/aurora_network_settings_summary"
                    app:text="@string/network" />

                <com.github.kr328.clash.design.view.LargeActionCard
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginVertical="7dp"
                    android:onClick="@{() -> self.request(Request.StartOverride)}"
                    app:icon="@drawable/ic_baseline_extension"
                    app:subtext="@string/aurora_override_settings_summary"
                    app:text="@string/override" />

                <com.github.kr328.clash.design.view.LargeActionCard
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginVertical="7dp"
                    android:onClick="@{() -> self.request(Request.StartMetaFeature)}"
                    app:icon="@drawable/ic_baseline_meta"
                    app:subtext="@string/aurora_meta_settings_summary"
                    app:text="@string/meta_features" />
            </LinearLayout>
        </com.github.kr328.clash.design.view.ObservableScrollView>

        <com.github.kr328.clash.design.view.ActivityBarLayout
            android:id="@+id/activity_bar_layout"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:paddingTop="@{self.surface.insets.top}">
            <include layout="@layout/common_activity_bar" />
        </com.github.kr328.clash.design.view.ActivityBarLayout>
    </androidx.coordinatorlayout.widget.CoordinatorLayout>
</layout>''')

# Add UI-only strings without touching product branding.
def add_strings(rel, block):
    p = root / rel
    text = p.read_text(encoding="utf-8")
    if 'name="aurora_section_connection"' in text:
        print("strings already present", rel)
        return
    text = text.replace("</resources>", block.strip() + "\n</resources>")
    p.write_text(text, encoding="utf-8")
    print("update", rel)

add_strings("design/src/main/res/values/strings.xml", r'''
    <string name="aurora_section_connection">Connection</string>
    <string name="aurora_section_tools">Tools &amp; diagnostics</string>
    <string name="aurora_providers_summary">Manage proxy and rule providers</string>
    <string name="aurora_logs_summary">Inspect runtime events and connection details</string>
    <string name="aurora_settings_summary">Network, app behavior and advanced options</string>
    <string name="aurora_help_summary">Usage guides and troubleshooting</string>
    <string name="aurora_about_summary">Version, licenses and project information</string>
    <string name="aurora_settings_intro">Tune the app by area. Core networking behavior remains unchanged.</string>
    <string name="aurora_app_settings_summary">App behavior, notifications and startup</string>
    <string name="aurora_network_settings_summary">VPN, DNS, routing and access control</string>
    <string name="aurora_override_settings_summary">Profile overrides and advanced configuration</string>
    <string name="aurora_meta_settings_summary">Meta kernel features and experimental options</string>
''')

zh = root / "design/src/main/res/values-zh/strings.xml"
if zh.exists():
    add_strings("design/src/main/res/values-zh/strings.xml", r'''
    <string name="aurora_section_connection">连接</string>
    <string name="aurora_section_tools">工具与诊断</string>
    <string name="aurora_providers_summary">管理代理与规则提供者</string>
    <string name="aurora_logs_summary">查看运行事件与连接详情</string>
    <string name="aurora_settings_summary">网络、应用行为与高级选项</string>
    <string name="aurora_help_summary">使用说明与故障排查</string>
    <string name="aurora_about_summary">版本、许可证与项目信息</string>
    <string name="aurora_settings_intro">按功能区域调整设置，底层网络行为保持不变。</string>
    <string name="aurora_app_settings_summary">应用行为、通知与启动方式</string>
    <string name="aurora_network_settings_summary">VPN、DNS、路由与访问控制</string>
    <string name="aurora_override_settings_summary">配置覆写与高级参数</string>
    <string name="aurora_meta_settings_summary">Meta 内核功能与实验选项</string>
''')

print("Aurora Material 3 Expressive UI patch v1 applied.")
