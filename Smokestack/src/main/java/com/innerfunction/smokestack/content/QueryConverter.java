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

import com.innerfunction.smokestack.db.ResultSet;

/**
 * An interface implemented by classes which can convert content query data to some output format.
 * Query data is provided as a DB result set.
 *
 * Created by juliangoacher on 10/03/2017.
 */
public interface QueryConverter {

    /** Convert some content to the output format, and write to the specified response object. */
    void writeContent(ResultSet content, AuthorityResponse response);

}
