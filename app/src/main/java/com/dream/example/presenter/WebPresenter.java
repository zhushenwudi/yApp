package com.dream.example.presenter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.dream.example.R;
import com.dream.example.data.support.AppConsts;
import com.dream.example.data.support.HttpFactory;
import com.dream.example.presenter.base.AppSwipeRefreshActivityPresenter;
import com.dream.example.utils.IntentUtil;
import com.dream.example.utils.JsonUtil;
import com.dream.example.utils.SynUtils;
import com.dream.example.view.IWebView;
import com.linroid.filtermenu.library.FilterMenu;
import com.linroid.filtermenu.library.FilterMenuLayout;

import org.yapp.core.ui.inject.annotation.ViewInject;
import org.yapp.utils.Log;
import org.yapp.utils.SignalUtil;
import org.yapp.utils.Toast;
import org.yapp.utils.share.ShareApiEnum;
import org.yapp.utils.share.ShareTypeEnum;
import org.yapp.utils.share.ShareUtil;

import java.io.IOException;
import java.util.Map;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Description: WebPresenter. <br>
 * Date: 2016/08/17 16:59 <br>
 * Author: ysj
 */
public class WebPresenter extends AppSwipeRefreshActivityPresenter implements IWebView {
    private static final String EXTRA_URL = "URL";
    private static final String EXTRA_TITLE = "TITLE";

    @ViewInject(R.id.wb_content)
    private WebView mWebContent;

    @ViewInject(R.id.web_filter_menu)
    private FilterMenuLayout mMenuLayout;

    @Override
    public void onInit() {
        String url = getContext().getIntent().getStringExtra(EXTRA_URL);
        String title = getContext().getIntent().getStringExtra(EXTRA_TITLE);

        if (!TextUtils.isEmpty(title)) {
            setTitle(title);
        } else {
            setTitle(getContext().getString(R.string.app_name));
        }
        attachMenu(mMenuLayout);
        JavaScriptUtil javaScriptUtil = new JavaScriptUtil(getContext());
        WebSettings settings = mWebContent.getSettings();
        settings.setLoadWithOverviewMode(true);
        settings.setAppCacheEnabled(true);
//        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptEnabled(true);// 可用JS
//        settings.setAllowFileAccess(true); // 允许访问文件
//        settings.setSupportZoom(true); // 支持缩放
//        settings.setPluginState(WebSettings.PluginState.ON);// 设置webview支持插件
//        settings.setBuiltInZoomControls(true); // 设置显示缩放按钮
//        settings.setDisplayZoomControls(false);
        mWebContent.setScrollBarStyle(0);// 滚动条风格，为0就是不给滚动条留空间，滚动条覆盖在网页上
        mWebContent.setWebViewClient(new DefaultClient());
        mWebContent.addJavascriptInterface(javaScriptUtil,
                JavaScriptUtil.INTERFACE_NAME);
        getContext().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        loadUrl(url);
    }

    @Override
    public void onPause() {
        if (mWebContent != null) mWebContent.onPause();
    }

    @Override
    public void onDestroy() {
        if (mWebContent != null) mWebContent.destroy();
        super.onDestroy();
    }

