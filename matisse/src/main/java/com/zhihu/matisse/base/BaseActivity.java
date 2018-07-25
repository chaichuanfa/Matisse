package com.zhihu.matisse.base;

import com.zhihu.matisse.internal.entity.SelectionSpec;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;

import java.util.Locale;

/**
 * Created by chaichuanfa on 2018/7/25.
 *
 * 支持7.0+动态切换语言
 */

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateResources(newBase));
    }

    private Context updateResources(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && SelectionSpec.getInstance().locale != null) {
            Resources resources = context.getResources();
            Locale locale = SelectionSpec.getInstance().locale;
            Configuration configuration = resources.getConfiguration();
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        }
        return context;
    }
}
