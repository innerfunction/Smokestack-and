// Copyright 2017 InnerFunction Ltd.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License
package com.innerfunction.smokestack;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.CancellationSignal;
import android.util.Log;

import com.innerfunction.smokestack.content.Provider;

import java.io.FileNotFoundException;

/**
 * The Smokestack content provider.
 * Allows Smokestack CMS content to be accessed by an app or shared with other apps.
 *
 * This class must be declared within an app's manifest for content to be accessed. Add the
 * following to the manifest:
 *
 * <pre>
 * &lt;provider
 *      android:authorities="..."
 *      android:name="com.innerfunction.smokestack.ContentProvider"&gt;
 * &lt;/provider&gt;
 * </pre>
 *
 * The authority names for each Smokestack content repository to be accessed should be listed in the
 * authorities attribute; multiple values must be separated by semi-colons.
 *
 * This class delegates all requests to the instance of
 * com.innerfunction.smokestack.content.Provider instantiated by the content app container (@see
 * com.innerfunction.content.AppContainer). The Provider instance can be configured through the
 * app container or new content authority configurations can be added to the SCFFLD filesystem.
 *
 * Created by juliangoacher on 07/03/2017.
 */
public class ContentProvider extends android.content.ContentProvider {

    static final String Tag = ContentProvider.class.getSimpleName();

    /** The content provider's delegate. */
    private Provider provider;

    private Provider getProvider() {
        if( provider == null ) {
            provider = AppContainer.getAppContainer().getContentProvider();
        }
        return provider;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        Provider provider = getProvider();
        if( provider != null ) {
            return provider.openFile( uri, signal );
        }
        throw new FileNotFoundException("Content provider not found");
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        throw new UnsupportedOperationException("Op not supported");
    }

    @Override
    public String getType(Uri uri) {
        return provider.getType( uri );
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Op not supported");
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        return query( uri, projection, selection, args, order, null );
    }

        @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order, CancellationSignal signal) {
        return provider.query( uri, projection, selection, args, order, signal );
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        throw new UnsupportedOperationException("Op not supported");
    }

}
