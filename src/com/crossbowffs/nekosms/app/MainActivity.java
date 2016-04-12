package com.crossbowffs.nekosms.app;

import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.utils.XposedUtils;

import static com.crossbowffs.nekosms.app.AboutConsts.ISSUES_URL;
import static com.crossbowffs.nekosms.app.AboutConsts.XPOSED_FORUM_URL;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static final String ACTION_OPEN_SECTION = "action_open_section";
    public static final String EXTRA_SECTION = "section";
    public static final String EXTRA_SECTION_FILTER_LIST = "filter_list";
    public static final String EXTRA_SECTION_BLOCKED_SMS_LIST = "blocked_sms_list";
    public static final String EXTRA_SECTION_SETTINGS = "settings";

    private Fragment mFragment;
    private CoordinatorLayout mCoordinatorLayout;
    private FloatingActionButton mFloatingActionButton;
    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCoordinatorLayout = (CoordinatorLayout)findViewById(R.id.main_coordinator);

        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);

        mFloatingActionButton = (FloatingActionButton)findViewById(R.id.main_fab);
        mNavigationView = (NavigationView)findViewById(R.id.main_navigation);
        mNavigationView.setNavigationItemSelectedListener(this);

        mDrawerLayout = (DrawerLayout)findViewById(R.id.main_drawer);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.open_drawer, R.string.close_drawer);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        if (!XposedUtils.isModuleEnabled()) {
            showEnableModuleDialog();
        } else if (XposedUtils.getAppVersion() != XposedUtils.getModuleVersion()) {
            showModuleOutdatedDialog();
        }

        setFragment(new FilterListFragment());
        mNavigationView.setCheckedItem(R.id.main_drawer_filter_list);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.syncState();
    }

    /*public void setTitle(int titleId) {
        mToolbar.setTitle(titleId);
    }*/

    public void setFabVisible(boolean visible) {
        if (visible) {
            mFloatingActionButton.setTranslationY(0);
            mFloatingActionButton.show();
            mFloatingActionButton.requestLayout();
        } else {
            mFloatingActionButton.hide();
        }
    }

    public void setFabCallback(View.OnClickListener listener) {
        mFloatingActionButton.setOnClickListener(listener);
    }

    public void scrollFabIn() {
        mFloatingActionButton.animate()
            .translationY(0)
            .setInterpolator(new DecelerateInterpolator(2))
            .start();
    }

    public void scrollFabOut() {
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams)mFloatingActionButton.getLayoutParams();
        mFloatingActionButton.animate()
            .translationY(mFloatingActionButton.getHeight() + lp.bottomMargin)
            .setInterpolator(new AccelerateInterpolator(2))
            .start();
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    public CoordinatorLayout getCoordinatorLayout() {
        return mCoordinatorLayout;
    }

    private BaseFragment getContentFragment() {
        if (mFragment instanceof BaseFragment) {
            return (BaseFragment)mFragment;
        } else {
            return null;
        }
    }

    private void setFragment(Fragment fragment) {
        mFragment = fragment;
        getFragmentManager()
            .beginTransaction()
            .replace(R.id.main_content, fragment)
            .commit();
    }

    public void finishTryTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
    }

    public void requestPermissions(int requestCode, String... permissions) {
        int[] status = new int[permissions.length];
        for (int i = 0; i < permissions.length; ++i) {
            String permission = permissions[i];
            int permissionStatus = ContextCompat.checkSelfPermission(this, permission);
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, requestCode);
                return;
            }
            status[i] = permissionStatus;
        }
        onRequestPermissionsResult(requestCode, permissions, status);
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        BaseFragment fragment = getContentFragment();
        if (fragment != null) {
            fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_OPEN_SECTION.equals(intent.getAction())) {
            switch (intent.getStringExtra(EXTRA_SECTION)) {
            case EXTRA_SECTION_FILTER_LIST:
                setFragment(new FilterListFragment());
                mNavigationView.setCheckedItem(R.id.main_drawer_filter_list);
                break;
            case EXTRA_SECTION_BLOCKED_SMS_LIST:
                setFragment(new BlockedSmsListFragment());
                mNavigationView.setCheckedItem(R.id.main_drawer_blocked_sms_list);
                break;
            case EXTRA_SECTION_SETTINGS:
                setFragment(new SettingsFragment());
                mNavigationView.setCheckedItem(R.id.main_drawer_settings);
                break;
            default:
                throw new IllegalArgumentException("Unknown section");
            }
            return;
        }
        BaseFragment fragment = getContentFragment();
        if (fragment != null) {
            fragment.onNewIntent(intent);
        }
    }

    private void startBrowserActivity(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void startXposedActivity(String section) {
        if (!XposedUtils.startXposedActivity(this, section)) {
            Toast.makeText(this, R.string.xposed_not_installed, Toast.LENGTH_SHORT).show();
            startBrowserActivity(XPOSED_FORUM_URL);
        }
    }

    private void showEnableModuleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(R.string.enable_xposed_module_title)
            .setMessage(R.string.enable_xposed_module_message)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(R.string.enable, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startXposedActivity(XposedUtils.XPOSED_SECTION_MODULES);
                }
            })
            .setNeutralButton(R.string.report_bug, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startBrowserActivity(ISSUES_URL);
                }
            })
            .setNegativeButton(R.string.ignore, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showModuleOutdatedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(R.string.module_outdated_title)
            .setMessage(R.string.module_outdated_message)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(R.string.reboot, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startXposedActivity(XposedUtils.XPOSED_SECTION_INSTALL);
                }
            })
            .setNegativeButton(R.string.ignore, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.main_drawer_blocked_sms_list:
            setFragment(new BlockedSmsListFragment());
            mDrawerLayout.closeDrawer(mNavigationView);
            mNavigationView.setCheckedItem(R.id.main_drawer_blocked_sms_list);
            return true;
        case R.id.main_drawer_filter_list:
            setFragment(new FilterListFragment());
            mDrawerLayout.closeDrawer(mNavigationView);
            mNavigationView.setCheckedItem(R.id.main_drawer_filter_list);
            return true;
        case R.id.main_drawer_settings:
            setFragment(new SettingsFragment());
            mDrawerLayout.closeDrawer(mNavigationView);
            mNavigationView.setCheckedItem(R.id.main_drawer_settings);
            return true;
        default:
            return false;
        }
    }
}
