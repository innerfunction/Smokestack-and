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
package com.innerfunction.smokestack.content.cms;

import android.content.Context;

import com.innerfunction.http.Client;
import com.innerfunction.http.Response;
import com.innerfunction.q.Q;
import com.innerfunction.scffld.Message;
import com.innerfunction.scffld.MessageReceiver;
import com.innerfunction.smokestack.commands.CommandScheduler;
import com.innerfunction.smokestack.content.AbstractAuthority;
import com.innerfunction.smokestack.content.AuthorityResponse;
import com.innerfunction.smokestack.content.ContentPath;
import com.innerfunction.smokestack.db.Record;
import com.innerfunction.smokestack.db.ResultSet;
import com.innerfunction.util.KeyPath;

import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Map;

/**
 * A class representing a content repository in the Smokestack CMS.
 *
 * Created by juliangoacher on 09/03/2017.
 */
public class Repository extends AbstractAuthority implements MessageReceiver {

    /** The content repository settings. */
    private Settings cms;
    /** The file database. */
    private FileDB fileDB;
    /** An HTTP client. */
    private Client httpClient;
    /** An object for managing user authentication credentials etc. */
    private AuthenticationManager authManager;
    /** An action to be posted after logging out a user. */
    private String logoutAction;

    public Repository(Context context) {
        super( context );
        this.httpClient = new Client( context );
        this.authManager = new AuthenticationManager( context );
        this.fileDB = new FileDB( this );
    }

    public void setCMS(Settings cms) {
        this.cms = cms;
        authManager.setCMSSettings( cms );
    }

    public Settings getCMS() {
        return cms;
    }

    public AuthenticationManager getAuthManager() {
        return authManager;
    }

    public void setLogoutAction(String logoutAction) {
        this.logoutAction = logoutAction;
    }

    public String getLogoutAction() {
        return logoutAction;
    }

    public void setFileDB(FileDB fileDB) {
        this.fileDB = fileDB;
    }

    public FileDB getFileDB() {
        return fileDB;
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public Q.Promise<Boolean> loginWithCredentials(Map<String,Object> credentials) {
        // Check for username & password.
        String username = KeyPath.getValueAsString("username", credentials );
        if( username == null ) {
            return Q.reject("Missing username");
        }
        String password = KeyPath.getValueAsString("password", credentials );
        if( password == null ) {
            return Q.reject("Missing password");
        }
        final Q.Promise promise = new Q.Promise<>();
        // Register with the auth manager.
        authManager.registerCredentials( credentials );
        // Authenticate against the backend.
        String authURL = cms.getURLForAuthentication();
        httpClient.post( authURL, null )
            .then( new Q.Promise.Callback<Response, Void>() {
                @Override
                public Void result(Response response) {
                    int responseCode = response.getStatusCode();
                    boolean authenticated = false;
                    if( responseCode == 200 ) {
                        Map<String, Object> data = (Map<String, Object>)response.parseBodyData();
                        authenticated = KeyPath.getValueAsBoolean("authenticated", data );
                    }
                    if( authenticated ) {
                        promise.resolve( forceRefresh() );
                    }
                    else {
                        // Authentication failure.
                        authManager.removeCredentials();
                        promise.reject("Authentication failure");
                    }
                    return null;
                }
            })
            .error( new Q.Promise.ErrorCallback() {
                @Override
                public void error(Exception e) {
                    // Authentication failure.
                    authManager.removeCredentials();
                    promise.reject("Authentication failure");
                }
            });

        return promise;
    }

    public boolean isLoggedIn() {
        return authManager.hasCredentials();
    }

    public Q.Promise<Boolean> logout() {
        authManager.removeCredentials();
        return forceRefresh();
    }

    private Q.Promise<Boolean> forceRefresh() {
        // Refresh content.
        CommandScheduler commandScheduler = getCommandScheduler();
        commandScheduler.purgeQueue();
        String cmd = getAuthorityName().concat(".refresh");
        return commandScheduler.execCommand( cmd );
    }

    @Override
    public void refreshContent() {
        String cmd = getAuthorityName().concat(".refresh");
        CommandScheduler commandScheduler = getCommandScheduler();
        commandScheduler.appendCommand( cmd );
        commandScheduler.executeQueue();
    }

    @Override
    public void writeResponse(AuthorityResponse response, ContentPath contentPath, Map<String,Object> params) {
        // A tilde at the start of a path indicates a fileset category reference; so any path which
        // doesn't start with tilde is a direct reference to a file by its path. Convert the
        // reference to a fileset reference by looking up the file ID and category for the path.
        String root = contentPath.getRoot();
        if( root.charAt( 0 ) == '~' ) {
            // Lookup file entry by path.
            String filePath = contentPath.getFullPath();
            ResultSet rs = fileDB.performQuery("SELECT id, category FROM files WHERE path=?", filePath );
            if( rs.size() > 0 ) {
                // File entry found in database; rewrite content path to a direct resource reference.
                Record row = rs.get( 0 );
                String fileID = row.getValueAsString("id");
                String category = row.getValueAsString("category");
                String resourcePath = String.format("~%s/$%s", category, fileID );
                String ext = contentPath.getExt();
                if( ext != null ) {
                    resourcePath = resourcePath+"."+ext;
                }
                contentPath = new ContentPath( resourcePath );
            }
        }
        // Continue with standard response behaviour.
        super.writeResponse( response, contentPath, params );
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication(String realm, URL url) {
        // Return the user's stored credentials, if any, if the request is for this authority's
        // configured HTTP authentication realm.
        if( realm != null && realm.equals( cms.getAuthRealm() ) ) {
            return authManager.getPasswordAuthentication();
        }
        return null;
    }

    @Override
    public boolean receiveMessage(Message message, Object sender) {
        if( message.hasName("logout") ) {
            authManager.removeCredentials();
            return true;
        }
        return false;
    }

    @Override
    public void startService() {
        super.startService();
        // Register command protocol with the scheduler, using the authority name as the
        // command prefix.
        getCommandScheduler().setCommand( getAuthorityName(), new CommandProtocol( this ) );
        // Refresh the app content on start.
        refreshContent();
    }

}
