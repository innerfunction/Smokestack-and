// Copyright 2016 InnerFunction Ltd.
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
package com.innerfunction.smokestack.commands;

import android.util.Log;

import com.innerfunction.q.Q;

import org.json.simple.JSONValue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An object representing a command item on the execution queue
 *
 * Created by juliangoacher on 10/03/2017.
 */
public class CommandItem {

    /** The ID of the command's DB record. */
    String rowID;
    /** The command name. */
    String name;
    /** A list of the command arguments. */
    List args;
    /** The command's execution priority. */
    Integer priority;
    /** An optional promise to be resolved once the command item is executed. */
    Q.Promise<Boolean> promise;

    /** Instantiate an empty command item. */
    CommandItem() {}

    /** Instantiate a new command item from a db record. */
    CommandItem(Map<String,?> record) {
        this.rowID = record.get("id").toString();
        this.name = (String)record.get("command");
        try {
            String json = (String)record.get("args");
            this.args = (List)JSONValue.parseWithException( json );
        }
        catch(org.json.simple.parser.ParseException e) {
            Log.e( CommandScheduler.Tag, "Parsing JSON args", e );
        }
    }

    /** Instantiate a new command item with a command name and list of arguments. */
    public CommandItem(String name, Object... args) {
        this.name = name;
        this.args = Arrays.asList( args );
    }

    /** Instantiate a new command item with a command name and list of arguments. */
    public CommandItem(String name, List args) {
        this.name = name;
        this.args = args;
    }
}