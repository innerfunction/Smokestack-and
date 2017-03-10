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

import com.innerfunction.uri.CompoundURI;

import java.io.File;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Map;

/**
 * An interface implemented by classes which act as content authorities to content providers.
 *
 * Created by juliangoacher on 07/03/2017.
 */
public interface Authority {

    /** A path for temporarily staging downloaded content. */
    String getStagingPath();

    /** A path for caching app content. */
    String getAppCachePath();

    /** A property for caching downloaded content. */
    String getContentCachePath();

    /** A path for CMS content that has been packaged with the app. */
    String getPackagedContentPath();

    /**
     * Set the content provider using this authority.
     * Allows access to standard configuration settings and the command scheduler.
     * @param provider
     */
    void setProvider(Provider provider);

    /**
     * Return the type of the content associated with a URI.
     * This is one of the authority's methods used to service requests from Android's internal
     * content URL or content resolver subsystems. Type here refers to an internal Android record
     * or cursor type identifier.
     *
     * @link https://developer.android.com/reference/android/content/ContentProvider.html#getType(android.net.Uri)
     *
     * @param uri   The URI of the content whose type is required.
     * @return A string specifying the content type.
     */
    String getType(Uri uri);

    /**
     * Return the content associated with a URI.
     * This is one of the authority's methods used to service requests from Android's internal
     * content URL or content resolver subsystems.
     *
     * @param uri       The content URI to resolve.
     * @param signal    An object used to signal request cancellation.
     * @return A file containing the required content; may return null if the content isn't found.
     */
    File getContentFile(Uri uri, CancellationSignal signal);

    /**
     * Return a cursor for iterating over the data associated with a URI.
     * This is one of the authority's methods used to service requests from Android's internal
     * content URL or content resolver subsystems.
     *
     * @param uri           The content URI to resolve.
     * @param projection    An array specifying names of data fields to return.
     * @param selection     An array specifying the selection criteria.
     * @param args          An array of arguments to the selection criteria.
     * @param order         A string specifying the result sort order.
     * @param signal        An object used to signal request cancellation.
     * @return A cursor over the required content; may return null if the content isn't found.
     */
    Cursor query(Uri uri, String[] projection, String selection, String[] args, String order, CancellationSignal signal);

    /**
     * Return the content associated with the specified path.
     * This method is used to resolve data for content: URIs in the SDK's internal URI system.
     *
     * @param uri       The URI being requested.
     * @param path      The content path.
     * @param params    Associated request parameters.
     * @return An object encapsulating the required data.
     */
    Object getContent(CompoundURI uri, String path, Map<String,Object> params);

    /**
     * Return password details suitable for authenticating an HTTP request.
     * May return null if the request isn't relevant to the authority.
     * @param authRealm The HTTP authentication realm.
     * @param url       The request URL.
     * @return An object encapsulating username and password details, or null if no credentials are
     *         available.
     */
    PasswordAuthentication getPasswordAuthentication(String authRealm, URL url);

    /**
     * Return the content converter for a specified record type.
     */
    RecordConverter getRecordTypeConverter(String type);

    /**
     * Return the content converter for a specified query type.
     */
    QueryConverter getQueryTypeConverter(String type);

}
