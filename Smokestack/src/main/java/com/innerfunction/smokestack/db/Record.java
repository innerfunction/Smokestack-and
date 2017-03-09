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

import java.util.HashMap;

/**
 * A database record.
 * Returned by the DB in result sets etc. Note that the class is a lightweight extension of HashMap,
 * and so DB records are essentially just maps of field names onto values.
 *
 * Created by juliangoacher on 09/03/2017.
 */
public class Record extends HashMap<String,Object> {

    /**
     * Return a field value as an integer.
     * @param field The name of the field to get.
     * @return The integer value of the field; or -1 if the field is null or not a number.
     */
    public int getValueAsInteger(String field) {
        Object value = get( field );
        return value instanceof Number ? ((Number)value).intValue() : -1;
    }

    /**
     * Return a field value as a string.
     * @param field The name of the field to get.
     * @return The string value of the field, or null if the field is not a string.
     */
    public String getValueAsString(String field) {
        Object value = get( field );
        return value == null ? null : value.toString();
    }
}
