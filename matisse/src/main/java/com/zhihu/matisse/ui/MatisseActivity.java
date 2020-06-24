/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.ui;

import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;
import com.zhihu.matisse.R;
import com.zhihu.matisse.base.BaseActivity;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.model.AlbumCollection;
import com.zhihu.matisse.internal.model.SelectedItemCollection;
import com.zhihu.matisse.internal.ui.AlbumPreviewActivity;
import com.zhihu.matisse.internal.ui.BasePreviewActivity;
import com.zhihu.matisse.internal.ui.CropActivity;
import com.zhihu.matisse.internal.ui.MediaSelectionFragment;
import com.zhihu.matisse.internal.ui.SelectedPreviewActivity;
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter;
import com.zhihu.matisse.internal.ui.adapter.AlbumsAdapter;
import com.zhihu.matisse.internal.ui.widget.AlbumsSpinner;
import com.zhihu.matisse.internal.utils.MediaStoreCompat;
import com.zhihu.matisse.internal.utils.PathUtils;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

/**
 * Main Activity to display albums and media content (images/videos) in each album
 * and also support media selecting operations.
 */
public class MatisseActivity extends BaseActivity implements
        AlbumCollection.AlbumCallbacks, AdapterView.OnItemSelectedListener,
        MediaSelectionFragment.SelectionProvider, View.OnClickListener,
        AlbumMediaAdapter.CheckStateListener, AlbumMediaAdapter.OnMediaClickListener,
        AlbumMediaAdapter.OnPhotoCapture {

    public static final String EXTRA_RESULT_SELECTION = "extra_result_selection";

    public static final String EXTRA_RESULT_SELECTION_PATH = "extra_result_selection_path";

    private static final int REQUEST_CODE_PREVIEW = 23;

    private static final int REQUEST_CODE_CAPTURE = 24;

    private static final int REQUEST_CODE_CROP = 25;

    private final AlbumCollection mAlbumCollection = new AlbumCollection();

    private MediaStoreCompat mMediaStoreCompat;

    private SelectedItemCollection mSelectedCollection = new SelectedItemCollection(this);

    private SelectionSpec mSpec;

    private AlbumsSpinner mAlbumsSpinner;

    private AlbumsAdapter mAlbumsAdapter;

    private TextView mButtonPreview;

    private TextView mButtonApply;

    private View mContainer;

    private View mEmptyView;

    private View mBottomToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // programmatically set theme before super.onCreate()
        mSpec = SelectionSpec.getInstance();
        setTheme(mSpec.themeId);
        super.onCreate(savedInstanceState);
        if (!mSpec.hasInited) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        setContentView(R.layout.activity_matisse);

        if (mSpec.needOrientationRestriction()) {
            setRequestedOrientation(mSpec.orientation);
        }

        if (mSpec.imageEngine instanceof GlideEngine) {
            Glide.get(this).setMemoryCategory(MemoryCategory.LOW);
        }

        if (mSpec.capture) {
            mMediaStoreCompat = new MediaStoreCompat(this);
            if (mSpec.captureStrategy == null) {
                throw new RuntimeException("Don't forget to set CaptureStrategy.");
            }
            mMediaStoreCompat.setCaptureStrategy(mSpec.captureStrategy);
            if (mSpec.openCameraNow) {
                mMediaStoreCompat.dispatchCaptureIntent(this, REQUEST_CODE_CAPTURE);
                return;
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        Drawable navigationIcon = toolbar.getNavigationIcon();
        navigationIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

        mButtonPreview = (TextView) findViewById(R.id.button_preview);
        mButtonApply = (TextView) findViewById(R.id.button_apply);
        mButtonPreview.setOnClickListener(this);
        mButtonApply.setOnClickListener(this);
        mContainer = findViewById(R.id.container);
        mEmptyView = findViewById(R.id.empty_view);
        mBottomToolbar = findViewById(R.id.bottom_toolbar);
        if (mSpec.maxSelectable == 1) {
            mBottomToolbar.setVisibility(View.GONE);
        } else {
            mBottomToolbar.setVisibility(View.VISIBLE);
        }

        mSelectedCollection.onCreate(savedInstanceState);
        updateBottomToolbar();

        mAlbumsAdapter = new AlbumsAdapter(this, null, false);
        mAlbumsSpinner = new AlbumsSpinner(this);
        mAlbumsSpinner.setOnItemSelectedListener(this);
        mAlbumsSpinner.setSelectedTextView((TextView) findViewById(R.id.selected_album));
        mAlbumsSpinner.setPopupAnchorView(findViewById(R.id.toolbar));
        mAlbumsSpinner.setAdapter(mAlbumsAdapter);
        mAlbumCollection.onCreate(this, this);
        mAlbumCollection.onRestoreInstanceState(savedInstanceState);
        mAlbumCollection.loadAlbums();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedCollection != null) {
            mSelectedCollection.onSaveInstanceState(outState);
        }
        if (mAlbumCollection != null) {
            mAlbumCollection.onSaveInstanceState(outState);
        }
        mSelectedCollection.onSaveInstanceState(outState);
        mAlbumCollection.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        mAlbumCollection.onDestroy();
        if (mSpec != null && mSpec.imageEngine != null) {
            mSpec.imageEngine.clearCache(getApplicationContext());
        }
        mMediaStoreCompat = null;
        mSelectedCollection.clear();
        mSelectedCollection = null;
        super.onDestroy();
        mSpec.onCheckedListener = null;
        mSpec.onSelectedListener = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            if (requestCode == REQUEST_CODE_CROP) {
                if (mSpec.openCameraNow) {
                    finish();
                } else {
                    mSelectedCollection.clear();
                }
            } else if (requestCode == REQUEST_CODE_CAPTURE) {
                if (mSpec.openCameraNow) {
                    finish();
                }
            }
            return;
        }

        if (requestCode == REQUEST_CODE_PREVIEW) {
            Bundle resultBundle = data.getBundleExtra(BasePreviewActivity.EXTRA_RESULT_BUNDLE);
            ArrayList<Item> selected = resultBundle
                    .getParcelableArrayList(SelectedItemCollection.STATE_SELECTION);
            int collectionType = resultBundle.getInt(SelectedItemCollection.STATE_COLLECTION_TYPE,
                    SelectedItemCollection.COLLECTION_UNDEFINED);
            if (data.getBooleanExtra(BasePreviewActivity.EXTRA_RESULT_APPLY, false)) {
                if (mSpec.singleImageCropEnable() && selected.size() == 1) {
                    Intent intent = CropActivity.newIntent(this, selected.get(0).getContentUri());
                    startActivityForResult(intent, REQUEST_CODE_CROP);
                } else {
                    Intent result = new Intent();
                    ArrayList<Uri> selectedUris = new ArrayList<>();
                    ArrayList<String> selectedPaths = new ArrayList<>();
                    if (selected != null) {
                        for (Item item : selected) {
                            selectedUris.add(item.getContentUri());
                            selectedPaths.add(PathUtils.getPath(this, item.getContentUri()));
                        }
                    }
                    result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris);
                    result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths);
                    setResult(RESULT_OK, result);
                    finish();
                }
            } else {
                mSelectedCollection.overwrite(selected, collectionType);
                Fragment mediaSelectionFragment = getSupportFragmentManager().findFragmentByTag(
                        MediaSelectionFragment.class.getSimpleName());
                if (mediaSelectionFragment instanceof MediaSelectionFragment) {
                    ((MediaSelectionFragment) mediaSelectionFragment).refreshMediaGrid();
                }
                updateBottomToolbar();
            }
        } else if (requestCode == REQUEST_CODE_CAPTURE) {
            File file = new File(mMediaStoreCompat.getCurrentPhotoPath());
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(file));
            sendBroadcast(mediaScanIntent);

            // Just pass the data back to previous calling Activity.
            Uri contentUri = mMediaStoreCompat.getCurrentPhotoUri();
            if (mSpec.singleImageCropEnable()) {
                Intent intent = CropActivity.newIntent(this, contentUri);
                startActivityForResult(intent, REQUEST_CODE_CROP);
            } else {
                String path = mMediaStoreCompat.getCurrentPhotoPath();
                ArrayList<Uri> selected = new ArrayList<>();
                selected.add(contentUri);
                ArrayList<String> selectedPath = new ArrayList<>();
                selectedPath.add(path);
                Intent result = new Intent();
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selected);
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPath);
                setResult(RESULT_OK, result);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    MatisseActivity.this.revokeUriPermission(contentUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                finish();
            }
        } else if (requestCode == REQUEST_CODE_CROP) {
            Uri uri = data.getParcelableExtra(CropActivity.CROP_IMAGE_URI);
            String path = data.getStringExtra(CropActivity.CROP_IMAGE_PATH);
            ArrayList<Uri> selected = new ArrayList<>();
            selected.add(uri);
            ArrayList<String> selectedPath = new ArrayList<>();
            selectedPath.add(path);
            Intent result = new Intent();
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selected);
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPath);
            setResult(RESULT_OK, result);
            finish();
        }
    }

    private void updateBottomToolbar() {

        int selectedCount = mSelectedCollection.count();
        if (selectedCount == 0) {
            mButtonPreview.setEnabled(false);
            mButtonApply.setEnabled(false);
            mButtonApply.setText(getString(R.string.button_apply_default));
        } else if (selectedCount == 1 && mSpec.singleSelectionModeEnabled()) {
            mButtonPreview.setEnabled(true);
            mButtonApply.setText(R.string.button_apply_default);
            mButtonApply.setEnabled(true);
        } else {
            mButtonPreview.setEnabled(true);
            mButtonApply.setEnabled(true);
            mButtonApply.setText(getString(R.string.button_apply, selectedCount));
        }

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_preview) {
            Intent intent = new Intent(this, SelectedPreviewActivity.class);
            intent.putExtra(BasePreviewActivity.EXTRA_DEFAULT_BUNDLE,
                    mSelectedCollection.getDataWithBundle());
            intent.putExtra(BasePreviewActivity.EXTRA_DEFAULT_BUNDLE,
                    mSelectedCollection.getDataWithBundle());
            startActivityForResult(intent, REQUEST_CODE_PREVIEW);
        } else if (v.getId() == R.id.button_apply) {
            applyImpl();
        }
    }

    private void applyImpl() {
        if (mSpec.singleImageCropEnable() && mSelectedCollection.count() > 0) {
            //crop image
            Intent intent = CropActivity.newIntent(this, mSelectedCollection.asListOfUri().get(0));
            startActivityForResult(intent, REQUEST_CODE_CROP);
        } else {
            Intent result = new Intent();
            ArrayList<Uri> selectedUris = (ArrayList<Uri>) mSelectedCollection.asListOfUri();
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris);
            ArrayList<String> selectedPaths = (ArrayList<String>) mSelectedCollection
                    .asListOfString();
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths);
            setResult(RESULT_OK, result);
            finish();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mAlbumCollection.setStateCurrentSelection(position);
        mAlbumsAdapter.getCursor().moveToPosition(position);
        Album album = Album.valueOf(mAlbumsAdapter.getCursor());
        if (album.isAll() && SelectionSpec.getInstance().capture) {
            album.addCaptureCount();
        }
        onAlbumSelected(album);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onAlbumLoad(final Cursor cursor) {
        mAlbumsAdapter.swapCursor(cursor);
        // select default album.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                if (cursor.isClosed()) {
                    finish();
                } else {
                    cursor.moveToPosition(mAlbumCollection.getCurrentSelection());
                    mAlbumsSpinner.setSelection(MatisseActivity.this,
                            mAlbumCollection.getCurrentSelection());
                    Album album = Album.valueOf(cursor);
                    if (album.isAll() && SelectionSpec.getInstance().capture) {
                        album.addCaptureCount();
                    }
                    onAlbumSelected(album);
                }
            }
        });
    }

    @Override
    public void onAlbumReset() {
        mAlbumsAdapter.swapCursor(null);
    }

    private void onAlbumSelected(Album album) {
        if (album.isAll() && album.isEmpty()) {
            mContainer.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mContainer.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            Fragment fragment = MediaSelectionFragment.newInstance(album);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment, MediaSelectionFragment.class.getSimpleName())
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void onUpdate() {
        // notify bottom toolbar that check state changed.
        updateBottomToolbar();

        if (mSpec.onSelectedListener != null) {
            mSpec.onSelectedListener.onSelected(
                    mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString());
        }
    }

    @Override
    public void onMediaClick(Album album, Item item, int adapterPosition) {
        if (mSpec.singleMediaClosePreview()) {
            mSelectedCollection.clear();
            mSelectedCollection.add(item);
            applyImpl();
        } else {
            Intent intent = new Intent(this, AlbumPreviewActivity.class);
            intent.putExtra(AlbumPreviewActivity.EXTRA_ALBUM, album);
            intent.putExtra(AlbumPreviewActivity.EXTRA_ITEM, item);
            intent.putExtra(BasePreviewActivity.EXTRA_DEFAULT_BUNDLE,
                    mSelectedCollection.getDataWithBundle());
            startActivityForResult(intent, REQUEST_CODE_PREVIEW);
        }
    }

    @Override
    public SelectedItemCollection provideSelectedItemCollection() {
        return mSelectedCollection;
    }

    @Override
    public void capture() {
        if (mMediaStoreCompat != null) {
            mMediaStoreCompat.dispatchCaptureIntent(this, REQUEST_CODE_CAPTURE);
        }
    }

}
