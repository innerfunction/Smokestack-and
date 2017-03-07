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
import android.os.ParcelFileDescriptor;

import com.innerfunction.scffld.Message;
import com.innerfunction.scffld.MessageReceiver;
import com.innerfunction.scffld.MessageRouter;
import com.innerfunction.scffld.Service;
import com.innerfunction.smokestack.commands.CommandScheduler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

/**
 * A class used to provide content access to the smokestack ContentProvider instance.
 * Instances of this class are instantiated and configured by the content app container, and act
 * as a delegate for the Smokestack content provider which is declared in the app manifest. This
 * class in turn delegates requests to content authority instances which are registered with it.
 *
 * @see com.innerfunction.smokestack.ContentProvider
 * @see com.innerfunction.smokestack.AppContainer
 * @see com.innerfunction.smokestack.content.Authority
 *
 * Created by juliangoacher on 07/03/2017.
 */
public class Provider implements Service, MessageRouter, MessageReceiver {

    static final String SmokestackNamePrefix = "smokestack";

    /** A command scheduler to be used by the different content authorities. */
    private CommandScheduler commandScheduler;
    /** A map of content authority instances keyed by authority name. */
    private Map<String,Authority> authorities;
    /** A path for temporarily staging downloaded content. */
    private String stagingPath;
    /** A path for caching app content. */
    private String appCachePath;
    /** A path for caching downloaded content. */
    private String contentCachePath;
    /** A path for app packaged content. */
    private String packagedContentPath;

    public Provider(Context context) {
        commandScheduler = new CommandScheduler( context );
        commandScheduler.setQueueDBName( String.format("%s.commandqueue", SmokestackNamePrefix ) );
    }

    public CommandScheduler getCommandScheduler() {
        return commandScheduler;
    }

    public void setCommandScheduler(CommandScheduler scheduler) {
        this.commandScheduler = scheduler;
    }

    public void setAuthorities(Map<String,Authority> authorities) {
        this.authorities = authorities;
        for( Authority authority : authorities.values() ) {
            authority.setProvider( this );
        }
    }

    public void setStagingPath(String path) {
        this.stagingPath = path;
    }

    public String getStagingPath() {
        return stagingPath;
    }

    public void setAppCachePath(String path) {
        this.appCachePath = path;
    }

    public String getAppCachePath() {
        return appCachePath;
    }

    public void setContentCachePath(String path) {
        this.contentCachePath = path;
    }

    public String getContentCachePath() {
        return contentCachePath;
    }

    public void setPackagedContentPath(String path) {
        this.packagedContentPath = path;
    }

    public String getPackagedContentPath() {
        return packagedContentPath;
    }

    public Authority getContentAuthority(Uri uri) throws FileNotFoundException {
        String authorityName = uri.getAuthority();
        Authority authority = authorities.get( authorityName );
        if( authority == null ) {
            throw new FileNotFoundException( authorityName );
        }
        return authority;
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        Authority authority = getContentAuthority( uri );
        File contentFile = authority.getContentFile( uri, signal );
        if( contentFile == null ) {
            throw new FileNotFoundException( uri.toString() );
        }
        return ParcelFileDescriptor.open( contentFile, ParcelFileDescriptor.MODE_READ_ONLY );
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order, CancellationSignal signal) {
        try {
            Authority authority = getContentAuthority( uri );
            return authority.query( uri, projection, selection, args, order, signal );
        }
        catch(FileNotFoundException e) {
            // TODO What should be returned in this case?
            return null;
        }
    }

    @Override
    public boolean routeMessage(Message message, Object sender) {
        boolean routed = false;
        String authorityName = message.targetHead();
        Authority authority = authorities.get( authorityName );
        if( authority instanceof MessageReceiver ) {
            routed = ((MessageReceiver)authority).receiveMessage( message, sender );
        }
        return routed;
    }

    @Override
    public boolean receiveMessage(Message message, Object sender) {
        return false;
    }

    @Override
    public void startService() {}

    @Override
    public void stopService() {}

}
