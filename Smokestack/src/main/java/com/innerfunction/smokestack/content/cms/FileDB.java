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

import com.innerfunction.smokestack.db.DB;
import com.innerfunction.util.Paths;

import java.util.List;
import java.util.Map;

/**
 * A content repository's file database.
 *
 * Created by juliangoacher on 09/03/2017.
 */
public class FileDB extends DB {

    /** The content authority this database belongs to. */
    private ContentAuthority authority;
    /** The fileset categories defined for the database. */
    private Map<String,Fileset> filesets;
    /** The name of the files table; defaults to 'files'. */
    private String filesTable = "files";

    public FileDB(ContentAuthority authority) {
        this.authority = authority;
    }

    public void setFilesets(Map<String,Fileset> filesets) {
        this.filesets = filesets;
    }

    public void setFilesTable(String table) {
        this.filesTable = table;
    }

    /**
     * Return the absolute path for the cache location of the specified file record.
     * Returns null if the file isn't locally cachable.
     */
    public String getCacheLocationForFileRecord(Map<String,Object> record) {
        String location = null;
        Object status = record.get("status");
        String path = (String)record.get("path");
        if( "packaged".equals( status ) ) {
            // Packaged content is distributed with the app, under a folder with the content
            // authority name.
            location = Paths.join( authority.getPackagedContentPath(), path );
        }
        else {
            String category = (String)record.get("category");
            String cachePath = getCacheLocationForFileset( category );
            if( cachePath != null ) {
                location = Paths.join( cachePath, path );
            }
        }
        return location;
    }

    /**
     * Return the absolute path for the cache location of the file with the specified path.
     * Returns null if the file isn't locally cachable.
     */
    public String getCacheLocationForFileWithPath(String path) {
        String location = null;
        String sql = String.format("SELECT * FROM %s WHERE path=?", filesTable );
        List<Map<String,Object>> rs = performQuery( sql, path );
        if( rs.size() > 0 ) {
            location = getCacheLocationForFileRecord( rs.get( 0 ) );
        }
        return location;
    }

    /**
     * Return the path of the cache location for files of the specified fileset category.
     * Returns null if the fileset category isn't locally cachable.
     */
    public String getCacheLocationForFileset(String category) {
        String location = null;
        Fileset fileset = filesets.get( category );
        if( fileset != null && fileset.isCachable() ) {
            location = fileset.getCachePath( authority );
        }
        return location;
    }

    /**
     * Prune ORM related values after applying updates to the database.
     * Deletes records in related tables where the version value (as specified in the table's
     * schema) doesn't match the version value on the source table.
     */
    public boolean pruneRelatedValues() {

    }
}
