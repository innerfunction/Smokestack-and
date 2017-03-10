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
package com.innerfunction.smokestack.commands;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of commands.
 * This class is a lightweight extension of ArrayList which provides a generically typed
 * List&lt;CommandItem&gt; implementation which is more convenient to use in client code (e.g.
 * command protocol implementations).
 *
 * Created by juliangoacher on 10/03/2017.
 */
public class CommandList extends ArrayList<CommandItem> {

    /**
     * Add a command item to the list.
     * @param name  The name of the command.
     * @param args  The command's arguments.
     */
    public void addCommand(String name, Object... args) {
        add( new CommandItem( name, args ) );
    }

    /**
     * Add a command item to the list.
     * @param name  The name of the command.
     * @param args  The command's arguments.
     */
    public void addCommand(String name, List args) {
        add( new CommandItem( name, args ) );
    }
}