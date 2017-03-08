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

import com.innerfunction.scffld.IOCObjectAware;
import com.innerfunction.smokestack.content.Authority;
import com.innerfunction.util.Paths;

import org.json.JSONArray;

/**
 * A class declaring the options available on a fileset database configuration entry.
 *
 * Created by juliangoacher on 08/03/2017.
 */
public class Fileset implements IOCObjectAware {

    /** A list of the mapping names supported by the fileset. */
    private JSONArray mappings;
    /**
     * The fileset's caching policy.
     * One of the following strings:
     * <ul>
     * <li>none: The content is always downloaded from the server, never cached locally.</li>
     * <li>content: The content is downloaded and stored in the content cache. Data
     *   in the content cache may be removed by the OS to free up space, after which the
     *   content needs to be downloaded again if required.</li>
     * <li>app: The content is downloaded and stored in the app cache. Data in the app
     *   cache will only be removed when the app is uninstalled.</li>
     * </ul>
     */
    private String cache;
    /** The fileset's category name. */
    private String category;
    /** A flag indicating whether a fileset's content should be downloaded and cached. */
    private boolean cachable;

    public void setCache(String cache) {
        this.cache = cache;
        this.cachable = "content".equals( cache ) || "app".equals( cache );
    }

    /** Get the path of the cache location for this fileset. */
    public String getCachePath(Authority authority) {
        String path = null;
        if( "content".equals( cache ) ) {
            path = Paths.join( authority.getContentCachePath(), "~"+category );
        }
        else if ("app".equals( cache ) ) {
            path = Paths.join( authority.getAppCachePath(), "~"+category );
        }
        return path;
    }

    @Override
    public void notifyIOCObject(Object object, String propertyName) {
        // Record the fileset's category name as the name this object is bound to in its parent
        // object.
        this.category = propertyName;
    }
}
