/*
 * Copyright (C) 2014 nohana, Inc.
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.entity;

import com.isseiaoki.simplecropview.CropImageView;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.R;
import com.zhihu.matisse.engine.ImageEngine;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.filter.Filter;
import com.zhihu.matisse.listener.OnCheckedListener;
import com.zhihu.matisse.listener.OnSelectedListener;

import android.content.pm.ActivityInfo;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.StyleRes;

public final class SelectionSpec {

    public Set<MimeType> mimeTypeSet;

    public boolean mediaTypeExclusive;

    public boolean showSingleMediaType;

    public boolean singleMediaPreview;

    public boolean singleImageCrop;

    public boolean openCameraNow;

    @StyleRes
    public int themeId = R.style.Matisse_Tongzhuo;

    public int orientation;

    public boolean countable;

    public int maxSelectable;

    public int maxImageSelectable;

    public int maxVideoSelectable;

    public List<Filter> filters;

    public boolean capture;

    public boolean captureFront;

    public CaptureStrategy captureStrategy;

    public int spanCount;

    public int gridExpectedSize;

    public float thumbnailScale;

    public ImageEngine imageEngine;

    public boolean hasInited;

    public CropImageView.CropMode cropMode;

    public int minFrameSizeInDp;

    public float initialFrameScale;

    public OnSelectedListener onSelectedListener;

    public OnCheckedListener onCheckedListener;

    public Locale locale;

    private SelectionSpec() {
    }

    public static SelectionSpec getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public static SelectionSpec getCleanInstance() {
        SelectionSpec selectionSpec = getInstance();
        selectionSpec.reset();
        return selectionSpec;
    }

    private void reset() {
        mimeTypeSet = null;
        mediaTypeExclusive = true;
        showSingleMediaType = false;
        themeId = R.style.Matisse_Tongzhuo;
        orientation = 0;
        countable = false;
        maxSelectable = 1;
        maxImageSelectable = 0;
        maxVideoSelectable = 0;
        filters = null;
        capture = false;
        captureStrategy = null;
        captureFront = false;
        spanCount = 3;
        gridExpectedSize = 0;
        thumbnailScale = 0.5f;
        singleMediaPreview = false;
        singleImageCrop = false;
        openCameraNow = false;
        imageEngine = new GlideEngine();
        hasInited = true;
        locale = null;
        cropMode = CropImageView.CropMode.SQUARE;
        minFrameSizeInDp = 240;
        initialFrameScale = 0.75f;
    }

    public boolean singleSelectionModeEnabled() {
        return !countable && (maxSelectable == 1 || (maxImageSelectable == 1
                && maxVideoSelectable == 1));
    }

    public boolean needOrientationRestriction() {
        return orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    }

    public boolean onlyShowImages() {
        return showSingleMediaType && MimeType.ofImage().containsAll(mimeTypeSet);
    }

    public boolean onlyShowVideos() {
        return showSingleMediaType && MimeType.ofVideo().containsAll(mimeTypeSet);
    }

    public boolean onlyShowGif() {
        return showSingleMediaType && MimeType.ofGif().equals(mimeTypeSet);
    }

    public boolean singleMediaClosePreview() {
        return maxSelectable == 1 && !singleMediaPreview;
    }

    public boolean singleMediaOpenPreview() {
        return maxSelectable == 1 && singleMediaPreview;
    }

    public boolean singleImageCropEnable() {
        return maxSelectable == 1 && singleImageCrop && MimeType.ofStaticImage()
                .containsAll(mimeTypeSet);
    }

    private static final class InstanceHolder {

        private static final SelectionSpec INSTANCE = new SelectionSpec();
    }
}
