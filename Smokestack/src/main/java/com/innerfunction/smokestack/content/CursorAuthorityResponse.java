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

import java.io.File;

/**
 * A content authority response that returns its data via a database cursor.
 * Used to service requests via the ContentProvider.query(..) method.
 *
 * TODO: The detailed workings of this class need to be worked out.
 * It should be straightforward to build a cursor backed by an array read from e.g. a JSON file;
 * this can then also be done with results returned by DB queries, but in that case would no doubt
 * be simpler and more efficient to fetch the cursor directly from the DB. Because of this, and
 * the questions surrounding how to handle the additional query arguments (projection etc.), it
 * may make sense to split query servicing into a separate code path.
 *
 * Created by juliangoacher on 08/03/2017.
 */
public class CursorAuthorityResponse implements AuthorityResponse {

    private Cursor cursor;
    /** An object used to signal request cancellation. */
    private CancellationSignal cancelSignal;

    public CursorAuthorityResponse(CancellationSignal cancelSignal) {
        this.cancelSignal = cancelSignal;
    }

    public Cursor getCursor() {
        return cursor;
    }

    @Override
    public void respondWithData(byte[] data, String mimeType) {

    }

    @Override
    public void respondWithStringData(String data, String mimeType) {

    }

    @Override
    public void respondWithJSONData(Object data) {

    }

    @Override
    public void respondWithFileData(File file, String mimeType) {

    }

    @Override
    public void respondWithError(String message) {

    }

    @Override
    public void start(String mimeType) {

    }

    @Override
    public void write(byte[] data) {

    }

    @Override
    public void end() {

    }
}
