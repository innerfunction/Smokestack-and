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
package com.innerfunction.smokestack.db;

import android.text.TextUtils;

import com.innerfunction.scffld.IOCObjectAware;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A class providing simple object-relational mapping capability.
 * The class maps objects, represented as dictionary instances, to a source table in
 * a local SQLite database. Compound properties of each object can be defined as joins
 * between the source table and other related tables, with 1:1, 1:Many and Many:1 relations
 * supported.
 *
 * Created by juliangoacher on 09/03/2017.
 */
public class ORM implements IOCObjectAware {

    /** The name of the relation source table. */
    private String source;
    /** A dictionary of relation mappings from the source table, keyed by name. */
    private Map<String,Mapping> mappings;
    /** The database. */
    private DB db;

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setMappings(Map<String,Mapping> mappings) {
        this.mappings = mappings;
    }

    public Map<String,Mapping> getMappings() {
        return mappings;
    }

    /**
     * Select the object with the specified key value.
     * Returns the object record from the source table, with all related properties
     * named in the mappings argument joined from the related tables.
     */
    public Record selectKey(String key, List<String> mappings) {
        String idColumn = getIDColumnForTable( source );
        String where = String.format("%s.%s=?", source, idColumn );
        ResultSet result = selectWhere( mappings, where, Arrays.asList( key ) );
        return result.size() > 0 ? result.get( 0 ) : null;
    }

    /**
     * Select the objects matching the specified where condition.
     */
    public ResultSet selectWhere(List<String> mappings, String where, String... values) {
        return selectWhere( mappings, where, Arrays.asList( values ) );
    }

    /**
     * Select the objects matching the specified where condition.
     * Returns an array of object records from the source table, with all related properties
     * named in the mappings argument joined from the related tables.
     */
    public ResultSet selectWhere(List<String> mappings, String where, List<String> values) {
        // The name of the ID column on the source table.
        String sidColumn = getIDColumnForTable( source );
        // Generate SQL to describe each join for each relation.
        List<String> columns = new ArrayList<>();     // Array of column name lists for source table and all joins.
        List<String> joins = new ArrayList<>();       // Array of join SQL.
        List<String> orderBys = new ArrayList<>();    // Array of order by column names.
        List<String> collectionJoins = new ArrayList<>();  // Array of collection relation names.

        columns.add( getColumnNamesForTable( source, source  ) );

        for( String mname : this.mappings.keySet() ) {

            // Skip the mapping if its name isn't in the list of mappings to include.
            if( !mappings.contains( mname ) ) {
                continue;
            }

            ORM.Mapping mapping = this.mappings.get( mname );
            String mtable = mapping.getTable();
            String relation = mapping.getRelation();

            if( "object".equals( relation ) || "property".equals( relation ) ) {

                columns.add( getColumnNamesForTable( mtable, mname ) );
                String midColumn = getColumnWithNameOrTag( mtable, mapping.getIdColumn(), "id" );
                String join = String.format("LEFT OUTER JOIN %s %s ON %s.%s=%s.%s",
                    mtable,
                    mname,
                    mtable,
                    midColumn,
                    source,
                    sidColumn );

                joins.add( join );
            }
            else if( "shared-object".equals( relation ) || "shared-property".equals( relation ) ) {

                columns.add( getColumnNamesForTable( mtable, mname ) );
                String midColumn = getColumnWithNameOrTag( mtable, mapping.getIdColumn(), "id" );
                String join = String.format("LEFT OUTER JOIN %s %s ON %s.%s=%s.%s",
                    mtable,
                    mname,
                    source,
                    mname,
                    mname,
                    midColumn );

                joins.add( join );
            }
            else if( "map".equals( relation ) || "dictionary".equals( relation ) ||
                     "array".equals( relation ) || "list".equals( relation ) ) {

                columns.add( getColumnNamesForTable( mtable, mname ) );
                String oidColumn = getColumnWithNameOrTag( mtable, mapping.getOwneridColumn(), "ownerid" );
                String join = String.format("LEFT OUTER JOIN %s %s ON %s.%s=%s.%s",
                    mtable,
                    mname,
                    source,
                    sidColumn,
                    mname,
                    oidColumn );

                joins.add( join );
                collectionJoins.add( mname );

                // Order the result by the index column; note that this will be empty for
                // map/dictionary sets (i.e. unordered collections), but will have values for
                // array/list items.
                String idxColumn = getColumnWithNameOrTag( mtable, mapping.getIndexColumn(), "index" );
                orderBys.add( String.format("%s.%s", mname, idxColumn ) );
            }
        }
        // Generate select SQL.
        String sql = String.format("SELECT %s FROM %s %s %s WHERE %s",
            TextUtils.join(",", columns ),
            source,
            source,
            TextUtils.join(" ", joins ),
            where );

        if( orderBys.size() > 0 ) {
            sql = sql+"ORDER BY "+TextUtils.join(",", orderBys );
        }

        // Execute the query and generate the result.
        ResultSet rs = db.performQuery( sql, values );
        ResultSet result = new ResultSet();
        // The fully qualified name of the source object key column in the result set.
        String keyColumn = String.format("%s.%s", source, sidColumn );
        // The object currently being processed.
        Record obj = null;
        for( Record row : rs ) {
            Object key = row.get( keyColumn ); // Read the key value from the current result set row.
            // Convert flat result set row into groups of properties sharing the same column name prefix.
            Record groups = new Record();
            for( String cname : row.keySet() ) {
                Object value = row.get( cname );
                // Only map columns with values.
                if( value != null ) {
                    // Split column name into prefix/suffix parts.
                    int idx = cname.indexOf('.');
                    String prefix = cname.substring( 0, idx );
                    String suffix = cname.substring( idx + 1 );
                    // Ensure that we have a record for the prefix group.
                    Record group = (Record)groups.get( prefix );
                    if( group != null ) {
                        group = new Record();
                        groups.put( prefix, group );
                    }
                    // Map the value to the suffix name within the group.
                    group.put( suffix, value );
                }
            }
            // Check if dealing with a new object.
            if( obj == null || !key.equals( obj.get( sidColumn ) ) ) {
                // Convert groups into object + properties.
                obj = (Record)groups.get( source );
                for( String rname : groups.keySet() ) {
                    Object value = groups.get( rname );
                    if( !rname.equals( source ) ) {
                        // If relation name is for an outer join - i.e. a one to many - then init
                        // the object property as an array of values.
                        if( collectionJoins.contains( rname ) ) {
                            List<Object> vlist = new ArrayList<>();
                            vlist.add( value );
                            obj.put( rname, vlist );
                        }
                    else {
                            // Else map the object property name to the value.
                            obj.put( rname, value );
                        }
                    }
                }
                result.add( obj );
            }
            else for( String rname : collectionJoins ) {
                // Processing subsequent rows for the same object - indicates outer join results.
                List<Object> vlist = (List<Object>)obj.get( rname );
                if( vlist != null ) {
                    // Ensure that we have a list to hold the additional values.
                    vlist = new ArrayList<>();
                    obj.put( rname, vlist );
                }
                // If we have a value for the current relation group then add to the property value list.
                Object value = groups.get( rname );
                if( value != null ) {
                    vlist.add( value );
                }
            }
        }
        return result;
    }

