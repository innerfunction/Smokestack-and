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

import android.database.Cursor;
import android.os.CancellationSignal;
import android.text.TextUtils;

import com.innerfunction.http.Client;
import com.innerfunction.http.Response;
import com.innerfunction.q.Q;
import com.innerfunction.smokestack.content.Authority;
import com.innerfunction.smokestack.content.AuthorityResponse;
import com.innerfunction.smokestack.content.ContentPath;
import com.innerfunction.smokestack.content.QueryConverter;
import com.innerfunction.smokestack.content.RecordConverter;
import com.innerfunction.smokestack.content.PathRoot;
import com.innerfunction.smokestack.db.ORM;
import com.innerfunction.smokestack.db.Record;
import com.innerfunction.smokestack.db.ResultSet;
import com.innerfunction.util.Files;
import com.innerfunction.util.Paths;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A default path root implementation for access to a single category of fileset contents.
 *
 * Created by juliangoacher on 10/03/2017.
 */
public class FilesetCategoryPathRoot implements PathRoot {

    /** The fileset being accessed. */
    protected Fileset fileset;
    /** The content repository. */
    protected Repository repository;
    /** The file database. */
    protected FileDB fileDB;
    /** The file DB's object-relation mapping layer. */
    protected ORM orm;
    /** An HTTP client. */
    protected Client httpClient;

    public FilesetCategoryPathRoot() {}

    /** Initialize the path root with the specified fileset and content authority. */
    public FilesetCategoryPathRoot(Fileset fileset, Repository repository) {
        this.fileset = fileset;
        setRepository( repository );
    }

    public void setFileset(Fileset fileset) {
        this.fileset = fileset;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
        setFileDB( repository.getFileDB() );
        setHttpClient( repository.getHttpClient() );
    }

    public void setFileDB(FileDB fileDB) {
        this.fileDB = fileDB;
        this.orm = fileDB.getOrm();
    }

    public void setHttpClient(Client httpClient) {
        this.httpClient = httpClient;
    }

    /** Query the file database for entries in the current fileset. */
    public ResultSet queryWithParameters(Map<String,Object> parameters) {

        List<String> wheres = new ArrayList<>();
        List<String> values = new ArrayList<>();
        List<String> mappings = Collections.emptyList();

        // Note that category field is qualifed by source table name.
        if( fileset != null ) {
            wheres.add( String.format("%s.category = ?", orm.getSource() ) );
            values.add( fileset.getCategory() );
            mappings = fileset.getMappings();
        }

        // Add filters for each of the specified parameters.
        for( String key : parameters.keySet() ) {
            // Note that parameter names must be qualified by the correct relation name.
            wheres.add( String.format("%s = ?", key ) );
            values.add( parameters.get( key ).toString() );
        }

        // Join the wheres into a single where clause.
        String where = TextUtils.join(" AND ", wheres );
        // Execute query and return result.
        return fileDB.getOrm().selectWhere( mappings, where, values );
    }

    /** Read a single entry from the file database by key (i.e. file ID). */
    public Record getRecordWithKey(String key) {
        // Read the content and return the result.
        return orm.selectKey( key, fileset.getMappings() );
    }

    /** Read a single file record from the database by file path. */
    public Record getRecordWithPath(String path) {
        ResultSet rs = orm.selectWhere( fileset.getMappings(), "path = ?", path );
        return rs.size() > 0 ? rs.get( 0 ) : null;
    }

    /** Write a query response. */
    public void writeQueryContent(ResultSet content, String type, AuthorityResponse response) {
        if( type != null ) {
            QueryConverter typeConverter = repository.getQueryTypeConverter( type );
            if( typeConverter != null ) {
                typeConverter.writeContent( content, response );
            }
            else {
                response.respondWithError( String.format("Unsupported query type: %s", type ) );
            }
        }
        else {
            response.respondWithJSONData( content );
        }
    }

    /** Write an entry response. */
    public void writeRecordContent(Record content, String type, final AuthorityResponse response) {
        if( type != null ) {
            RecordConverter typeConverter = repository.getRecordTypeConverter( type );
            if( typeConverter != null ) {
                typeConverter.writeContent( content, response );
            }
            else {
                // No specific type converter found. Check if the content file type is compatible
                // with the requested type and that fileset info is available.
                String path = content.getValueAsString("path");
                String ext = Paths.extname( path );
                if( fileset != null && type.equals( ext ) ) {
                    final String mimeType = null; // TODO
                    String cachePath = fileDB.getCacheLocationForFileRecord( content );
                    final File cacheFile = new File( cachePath );
                    final boolean cachable = fileset.isCachable();
                    // Check if a local copy of the file exists in the cache.
                    if( cachable && cacheFile.exists() ) {
                        // Local copy found, respond with contents.
                        response.respondWithFileData( cacheFile, mimeType );
                    }
                    else {
                        // No local copy found, download from server.
                        String url = repository.getCms().getURLForFile( path );
                        httpClient.getFile( url )
                            .then( new Q.Promise.Callback<Response, Response>() {
                                @Override
                                public Response result(Response httpResponse) {
                                    File downloadFile = httpResponse.getDataFile();
                                    // If cachable then move file to cache.
                                    if( cachable ) {
                                        Files.mv( downloadFile, cacheFile );
                                        downloadFile = cacheFile;
                                    }
                                    // Respond with file contents.
                                    response.respondWithFileData( downloadFile, mimeType );
                                    return httpResponse;
                                }
                            })
                            .error( new Q.Promise.ErrorCallback() {
                                public void error(Exception e) {
                                    response.respondWithError( e.getMessage() );
                                }
                            });
                    }
                }
                else {
                    response.respondWithError( String.format("Unsupported type: %s", type ) );
                }
            }
        }
        else {
            response.respondWithJSONData( content );
        }
    }

    @Override
    public void writeResponse(Authority authority, ContentPath path, Map<String, Object> params, AuthorityResponse response) {
        if( path.isEmpty() ) {
            // Content path references a content query.
            String type = path.getExt();
            ResultSet content = queryWithParameters( params );
            writeQueryContent( content, type, response );
        }
        else {
            // Content path references a resource (i.e. file entry). The resource identifier can be
            // in the format ${key}.{type}; i.e. if prefixed with a dollar symbol then the resource
            // is referenced by file ID and has a type modifier. Otherwise, the relative portion of
            // the path at this point can be used to reference a resource by its file path.
            Record record = null;
            String head = path.getRoot();
            String type = path.getExt();
            // Check for reference by ID.
            if( head.charAt( 0 ) == '$' && path.length() == 1 ) {
                String key = head.substring( 1 );
                record = getRecordWithKey( key );
            }
            // If no content yet then may be referenced by file path.
            if( record == null ) {
                record = getRecordWithPath( path.getRelativePath() );
            }
            // If now have an entry then return it, else return an error.
            if( record != null ) {
                writeRecordContent( record, type, response );
            }
            else {
                response.respondWithError( String.format("Invalid path: %s", path.getFullPath() ) );
            }
        }
    }

    @Override
    public Cursor getCursor(Authority authority, ContentPath path, String[] projection, String selection, String[] args, String order, CancellationSignal signal) {
        // TODO
        return null;
    }
}
