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
package com.innerfunction.smokestack.db;

import java.util.ArrayList;

/**
 * A result set returned by DB queries etc.
 * Note that a result set is a lightweight extension of ArrayList, and is essentially just a List
 * of Record objects, each record corresponding to a single row of the cursor result set returned
 * by the underlying sqlite database API.
 *
 * Created by juliangoacher on 09/03/2017.
 */
public class ResultSet extends ArrayList<Record> {
}
