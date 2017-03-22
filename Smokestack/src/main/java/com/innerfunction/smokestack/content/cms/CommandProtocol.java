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

import android.util.Log;

import com.innerfunction.http.Client;
import com.innerfunction.http.Response;
import com.innerfunction.q.Q;
import com.innerfunction.smokestack.AppContainer;
import com.innerfunction.smokestack.commands.Command;
import com.innerfunction.smokestack.commands.CommandList;
import com.innerfunction.smokestack.commands.CommandScheduler;
import com.innerfunction.smokestack.db.Record;
import com.innerfunction.smokestack.db.ResultSet;
import com.innerfunction.util.Files;
import com.innerfunction.util.KeyPath;

import static com.innerfunction.util.DataLiterals.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CMS operations command protocol.
 *
 * Created by juliangoacher on 09/03/2017.
 */
public class CommandProtocol extends com.innerfunction.smokestack.commands.CommandProtocol {

    static final String Tag = CommandProtocol.class.getSimpleName();

    static final String AcceptMIMETypes = "application/msgpack, application/json;q=0.9, */*;q=0.8";
    static final String AcceptEncodings = "gzip";

    static final Object NullCategory = new Object();

    private AuthenticationManager authManager;
    private String logoutAction;
    /** The content repository's settings. */
    private Settings cms;
    /** The local file database. */
    private FileDB fileDB;
    /** An HTTP client instance. */
    private Client httpClient;


    public CommandProtocol(Repository authority) {
        this.cms = authority.getCms();
        this.authManager = authority.getAuthManager();
        this.logoutAction = authority.getLogoutAction();
        this.httpClient = authority.getHttpClient();
        this.fileDB = authority.getFileDB();

        // Register command handlers.
        addCommand( "refresh", new Command() {
            @Override
            public Q.Promise<CommandList> execute(String name, List args) {
                return CommandProtocol.this.refresh( args );
            }
        } );
        addCommand( "download-fileset", new Command() {
            @Override
            public Q.Promise<CommandList> execute(String name, List args) {
                return CommandProtocol.this.downloadFileset( args );
            }
        } );
    }