    @Override
    public void onClear() {
        mWebContent = null;
        mMenuLayout = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_copy_url:
                String copyDone = getContext().getString(R.string.toast_copy_done);
                SynUtils.copyToClipBoard(getContext(), mWebContent.getUrl(), copyDone);
                return true;
            case R.id.action_open_url:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri uri = Uri.parse(mWebContent.getUrl());
                intent.setData(uri);
                if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                    getContext().startActivity(intent);
                } else {
                    Toast.showMessageForButtomShort(getContext().getString(R.string.toast_open_fail));
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public int getMenuRes() {
        return R.menu.menu_web;
    }

    @Override
    public void showMsg(String msg) {
        Snackbar.make(mWebContent, msg, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void showEmpty() {
        getContext().getPresenter().showMsg(getContext().getString(R.string.data_null));
    }

    @Override
    public void onRefreshStarted() {
        mWebContent.reload();
    }

    @Override
    public void loadUrl(String url) {
        if (SignalUtil.isNetworkConnected()) { // 检查网络是否可用
            app.synCookies(url);
            mWebContent.loadUrl(url);
        } else {
            showMsg("当前网络状态不佳");
        }
    }

    /**
     * 附上菜单
     *
     * @param layout
     * @return
     */
    private FilterMenu attachMenu(FilterMenuLayout layout) {
        return new FilterMenu.Builder(getContext())
                .addItem(android.R.drawable.ic_menu_rotate)
                .addItem(android.R.drawable.ic_menu_revert)
                .addItem(android.R.drawable.ic_menu_share)
                .attach(layout)
                .withListener(new FilterMenu.OnMenuChangeListener() {
                    @Override
                    public void onMenuItemClick(View view, int position) {
                        switch (position) {
                            case 0:
                                onRefreshStarted();
                                break;
                            case 1:
                                if (mWebContent.canGoBack()) {
                                    mWebContent.goBack();
                                } else {
                                    // do nothing
                                }
                                break;
                            case 2:
                                String url = getContext().getIntent().getStringExtra(EXTRA_URL);
                                String title = getContext().getIntent().getStringExtra(EXTRA_TITLE);
                                ShareUtil.with(getContext())
                                        .type(ShareTypeEnum.TEXT)
                                        .api(ShareApiEnum.QQ)
                                        .setTitle(getContext().getString(R.string.app_name))
                                        .setUrl(url)
                                        .setText(title)
                                        .setDescription(title)
                                        .send();
                                break;
                        }
                    }

                    @Override
                    public void onMenuCollapse() {

                    }

                    @Override
                    public void onMenuExpand() {

                    }
                })
                .build();
    }

    private class DefaultClient extends WebViewClient {

        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (TextUtils.isEmpty(url)) {
                return true;
            }
            if (Uri.parse(url).getHost().equals("github.com")) {
                return false;
            }
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            showLoading();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            hideRefresh();
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            hideRefresh();
            showMsg(description);
        }
    }

    public class JavaScriptUtil {
        public static final String INTERFACE_NAME = "ScriptUtil";

        private Context mContext;

        public JavaScriptUtil(Context context) {
            this.mContext = context;
        }

        /**
         * popupMsg:(弹出消息框). <br>
         *
         * @param data
         * @author ysj
         * @since JDK 1.7 date: 2015-10-19 下午6:33:09 <br>
         */
        @JavascriptInterface
        public void popupMsg(String data) {
            if (data.indexOf("<div") == -1) {
                Map<String, Object> result = JsonUtil.jsonToMap(filterTag(data));
                if (!"0".equals(result.get(AppConsts._STATUS))) {
                    showDialog(getContext().getString(R.string.data_error), (String) result.get(AppConsts._ERROR_MSG));
                }
            }
        }

        /**
         * popupConfrim:(弹出确认框). <br>
         *
         * @param json
         * @author ysj
         * @since JDK 1.7 date: 2015-9-23 下午3:08:36 <br>
         */
        @JavascriptInterface
        public void popupConfrim(String json) {
            Log.d(json);
            if (!TextUtils.isEmpty(json)) {
                Map<String, Object> result = JsonUtil.jsonToMap(json);
                if (result.containsKey(AppConsts._STATUS)
                        && "0".equals(result.get(AppConsts._STATUS))) {
                    // TODO
                } else {
                    showDialog(getContext().getString(R.string.app_name)
                            , (String) result.get(AppConsts._ERROR_MSG));
                }
            }
        }

        /**
         * showMsg:(底部提示消息). <br>
         *
         * @param message
         * @author ysj
         * @since JDK 1.7 date: 2015-9-11 上午10:34:41 <br>
         */
        @JavascriptInterface
        public void showMsg(String message) {
            Toast.showMessageForButtomShort(message);
        }

        /**
         * getResulet:(获取结果). <br>
         *
         * @param url
         * @return
         * @author ysj
         * @since JDK 1.7 date: 2015-9-25 下午4:19:09 <br>
         */
        @JavascriptInterface
        public String getResulet(String url) {
            try {
                Request request = new Request.Builder()
                        .url(AppConsts.ServerConfig.MAIN_HOST_PRIMARY + url)
                        .build();
                Response response = HttpFactory.mClient.newCall(request).execute();
                return response.body().string();
            } catch (IOException e) {
                Log.e(e.getMessage(), e);
            }
            return null;
        }

        /**
         * openBrowser:(通过外部浏览器打开链接). <br>
         *
         * @param url
         * @author ysj
         * @since JDK 1.7 date: 2015-9-19 下午4:20:18 <br>
         */
        @JavascriptInterface
        public void openBrowser(String url) {
            openUrl(url);
        }

        /**
         * openUrl:(打开新的活动窗口加载链接). <br>
         *
         * @param url
         * @author ysj
         * @since JDK 1.7
         * date: 2015-12-11 下午5:46:12 <br>
         */
        @JavascriptInterface
        public void openUrl(String url) {
            if (url.indexOf("http") == -1) {
                url = AppConsts.ServerConfig.MAIN_HOST_PRIMARY + url;
            }
            IntentUtil.gotoWebActivity(getContext(), url, getContext().getString(R.string.title_activity_web));
        }

        /**
         * filterTag:(过滤掉页面标签等字符). <br>
         *
         * @param str
         * @return
         * @author ysj
         * @since JDK 1.7
         * date: 2016-1-6 下午5:21:47 <br>
         */
        private String filterTag(String str) {
            String regexstr = "<[^>]*>";
            String json = str.replaceAll(regexstr, "");
            return json;
        }
    }
}