    /**
     * Delete the object with the specified key value.
     * Deletes any related records unique to the deleted object.
     */
    public boolean deleteKey(String key) {
        boolean ok = true;
        db.beginTransaction();
        String sql;
        for( String mname : mappings.keySet() ) {
            Mapping mapping = mappings.get( mname );
            String relation = mapping.relation;
            if( "map".equals( relation ) || "dictionary".equals( relation ) ||
                "array".equals( relation ) || "list".equals( relation ) ) {
                String oidColumn = getColumnWithNameOrTag( mapping.table, mapping.owneridColumn, "ownerid");
                sql = String.format("DELETE FROM %s WHERE %s=?", mapping.table, oidColumn );
                ok &= db.performUpdate( sql, key );
            }
        }
        // The name of the ID column on the source table.
        String sidColumn = getIDColumnForTable( source );
        // TODO Support deletion of many-one relations by deleting records from relation table where no foreign key value in source table.
        sql = String.format("DELETE FROM %s WHERE %s=?", source, sidColumn );
        ok &= db.performUpdate( sql, key );
        if( ok ) {
            db.commitTransaction();
        }
        else {
            db.rollbackTransaction();
        }
        return ok;
    }

    /** Return a column name, or if not specified, the name of the column on a table with the specified tag. */
    public String getColumnWithNameOrTag(String table, String name, String tag) {
        if( name == null ) {
            name = db.getColumnForTag( table, tag );
            if( name == null ) {
                name = tag;
            }
        }
        return name;
    }

    private String getColumnNamesForTable(String table, String prefix) {
        String columnNames = null;
        Table tableDef = db.getTables().get( table );
        if( tableDef != null ) {
            List<String> columns = new ArrayList<>();
            for( Column columnDef : tableDef.columns ) {
                String column = String.format("%s.%s", prefix, columnDef.name );
                columns.add( String.format("%s AS '%s'", column, column ) );
            }
            columnNames = TextUtils.join(",", columns );
        }
        return columnNames;
    }

    private String getIDColumnForTable(String table) {
        return db.getColumnForTag( table, "id");
    }

    @Override
    public void notifyIOCObject(Object object, String propertyName) {
        if( object instanceof DB ) {
            this.db = (DB)object;
        }
    }

    /** A class describing a relation mapping between a source and property value table. */
    public static class Mapping {

        /**
         * The relation type.
         * Values are 'object'/'property', 'shared-object'/'shared-property',
         * 'map'/'dictionary', 'array'/'list'.
         */
        private String relation;
        /** The name of the table holding the related values. */
        private String table;
        /** The name of the ID column on the joined table. */
        private String idColumn;
        /** The name of the key column for map/dictionary items. */
        private String keyColumn;
        /** The name of the index column for list/array items. */
        private String indexColumn;
        /** The name of the owner ID column for map/dictionary/array/list items. */
        private String owneridColumn;
        /** The name of the version column. */
        private String verColumn;

        public String getRelation() {
            return relation;
        }

        public void setRelation(String relation) {
            this.relation = relation;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public String getIdColumn() {
            return idColumn;
        }

        public void setIdColumn(String idColumn) {
            this.idColumn = idColumn;
        }

        public String getKeyColumn() {
            return keyColumn;
        }

        public void setKeyColumn(String keyColumn) {
            this.keyColumn = keyColumn;
        }

        public String getIndexColumn() {
            return indexColumn;
        }

        public void setIndexColumn(String indexColumn) {
            this.indexColumn = indexColumn;
        }

        public String getOwneridColumn() {
            return owneridColumn;
        }

        public void setOwneridColumn(String owneridColumn) {
            this.owneridColumn = owneridColumn;
        }

        public String getVerColumn() {
            return verColumn;
        }

        public void setVerColumn(String verColumn) {
            this.verColumn = verColumn;
        }

        /** Test whether the mapping represents a (non-shared) object or property mapping. */
        public boolean isObjectMapping() {
            return "object".equals( relation ) || "property".equals( relation );
        }

        /** Test whether the mapping represents a shared object or property mapping. */
        public boolean isSharedObjectMapping() {
            return "shared-object".equals( relation ) || "shared-property".equals( relation );
        }
    }

}
