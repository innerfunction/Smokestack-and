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
package com.innerfunction.smokestack.content;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;

import com.innerfunction.scffld.IOCObjectAware;
import com.innerfunction.scffld.Service;
import com.innerfunction.uri.CompoundURI;
import com.innerfunction.util.Paths;

import static com.innerfunction.util.DataLiterals.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 /**
 * An abstract content authority.
 * This class provides standard functionality needed to service requests from the different
 * content URL and resolver subsystems and the content internal URI scheme.
 * All requests are forwarded to the writeResponse(..) method, and subclasses should provide an
 * implementation of this method which resolves content data in an appropriate way for the
 * authority.
 *
 * Created by juliangoacher on 08/03/2017.
 */
public abstract class AbstractAuthority implements Authority, Service, IOCObjectAware {

    /** The app context. */
    private Context context;
    /** The name of the authority that the class instance is bound to. */
    private String authorityName;
    /** The content provider this authority is registered with. */
    private Provider contentProvider;
    /** Interval between content refreshes; in minutes. */
    private float refreshInterval;
    /**
     * A map of addressable path roots.
     * For example, given the path files/all, the path root is 'files'.
     */
    private Map<String,Object> pathRoots;

    public AbstractAuthority(Context context) {
        this.context = context;
    }

    public void setRefreshInterval(float interval) {
        this.refreshInterval = interval;
    }

    /** A path for temporarily staging downloaded content. */
    public String getStagingPath() {
        return Paths.join( contentProvider.getStagingPath(), authorityName );
    }

    /** A path for caching app content. */
    public String getAppCachePath() {
        return Paths.join( contentProvider.getAppCachePath(), authorityName );
    }

    /** A property for caching downloaded content. */
    public String getContentCachePath() {
        return Paths.join( contentProvider.getContentCachePath(), authorityName );
    }

    /** A path for CMS content that has been packaged with the app. */
    public String getPackagedContentPath() {
        return Paths.join( contentProvider.getPackagedContentPath(), authorityName );
    }

    /**
     * Refreshed content, e.g. by checking a server for downloadable updates.
     * Subclasses should provide an implementation of this class.
     */
    public abstract void refreshContent();

    public void writeResponse(AuthorityResponse response, String path, Map<String,Object> params) {
        // TODO Thought this method was abstract? (check class comment)
        ContentPath contentPath = new ContentPath( path );
        // Look-up a path root for the first path component, and if one is found then delegate the request to it.
        String root = contentPath.getRoot();
        Object pathRoot = pathRoots.get( root );
        if( pathRoot != null ) {
            // The path root only sees the rest of the path.
            contentPath = contentPath.getRest();
            // Delegate the request.
            pathRoot.writeResponse( response, this, contentPath, params );
        }
        else {
            // Path not found, respond with error.
            String error = String.format("Path not found: %s", contentPath.getFullPath() );
            response.respondWithError( error );
        }
    }

    @Override
    public void setProvider(Provider provider) {
        this.contentProvider = provider;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public File getContentFile(Uri uri, CancellationSignal signal) {
        FileAuthorityResponse response = new FileAuthorityResponse( signal );
        Map<String,Object> params = new HashMap<>();
        for( String name : uri.getQueryParameterNames() ) {
            params.put( name, uri.getQueryParameter( name ) );
        }
        writeResponse( response, uri.getPath(), params );
        return response.getFile();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order, CancellationSignal signal) {
        CursorAuthorityResponse response = new CursorAuthorityResponse( signal );
        // TODO Need to consider how to reconcile these params with the other methods.
        Map<String,Object> params = m(
            kv("selection",     selection ),
            kv("projection",    projection ),
            kv("args",          args ),
            kv("order",         order )
        );
        writeResponse( response, uri.getPath(), params );
        return response.getCursor();
    }

    @Override
    public Object getContent(CompoundURI uri, String path, Map<String, Object> params) {
        ContentScheme.AuthorityResponse response = new ContentScheme.AuthorityResponse( context, uri );
        writeResponse( response, path, params );
        return response;
    }

    @Override
    public void notifyIOCObject(Object object, String propertyName) {
        if( authorityName == null ) {
            // Default the authority name to the name of the property it's bound to.
            authorityName = propertyName;
        }
    }

    @Override
    public void startService() {

    }

    @Override
    public void stopService() {

    }
}
