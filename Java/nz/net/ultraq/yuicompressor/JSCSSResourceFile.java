/*
 * Copyright 2012, Emanuel Rabina (http://www.ultraq.net.nz/)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nz.net.ultraq.yuicompressor;

import nz.net.ultraq.web.filter.Resource;

/**
 * Resource file implementation specific to the YUI Compressor Filter.
 * 
 * @author Emanuel Rabina
 */
public class JSCSSResourceFile extends Resource {

	private static final String PATTERN_ALREADY_MINIFIED     = ".*\\.min\\.(j|les|cs)s$";
	private static final String PATTERN_FILETYPE_CSS_OR_LESS = ".*\\.(le|c)ss$";
	private static final String PATTERN_FILETYPE_JS          = ".*\\.js$";

	/**
	 * Contstructor, build the resource file from the given path and captured
	 * content.
	 * 
	 * @param path
	 * @param sourcecontent
	 */
	JSCSSResourceFile(String path, String sourcecontent) {

		super(path, sourcecontent);
	}

	/**
	 * Return whether or not the resource is already minified, denoted by the
	 * filename ending with .min.js/.min.css
	 * 
	 * @return <tt>true</tt> if the resource is already minified.
	 */
	boolean isAlreadyMinified() {

		return getFilename().matches(PATTERN_ALREADY_MINIFIED);
	}

	/**
	 * Return whether the resource is a stylesheet, denoted by the filename
	 * ending with .css
	 * 
	 * @return <tt>true</tt> if the resource is a stylesheet.
	 */
	boolean isCSSFile() {

		return getFilename().matches(PATTERN_FILETYPE_CSS_OR_LESS);
	}

	/**
	 * Return whether the resource is a JavaScript file, denoted by the filename
	 * ending with .js
	 * 
	 * @return <tt>true</tt> if the resource is a JavaScript file.
	 */
	boolean isJSFile() {

		return getFilename().matches(PATTERN_FILETYPE_JS);
	}
}
