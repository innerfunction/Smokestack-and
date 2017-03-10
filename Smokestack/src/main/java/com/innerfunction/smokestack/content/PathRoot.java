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

import android.database.Cursor;
import android.os.CancellationSignal;

import java.util.Map;

/**
 * The root of a content path.
 * Used by content authorities to delegate content resolution.
 *
 * Created by juliangoacher on 08/03/2017.
 */
public interface PathRoot {

    /**
     * Generate and write a file based content response.
     * @param authority The authority handling the content request.
     * @param path      The (parsed) path to the requested content.
     * @param params    The (parsed) content request parameters.
     * @param response  An object to write the response to.
     */
    void writeResponse(Authority authority, ContentPath path, Map<String,Object> params, AuthorityResponse response);

    /**
     * Make and return a cursor based content response.
     * @param authority     The authority handling the content request.
     * @param path          The (parsed) path to the requested content.
     * @param projection    A list of fields to include in the result.
     * @param selection     A selection statement.
     * @param args          Arguments to the selection statement.
     * @param order         The result sort order.
     * @param signal        An object used to signal request cancellation.
     */
    Cursor getCursor(Authority authority, ContentPath path, String[] projection, String selection, String[] args, String order, CancellationSignal signal);

}
