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

import java.net.PasswordAuthentication;

/**
 * An interface for managing a content authority's user authentication credentials.
 * Created by juliangoacher on 09/03/2017.
 */
public interface AuthenticationManager {

    /** Delete any stored user credentials */
    void removeCredentials();

    /** Check whether any user credentials are stored. */
    boolean hasCredentials();

    /** Return user credentials as a PasswordAuthentication instance. */
    PasswordAuthentication getPasswordAuthentication();

}
