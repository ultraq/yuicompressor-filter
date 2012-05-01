
package nz.net.ultraq.web.yuicompressor;

import nz.net.ultraq.web.filter.Resource;

import java.io.IOException;

/**
 * Resource file implementation specific to the YUI Compressor Filter.
 * 
 * @author Emanuel Rabina
 */
public class JSCSSResourceFile extends Resource {

	private static final String PATTERN_ALREADY_MINIFIED = ".*\\.min\\.(j|cs)s$";
	private static final String PATTERN_FILETYPE_CSS = ".*\\.css$";
	private static final String PATTERN_FILETYPE_JS  = ".*\\.js$";

	/**
	 * Contstructor, build the resource file from the given path and captured
	 * content.
	 * 
	 * @param path
	 * @param sourcecontent
	 * @throws IOException
	 */
	JSCSSResourceFile(String path, String sourcecontent) throws IOException {

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

		return getFilename().matches(PATTERN_FILETYPE_CSS);
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
