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

import java.io.File;

/**
 * An interface for writing responses to content requests.
 *
 * Created by juliangoacher on 08/03/2017.
 */
public interface AuthorityResponse {

    /**
     * Respond with content data.
     * Writes the response data in full and then ends the response.
     */
    void respondWithData(byte[] data, String mimeType);

    /**
     * Respond with string data of the specified MIME type.
     */
    void respondWithStringData(String data, String mimeType);

    /**
     * Respond with JSON data.
     */
    void respondWithJSONData(Object data);

    /**
     * Respond with file data of the specified MIME type.
     */
    void respondWithFileData(File file, String mimeType);

    /**
     * Respond with an error indicating why the request couldn't be resolved.
     */
    void respondWithError(String message);

    /**
     * Start a content response.
     * Content data should be sent by calling the send(..) method. The response must be completed
     * with a call to end().
     */
    void start(String mimeType);

    /**
     * Write content data to the response.
     * The response must be started with a call to the start(..) method before data is sent by
     * calling this method. This method may then be called as many times as necessary to write the
     * content data in full. The end() method must be called once all data is written.
     */
    void write(byte[] data);

    /**
     * End a content response.
     */
    void end();

}
