package org.yapp.core.ui.abase;

import android.content.Context;
import android.view.View;

/**
 * Description: Fragment主持层基础接口. <br>
 * Date: 2016/3/31 11:34 <br>
 * Author: ysj
 */
public interface BaseFragmentPresenterApi<F> {
    void onBuild(Context context,F fragment);

    F getFragment();

    View getContextView();
}
