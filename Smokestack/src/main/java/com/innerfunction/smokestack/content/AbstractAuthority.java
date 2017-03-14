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
import android.util.Log;

import com.innerfunction.scffld.IOCObjectAware;
import com.innerfunction.scffld.Service;
import com.innerfunction.smokestack.commands.CommandScheduler;
import com.innerfunction.uri.CompoundURI;
import com.innerfunction.util.Paths;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * An abstract content authority.
 * This class provides standard functionality needed to service requests from the different
 * content URL and resolver subsystems and the content internal URI scheme.
 *
 * Subclasses must provide an implementation of the refreshContent() method suitable for their
 * data source.
 *
 * Created by juliangoacher on 08/03/2017.
 */
public abstract class AbstractAuthority implements Authority, Service, IOCObjectAware {

    static final String Tag = AbstractAuthority.class.getSimpleName();

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
    private Map<String,PathRoot> pathRoots;
    /** A map of record converters, keyed by record type name. */
    private Map<String,RecordConverter> recordTypes = new HashMap<>();
    /** A map of query converters, keyed by record type name. */
    private Map<String,QueryConverter> queryTypes = new HashMap<>();

    public AbstractAuthority(Context context) {
        this.context = context;
    }

    public void setRefreshInterval(float interval) {
        this.refreshInterval = interval;
    }

    public void setRecordTypes(Map<String,RecordConverter> recordTypes) {
        this.recordTypes = recordTypes;
    }

    public void setQueryTypes(Map<String,QueryConverter> queryTypes) {
        this.queryTypes = queryTypes;
    }

    public String getAuthorityName() {
        return authorityName;
    }

    public Provider getContentProvider() {
        return contentProvider;
    }

    public CommandScheduler getCommandScheduler() {
        return contentProvider.getCommandScheduler();
    }

    public void addPathRoot(String pathRootName, PathRoot pathRoot) {
        pathRoots.put( pathRootName, pathRoot );
    }

    /**
     * Refreshed content, e.g. by checking a server for downloadable updates.
     * Subclasses should provide an implementation of this class.
     */
    public abstract void refreshContent();

    /** A path for temporarily staging downloaded content. */
    @Override
    public String getStagingPath() {
        return Paths.join( contentProvider.getStagingPath(), authorityName );
    }

    /** A path for caching app content. */
    @Override
    public String getAppCachePath() {
        return Paths.join( contentProvider.getAppCachePath(), authorityName );
    }

    /** A property for caching downloaded content. */
    @Override
    public String getContentCachePath() {
        return Paths.join( contentProvider.getContentCachePath(), authorityName );
    }

    /** A path for CMS content that has been packaged with the app. */
    @Override
    public String getPackagedContentPath() {
        return Paths.join( contentProvider.getPackagedContentPath(), authorityName );
    }

    public void writeResponse(AuthorityResponse response, ContentPath contentPath, Map<String,Object> params) {
        // Look-up a path root for the first path component, and if one is found then delegate the
        // request to it.
        String root = contentPath.getRoot();
        PathRoot pathRoot = pathRoots.get( root );
        if( pathRoot != null ) {
            // The path root only sees the rest of the path.
            contentPath = contentPath.getRest();
            // Delegate the request.
            pathRoot.writeResponse( this, contentPath, params, response );
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
        ContentPath contentPath = new ContentPath( uri.getPath() );
        writeResponse( response, contentPath, params );
        return response.getContentFile();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order, CancellationSignal signal) {
        Cursor cursor = null;
        ContentPath contentPath = new ContentPath( uri.getPath() );
        // Look-up a path root for the first path component, and if one is found then delegate the
        // request to it.
        String root = contentPath.getRoot();
        PathRoot pathRoot = pathRoots.get( root );
        if( pathRoot != null ) {
            // The path root only sees the rest of the path.
            contentPath = contentPath.getRest();
            // Delegate the request.
            cursor = pathRoot.getCursor( this, contentPath, projection, selection, args, order, signal );
        }
        else {
            // Path not found, respond with error.
            String error = String.format("Path not found: %s", contentPath.getFullPath() );
            Log.w( Tag, error );
        }
        return cursor;
    }

    @Override
    public Object getContent(CompoundURI uri, String path, Map<String, Object> params) {
        ContentScheme.AuthorityResponse response = new ContentScheme.AuthorityResponse( context, uri );
        ContentPath contentPath = new ContentPath( path );
        writeResponse( response, contentPath, params );
        return response.getResource();
    }

    @Override
    public RecordConverter getRecordTypeConverter(String type) {
        return recordTypes.get( type );
    }

    @Override
    public QueryConverter getQueryTypeConverter(String type) {
        return queryTypes.get( type );
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
