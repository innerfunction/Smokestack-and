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

import android.content.Context;
import android.util.Log;

import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.FileResource;
import com.innerfunction.uri.Resource;
import com.innerfunction.uri.URIScheme;
import com.innerfunction.util.Files;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * An internal content URI scheme handler.
 *
 * Created by juliangoacher on 08/03/2017.
 */
public class ContentScheme implements URIScheme {

    private Provider contentProvider;

    public void setContentProvider(Provider provider) {
        this.contentProvider = provider;
    }

    @Override
    public Object dereference(CompoundURI uri, Map<String, Object> params) {
        Object content = null;
        // The compound URI name contains both the authority and content path (e.g. as
        // content://{authority}/{path/to/content}).
        String name = uri.getName();
        if( name != null ) {
            // Split leading // from name.
            name = name.substring( 2 );
            // Find end of authority name.
            int idx = name.indexOf('/');
            if( idx > -1 ) {
                String authorityName = name.substring( 0, idx );
                // Find the content authority.
                Authority contentAuthority = contentProvider.getContentAuthority( authorityName );
                if( contentAuthority != null ) {
                    String path = name.substring( idx + 1 );
                    content = contentAuthority.getContent( uri, path, params );
                }
            }
        }
        return content;
    }

    /**
     * An implementation of the AuthorityResponse interface that wraps response data in a Resource.
     * TODO Check standard Resource type conversions - particularly byte[] -> JSON data
     */
    public static class AuthorityResponse implements com.innerfunction.smokestack.content.AuthorityResponse {

        static final String Tag = AuthorityResponse.class.getSimpleName();

        /** The Android context. */
        private Context context;
        /** The URI used to request the data. */
        private CompoundURI uri;
        /** An internal buffer for storing incremental responses. */
        private ByteArrayOutputStream buffer;
        /** The response data, as a byte array, string or JSON data. */
        private Object data;
        /** The response data, as a file. */
        private File file;

        /**
         * Create a new resource.
         *
         * @param context An Android context object (needed for some type conversions).
         * @param uri     The URI used to reference the resource.
         */
        public AuthorityResponse(Context context, CompoundURI uri) {
            this.context = context;
            this.uri = uri;
        }

        public Resource getResource() {
            if( data != null ) {
                return new Resource( context, data, uri );
            }
            else if( file != null ) {
                return new FileResource( context, file, uri ) {
                    @Override
                    public URI asURL() {
                        // Use the content URI as the file's URL.
                        return URI.create( uri.toString() );
                    }
                };
            }
            return null;
        }

        @Override
        public void respondWithData(byte[] data, String mimeType) {
            this.data = data;
        }

        @Override
        public void respondWithStringData(String data, String mimeType) {
            this.data = data;
        }

        @Override
        public void respondWithJSONData(Object data) {
            this.data = data;
        }

        @Override
        public void respondWithFileData(File file, String mimeType) {
            this.file = file;
        }

        @Override
        public void respondWithError(String message) {
            Log.e( Tag, message );
        }

        @Override
        public void start(String mimeType) {
            this.buffer = new ByteArrayOutputStream();
        }

        @Override
        public void write(byte[] data) {
            try {
                if( buffer != null ) {
                    buffer.write( data );
                }
            }
            catch(IOException e) {
                // Unlikely to happen, but if it does then terminate the response.
                buffer = null;
                respondWithError( e.getMessage() );
            }
        }

        @Override
        public void end() {
            this.data = buffer.toByteArray();
            buffer = null;
        }
    }

}
