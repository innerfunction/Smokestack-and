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
package com.innerfunction.smokestack.content.cms;

import android.util.Log;

import com.innerfunction.smokestack.content.AuthorityResponse;
import com.innerfunction.smokestack.db.Record;
import com.innerfunction.util.Files;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;

import java.io.File;

/**
 * A path root providing post content.
 *
 * Created by juliangoacher on 11/03/2017.
 */
public class PostPathRoot extends FilesetCategoryPathRoot {

    static final String Tag = PostPathRoot.class.getSimpleName();

    /**
     * Render a post's full HTML content.
     * Combines a post's body HTML with the appropriate client-side template to
     * generate the full HTML page.
     */
    public String renderPostContent(Record post) {
        String postType = post.getValueAsString("posts.type");
        String postHTML = null;
        // Resolve the client template to use to render the post.
        String templateFilename = String.format("_templates/post-%s.html", postType );
        String templatePath = fileDB.getCacheLocationForFileWithPath( templateFilename );
        File templateFile = new File( templatePath );
        if( !templateFile.exists() ) {
            templatePath = fileDB.getCacheLocationForFileWithPath("_templates/post.html");
            templateFile = new File( templatePath );
            if( !templateFile.exists() ) {
                Log.w( Tag, String.format("Client template not found for post type %s", postType ) );
                templateFile = null;
            }
        }
        if( templateFile != null ) {
            // Load the template and render the post.
            String template = Files.readString( templateFile );
            try {
                postHTML = Mustache.compiler().compile( template ).execute( post );
            }
            catch(MustacheException e) {
                Log.e( Tag, "Template error", e );
                postHTML = String.format("<h1>Template error</h1><pre>%s</pre>", e.getMessage() );
            }
        }
        if( postHTML == null ) {
            // If failed to render content then return a default rendering of the post body.
            String postBody = post.getValueAsString("posts.body");
            postHTML = String.format("<p>%s</p>", postBody );
        }
        return postHTML;
    }

    @Override
    public void writeRecordContent(Record content, String type, final AuthorityResponse response) {
        String postHTML = renderPostContent( content );
        if( "html".equals( type ) ) {
            // Respond with just the post HTML.
            response.respondWithStringData( postHTML, "text/html");
        }
        else {
            // Respond with the post JSON data with the post HTML added.
            content.put("postHTML", postHTML );
            super.writeRecordContent( content, type, response );
        }
    }

}
