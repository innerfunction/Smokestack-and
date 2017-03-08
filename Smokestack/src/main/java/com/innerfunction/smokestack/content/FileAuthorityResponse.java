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

import android.os.CancellationSignal;
import android.util.Log;

import com.innerfunction.util.Files;

import java.io.File;

/**
 * A content authority response that returns its data via a file.
 * Used to service requests via the ContentProvider.getContentFile(..) method.
 *
 * Created by juliangoacher on 08/03/2017.
 */
public class FileAuthorityResponse implements AuthorityResponse {

    static final String Tag = FileAuthorityResponse.class.getSimpleName();

    /** The file containing the content response data. */
    private File contentFile;
    /** An object used to signal request cancellation. */
    private CancellationSignal cancelSignal;

    public FileAuthorityResponse(CancellationSignal cancelSignal) {
        this.cancelSignal = cancelSignal;
    }

    public File getContentFile() {
        return contentFile;
    }

    private void createTemporaryFile(String mimeType) {
        if( notCancelled() ) {
            // TODO
        }
    }

    private boolean notCancelled() {
        if( cancelSignal.isCanceled() ) {
            this.contentFile = null;
            return false;
        }
        return true;
    }

    @Override
    public void respondWithData(byte[] data, String mimeType) {
        createTemporaryFile( mimeType );
        Files.writeData( contentFile, data, false );
    }

    @Override
    public void respondWithStringData(String data, String mimeType) {
        createTemporaryFile( mimeType );
        Files.writeString( contentFile, data );
    }

    @Override
    public void respondWithJSONData(Object data) {
        // TODO Serialize to JSON etc.
    }

    @Override
    public void respondWithFileData(File file, String mimeType) {
        this.contentFile = file;
    }

    @Override
    public void respondWithError(String message) {
        Log.e( Tag, message );
        contentFile = null;
    }

    @Override
    public void start(String mimeType) {
        createTemporaryFile( mimeType );
    }

    @Override
    public void write(byte[] data) {
        if( notCancelled() ) {
            Files.writeData( contentFile, data, true ); // NOTE data is appended to file.
        }
    }

    @Override
    public void end() {
        notCancelled();
        // Nothing to do.
    }
}
