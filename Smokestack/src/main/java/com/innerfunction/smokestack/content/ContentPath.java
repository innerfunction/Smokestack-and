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

import android.text.TextUtils;

import java.util.Arrays;
import java.util.List;

/**
 * An object representing a content path.
 * A content path is a string in the form /{c0}/{c1}..{cx}.{ext}, i.e
 * one or more path components followed by an optional path extension.
 * The path root refers to the first component (i.e. c0). The getRest()
 * method returns a new content path composed of all components after
 * the current root (i.e. c1..cx), or null if no components are left.
 *
 * Created by juliangoacher on 08/03/2017.
 */
public class ContentPath {

    /** An array containing all components of the full path. */
    private List<String> path;
    /** The index of the current path's root component. */
    private int rootIdx;
    /** The extension at the end of the path. */
    private String ext;

    /** Initialize with a path string. */
    public ContentPath(String path) {
        // Extract any file extension.
        int idx = path.lastIndexOf('.');
        if( idx > -1 ) {
            ext = path.substring( idx + 1 );
            path = path.substring( 0, idx );
        }
        // Strip any leading slash.
        if( path.charAt( 0 ) == '/' ) {
            path = path.substring( 1 );
        }
        this.path = Arrays.asList( path.split("/") );
    }

    private ContentPath(List<String> path, int rootIdx, String ext) {
        this.path = path;
        this.rootIdx = rootIdx;
        this.ext = ext;
    }

    /** Return the root path component. */
    public String getRoot() {
        return isEmpty() ? null : path.get( rootIdx );
    }

    /** Return the path extension, or nil if the path has no extension. */
    public String getExt() {
        return ext;
    }

    /**
     * Return the portion of the path after the root component.
     * Returns a new content path object whose root component is the path component after the
     * current root.
     */
    public ContentPath getRest() {
        if( isEmpty() ) {
            return null;
        }
        return new ContentPath( path, rootIdx + 1, ext );
    }

    /** Return the length of the path. */
    public int length() {
        return path.size() - rootIdx;
    }

    /** Return a list containing the root component and all path components following it. */
    private List<String> getComponents() {
        return path.subList( rootIdx, path.size() - 1 );
    }

    /** Test if the path is empty, i.e. has no root component. */
    public boolean isEmpty() {
        return rootIdx >= path.size();
    }

    /** Return a string representation of the full path. */
    public String getFullPath() {
        String fullPath = TextUtils.join("/", path );
        if( ext != null ) {
            fullPath = fullPath+"."+ext;
        }
        return fullPath;
    }

    /** Return a string representation of the relative portion of the path. */
    public String getRelativePath() {
        return TextUtils.join(",", getComponents() );
    }
}
