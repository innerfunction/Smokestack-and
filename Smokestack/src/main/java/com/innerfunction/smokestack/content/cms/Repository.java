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
import com.innerfunction.scffld.Configuration;
import com.innerfunction.scffld.Container;
import com.innerfunction.scffld.IOCContainerAware;
import com.innerfunction.scffld.IOCObjectAware;
import com.innerfunction.scffld.IOCProxyLookup;
import com.innerfunction.scffld.Message;
import com.innerfunction.scffld.MessageReceiver;
import com.innerfunction.smokestack.commands.CommandScheduler;
import com.innerfunction.smokestack.content.AbstractAuthority;
import com.innerfunction.smokestack.content.AuthorityResponse;
import com.innerfunction.smokestack.content.ContentPath;
import com.innerfunction.smokestack.db.Record;
import com.innerfunction.smokestack.db.ResultSet;
import com.innerfunction.util.KeyPath;

import org.json.simple.JSONObject;

import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Map;

import static com.innerfunction.util.DataLiterals.*;

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

    public void setCms(Settings cms) {
        this.cms = cms;
        authManager.setCMSSettings( cms );
    }

    public Settings getCms() {
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

    /**
     * Do a user login.
     * @param credentials A map with 'username' and 'password' values.
     * @return A deferred promise resolving to 'true' if the login is successful.
     */
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

    /** Test whether any user is currently logged in. */
    public boolean isLoggedIn() {
        return authManager.hasCredentials();
    }

    /** Logout the current user and remove their stored credentials. */
    public Q.Promise<Boolean> logout() {
        authManager.removeCredentials();
        return forceRefresh();
    }

    /** Force a content refresh. */
    private Q.Promise<Boolean> forceRefresh() {
        // Refresh content.
        CommandScheduler commandScheduler = getCommandScheduler();
        commandScheduler.purgeQueue();
        String cmd = getAuthorityName().concat(".refresh");
        return commandScheduler.execCommand( cmd );
    }

    /** Refresh content my checking for updates on the server. */
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

    static {
        // Register this class' configuration proxy.
        IOCProxyLookup.registerProxyClass( Repository.IOCProxy.class, Repository.class );
    }

    public static final class IOCProxy implements com.innerfunction.scffld.IOCProxy, IOCObjectAware, IOCContainerAware {

        /** The Android context. */
        private Context context;
        /** The container that built this object. */
        private Container container;
        /** The name of the authority that the class instance is bound to. */
        private String authorityName;
        /** The file database settings. */
        private JSONObject fileDB = new JSONObject( m(
            kv("name",      "$dbName"),
            kv("version",   1 ),
            kv("tables", m(
                kv("files", m(
                    kv("columns", m(
                        kv("id",            m( kv("type", "INTEGER"), kv("tag", "id") ) ),
                        kv("path",          m( kv("type", "STRING") ) ),
                        kv("category",      m( kv("type", "STRING") ) ),
                        kv("status",        m( kv("type", "STRING") ) ),
                        kv("commit",        m( kv("type", "STRING"), kv("tag", "version") ) )
                    ))
                )),
                kv("posts", m(
                    kv("columns", m(
                        kv("id",            m( kv("type", "INTEGER"), kv("tag", "id") ) ),
                        kv("type",          m( kv("type", "STRING") ) ),
                        kv("title",         m( kv("type", "STRING") ) ),
                        kv("body",          m( kv("type", "STRING") ) ),
                        kv("image",         m( kv("type", "INTEGER") ) ),
                        kv("commit",        m( kv("type", "STRING"), kv("tag", "version") ) )
                    ))
                )),
                kv("commits", m(
                    kv("columns", m(
                        kv("commit",        m( kv("type", "STRING"), kv("tag", "id") ) ),
                        kv("date",          m( kv("type", "STRING") ) ),
                        kv("subject",       m( kv("type", "STRING") ) )
                    ))
                )),
                kv("meta", m(
                    kv("columns", m(
                        kv("id",            m( kv("type", "STRING"),  kv("tag", "id"), kv("format", "{fileid}:{key}") ) ),
                        kv("fileid",        m( kv("type", "INTEGER"), kv("tag", "ownerid") ) ),
                        kv("key",           m( kv("type", "STRING"),  kv("tag", "key") ) ),
                        kv("value",         m( kv("type", "STRING") ) ),
                        kv("commit",        m( kv("type", "STRING"),  kv("tag", "version") ) )
                    ))
                ))
            )),
            kv("orm", m(
                kv("source",    "files"),
                kv("mappings", m(
                    kv("post", m(
                        kv("relation",  "object"),
                        kv("table",     "posts")
                    )),
                    kv("commit", m(
                        kv("relation",  "shared-object"),
                        kv("table",     "commits")
                    )),
                    kv("meta", m(
                        kv("relation",  "map"),
                        kv("table",     "meta")
                    ))
                ))
            )),
            kv("filesets", m(
                kv("posts", m(
                    kv("includes",  l( "posts/*.json" ) ),
                    kv("mappings",  l( "commit", "meta", "post" ) ),
                    kv("cache",     "none")
                )),
                kv("pages", m(
                    kv("includes",  l( "pages/*.json" ) ),
                    kv("mappings",  l( "commit", "meta" ) ),
                    kv("cache",     "none")
                )),
                kv("images", m(
                    kv("includes",  l( "posts/images/*", "pages/images/*" ) ),
                    kv("mappings",  l( "commit", "meta" ) ),
                    kv("cache",     "content")
                )),
                kv("assets", m(
                    kv("includes",  l( "assets/**" ) ),
                    kv("mappings",  l( "commit" ) ),
                    kv("cache",     "content")
                )),
                kv("templates", m(
                    kv("includes",  l( "templates/**" ) ),
                    kv("mappings",  l( "commit" ) ),
                    kv("cache",     "app")
                ))
            ))
        ));
        /** The CMS settings (host / account / repo). */
        private JSONObject cms;
        /** The path roots supported by this authority. */
        private JSONObject pathRoots = new JSONObject( m(
            kv("~posts", "$postsPathRoot"),
            kv("~pages", "$postsPathRoot"),
            kv("~files", m(
                kv("-class", "com.innerfunction.smokestack.content.cms.FilesetCategoryPathRoot")
            ))
        ));
        /** The content refresh interval, in minutes. */
        private float refreshInterval = 1.0f; // Refresh once per minute.
        /** An action to be performed after a logout. e.g. after the server returns a 401. */
        private String logoutAction;

        public IOCProxy(Context context) {
            this.context = context;
        }

        public void setFileDB(JSONObject fileDB) {
            this.fileDB = fileDB;
        }

        public void setCms(JSONObject cms) {
            this.cms = cms;
        }

        public void setPathRoots(JSONObject pathRoots) {
            this.pathRoots = pathRoots;
        }

        public void setRefreshInterval(float refreshInterval) {
            this.refreshInterval = refreshInterval;
        }

        public void setLogoutAction(String logoutAction) {
            this.logoutAction = logoutAction;
        }

        @Override
        public void notifyIOCObject(Object object, String propertyName) {
            this.authorityName = propertyName;
        }

        @Override
        public void setIOCContainer(Container container) {
            this.container = container;
        }

        @Override
        public Object unwrapValue() {
            // Build configuration for authority object.
            Configuration config = new Configuration( m(
                kv("authorityName",     authorityName ),
                kv("fileDB",            fileDB ),
                kv("cms",               cms ),
                kv("pathRoots",         pathRoots ),
                kv("refreshInterval",   refreshInterval )
            ), context );
            config = config.extendWithParameters( m(
                kv("authorityName",     authorityName ),
                kv("dbName",            String.format("%s.%s", cms.get("account"), cms.get("repo") ) ),
                kv("postsPathRoot",     new PostPathRoot() )
            ) );

            // Ask the container to build the authority object.
            Repository repository = new Repository( context );
            container.configureObject( repository, config, authorityName );
            repository.setLogoutAction( logoutAction );

            // By default, the initial copy of the db file is stored in the main app bundle under
            // the db name.
            FileDB fileDB = repository.fileDB;
            String initialCopyPath = fileDB.getInitialCopyPath();
            if( initialCopyPath == null ) {
                String filename = fileDB.getName()+".sqlite";
                fileDB.setInitialCopyPath( filename );
            }

            // Ensure a path root exists for each fileset, and is associated with the fileset.
            Map<String, Fileset> filesets = fileDB.getFilesets();
            for( String category : filesets.keySet() ) {
                Fileset fileset = filesets.get( category );
                // Note that fileset category path roots are prefixed with a tilde.
                String pathRootName = "~"+category;
                Object pathRoot = pathRoots.get( pathRootName );
                if( pathRoot == null ) {
                    // Create a default path root for the current category.
                    FilesetCategoryPathRoot categoryPathRoot  = new FilesetCategoryPathRoot( fileset, repository );
                    repository.addPathRoot( pathRootName, categoryPathRoot );
                }
                else if( pathRoot instanceof FilesetCategoryPathRoot ) {
                    // Path root for category found, match it up with its fileset and the authority.
                    FilesetCategoryPathRoot categoryPathRoot = (FilesetCategoryPathRoot)pathRoot;
                    categoryPathRoot.setFileset( fileset );
                    categoryPathRoot.setRepository( repository );
                }
            }

            return repository;
        }

    }

}