    /** Start a content refresh. */
    private Q.Promise<CommandList> refresh(List<String> args) {

        final Q.Promise<CommandList> promise = new Q.Promise<>();
        final String refreshURL = cms.getURLForUpdates();

        // Query the file DB for the latest commit ID.
        Object commit = null, group = null;
        Map<String,Object> params = new HashMap<>();
        params.put("secure", getIsSecureParam() );

        // Read current group fingerprint.
        Record record = fileDB.read("fingerprints","$group");
        if( record != null ) {
            group = record.get("current");
            params.put("group", group );
        }

        // Read latest commit ID.
        ResultSet rs = fileDB.performQuery("SELECT id, max(date) FROM commits");
        if( rs.size() > 0 ) {
            // File DB contains previous commits, read latest commit ID and add as request parameter
            record = rs.get( 0 );
            commit = record.get("id");
            params.put("since", commit );
        }
        // Otherwise simply omit the 'since' parameter; the feed will return all records in the
        // file DB.

        // Specify accepts options.
        Map<String,Object> headers = m(
            kv("Accept",          AcceptMIMETypes ),
            kv("Accept-Encoding", AcceptEncodings )
        );
        //Log.w( Tag, fileDB.performQuery("select * from files").toString() );
        // Fetch updates from the server.
        final Object _commit = commit, _group = group;
        httpClient.get( refreshURL, params, headers )
            .then(new Q.Promise.Callback<Response, Response>() {
                @Override
                public Response result(Response response) {

                    // Create list of follow up commands.
                    CommandList commands = new CommandList();

                    if( response.getStatusCode() == 401 ) {
                        // Authentication failure.
                        if( logoutAction != null ) {
                            AppContainer.getAppContainer().postMessage( logoutAction, this );
                        }
                        else {
                            authManager.removeCredentials();
                        }
                        promise.resolve( commands );
                        return null;
                    }

                    // Read the updates data.
                    Object bodyData = response.parseBodyData();
                    if( bodyData instanceof String ) {
                        // Indicates a server error
                        Log.e( Tag, String.format("%s %s", response.getRequestURL(), bodyData ) );
                        promise.resolve( commands );
                        return null;
                    }
                    else if( bodyData instanceof Map ) {
                        // Write updates to database.
                        Map<String, Object> updatesData = (Map<String, Object>)bodyData;
                        Map<String, Object> updates = (Map<String, Object>)updatesData.get("db");

                        // A map of fileset category names to a 'since' commit value (may be null).
                        Map<String, Object> updatedCategories = new HashMap<>();

                        // Start a DB transaction.
                        fileDB.beginTransaction();

                        // Check group fingerprint to see if a migration is needed.
                        String updateGroup = KeyPath.getValueAsString("repository.group", updatesData );
                        boolean migrate = _group == null || !_group.equals( updateGroup );
                        if( migrate ) {
                            // Performing a migration due to an ACM group ID change; mark all files
                            // as provisionally deleted.
                            fileDB.performUpdate("UPDATE files SET status='deleted'");
                        }
                        // Shift current fileset fingerprints to previous.
                        fileDB.performUpdate("UPDATE fingerprints SET previous=current");

                        // Apply all downloaded updates to the database.
                        for( String tableName : updates.keySet() ) {
                            boolean isFilesTable = "files".equals( tableName );
                            List<Map<String,Object>> table = (List<Map<String,Object>>)updates.get( tableName );
                            for( Map<String,Object> values : table ) {
                                fileDB.upsert( tableName, values );
                                // If processing the files table then record the updated file
                                // category name.
                                if( isFilesTable ) {
                                    String category = KeyPath.getValueAsString("category", values );
                                    String status = KeyPath.getValueAsString("status", values );
                                    if( category != null && !"deleted".equals( status ) ) {
                                        if( _commit != null ) {
                                            updatedCategories.put( category, _commit );
                                        }
                                        else {
                                            updatedCategories.put( category, NullCategory );
                                        }
                                    }
                                }
                            }
                        }

                        // Check for deleted files.
                        ResultSet deleted = fileDB.performQuery("SELECT id, path FROM files WHERE status='deleted'");
                        for( Record record : deleted ) {
                            // Delete cached file, if exists.
                            String path = fileDB.getCacheLocationForFileRecord( record );
                            if( path != null ) {
                                File cacheFile = new File( path );
                                cacheFile.delete();
                            }
                        }

                        // Delete obsolete records.
                        fileDB.performUpdate("DELETE FROM files WHERE status='deleted'");

                        // Prune ORM related records.
                        fileDB.pruneRelatedValues();

                        // Read list of fileset names with modified fingerprints.
                        ResultSet rs = fileDB.performQuery("SELECT category FROM fingerprints WHERE current != previous");
                        for( Record record : rs ) {
                            String category = record.getValueAsString("category");
                            if( "$group".equals( category ) ) {
                                // The ACM group fingerprint entry - skip.
                                continue;
                            }
                            // Map the category name to null - this indicates that the category is
                            // updated, but there is no 'since' parameter, so download a full
                            // update.
                            updatedCategories.put( category, NullCategory );
                        }

                        // Queue downloads of updated category filesets.
                        String command = CommandProtocol.this.getQualifiedCommandName("download-fileset");
                        for( String category : updatedCategories.keySet() ) {
                            Object since = updatedCategories.get( category );
                            // Get cache location for fileset; if nil then don't download the
                            // fileset.
                            String cacheLocation = fileDB.getCacheLocationForFileset( category );
                            if( cacheLocation != null ) {
                                List<String> args = new ArrayList<>();
                                args.add( category );
                                args.add( cacheLocation );
                                if( since != NullCategory ) {
                                    args.add( since.toString() );
                                }
                                commands.addCommand( command, args );
                            }
                        }

                        // Commit the transaction.
                        fileDB.commitTransaction();

                    }

                    promise.resolve( commands );
                    return null;
                }
            })
            .error(new Q.Promise.ErrorCallback() {
                public void error(Exception e) {
                    Log.e( Tag, "Updates download", e );
                    String msg = String.format("Updates download from %s failed: %s",
                        refreshURL, e.getMessage() );
                    promise.reject( msg );
                }
            });

        // Return deferred promise.
        return promise;
    }

    /** Download a fileset. */
    private Q.Promise<CommandList> downloadFileset(List<String> args) {

        final Q.Promise<CommandList> promise = new Q.Promise<>();

        final String category = args.get( 0 );
        final String cachePath = args.get( 1 );

        // Build the fileset URL and query parameters.
        final String filesetURL = cms.getURLForFileset( category );
        Map<String,Object> data = new HashMap<>();
        data.put("secure", getIsSecureParam() );
        if( args.size() > 2 ) {
            data.put("since", args.get( 2 ) );
        }
        // Download the fileset.
        httpClient.getFile( filesetURL, data )
            .then(new Q.Promise.Callback<Response, Response>() {
                @Override
                public Response result(Response response) {
                    int responseCode = response.getStatusCode();
                    if( responseCode == 200 ) {
                        // Unzip downloaded file to content location.
                        File dataFile = response.getDataFile();
                        Files.unzip( dataFile, new File( cachePath ) );
                    }
                    if( responseCode == 200 || responseCode == 204 ) {
                        // Update the fileset's fingerprint.
                        fileDB.performUpdate("UPDATE fingerprints SET previous=current WHERE category=?", category );
                    }
                    // Resolve empty list - no follow-on commands.
                    promise.resolve( CommandScheduler.NoFollowOns );
                    return null;
                }
            })
            .error(new Q.Promise.ErrorCallback() {
                public void error(Exception e) {
                    String msg = String.format("Fileset download from %s failed: %s",
                        filesetURL, e.getMessage() );
                    promise.reject( msg );
                }
            });

        return promise;
    }

    private String getIsSecureParam() {
        return authManager.hasCredentials() ? "true" : "false";
    }
}
