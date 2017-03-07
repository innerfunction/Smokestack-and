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
import android.net.Uri;
import android.os.CancellationSignal;

import java.io.File;

/**
 * An interface implemented by classes which act as content authorities to content providers.
 *
 * Created by juliangoacher on 07/03/2017.
 */
public interface Authority {

    /**
     * Set the content provider using this authority.
     * Allows access to standard configuration settings and the command scheduler.
     * @param provider
     */
    void setProvider(Provider provider);

    /**
     * Return the content associated with a URI.
     * @param uri       The content URI to resolve.
     * @param signal    A cancellation signal.
     * @return A file containing the required content; may return null if the content isn't found.
     */
    File getContentFile(Uri uri, CancellationSignal signal);

    /**
     * Return a cursor for iterating over the data associated with a URI.
     * @param uri           The content URI to resolve.
     * @param projection    An array specifying names of data fields to return.
     * @param selection     An array specifying the selection criteria.
     * @param args          An array of arguments to the selection criteria.
     * @param order         A string specifying the result sort order.
     * @param signal        A cancellation signal.
     * @return A cursor over the required content; may return null if the content isn't found.
     */
    Cursor query(Uri uri, String[] projection, String selection, String[] args, String order, CancellationSignal signal);

}
