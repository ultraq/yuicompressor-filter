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

import nz.net.ultraq.postprocessing.ResourceProcessingFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

/**
 * Filter to minify JavaScript and CSS resources.  Works on those file types
 * that don't end in .min.js/.min.css since they're minified already.
 * <p>
 * Annoyingly, the YUI Compressor (for JavaScript) uses a heavily-modified
 * version of Rhino so you can't easily put the official distributions of Rhino
 * into the classpath and expect things to work.  To get around this, this
 * filter launches the YUI Compressor in a separate classloader.  This filter
 * also needs to be told of the location of the YUI Compressor JAR though since
 * you might want to have this JAR outside of the normal classpath so it doesn't
 * interfere with other JARs that use Rhino.  If not specified, it will look in
 * <tt>/WEB-INF/lib-sandbox/yuicompressor-2.4.7.jar</tt>, but this can be
 * overridden by providing the necessary init parameter in your <tt>web.xml</tt>
 * file:
 * <pre>
 * &lt;filter&gt;
 *   &lt;filter-name&gt;YUICompressorFilter&lt;/filter-name&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;YUI_COMPRESSOR_JAR&lt;/param-name&gt;
 *     &lt;param-value&gt;location-of/yuicompressor.jar&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 * &lt;/filter&gt;
 * </pre>
 * Also, since having the minification on during the development process can be
 * annoying, this filter's processing can be disabled by setting a system
 * property like so:
 * <p>
 * <code>-Dnz.net.ultraq.web.yuicompressor.DisableProcessing=true</code>
 * 
 * @author Emanuel Rabina
 */
@WebFilter(
	filterName = "YUICompressorFilter",
	urlPatterns = {"*.css", "*.js", "*.less"},
	initParams = @WebInitParam(name = "YUI_COMPRESSOR_JAR", value = "/WEB-INF/lib-sandbox/yuicompressor-2.4.7.jar")
)
public class YUICompressorFilter extends ResourceProcessingFilter<JSCSSResourceFile> {

	public static final String DISABLE_PROCESSING_FLAG = "nz.net.ultraq.web.yuicompressor.DisableProcessing";
	public static final String YUI_COMPRESSOR_JAR = "YUI_COMPRESSOR_JAR";

	private static final Logger logger = LoggerFactory.getLogger(YUICompressorFilter.class);

	private boolean disableProcessing;
	private String yuicompressorjar;

	private ClassLoader yuiclassloader;

	private Constructor<?> csscompressorconstructor;
	private Method csscompressormethod;

	private Constructor<?> jscompressorconstructor;
	private Method jscompressormethod;
	private Class<?> errorreporterclass;
	private Object errorreporter;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected JSCSSResourceFile buildResource(String path, String resourcecontent) throws IOException {

		return new JSCSSResourceFile(path, resourcecontent);
	}

	/**
	 * Minify a JavaScript or CSS file using the YUI Compressor.
	 * 
	 * @param resource
	 * @throws ServletException
	 */
	@Override
	protected void doProcessing(JSCSSResourceFile resource) throws ServletException {

		// Do not process already-minified resources, do not process when the
		// DisableProcessing flag is set
		if (resource.isAlreadyMinified() || disableProcessing) {
			resource.setProcessedContent(resource.getSourceContent());
			return;
		}

		try {
			// Select the appropriate compressor
			if (resource.isCSSFile()) {
				StringWriter writer = new StringWriter();
				Object compressor = csscompressorconstructor.newInstance(new StringReader(resource.getSourceContent()));
				csscompressormethod.invoke(compressor, writer, -1);
				resource.setProcessedContent(writer.toString());
			}
			else if (resource.isJSFile()) {
				StringWriter writer = new StringWriter();
				Object compressor = jscompressorconstructor.newInstance(new StringReader(resource.getSourceContent()), errorreporter);
				jscompressormethod.invoke(compressor, writer, -1, true, true, false, false);
				resource.setProcessedContent(writer.toString());
			}
		}
		catch (InvocationTargetException | InstantiationException | IllegalAccessException ex) {
			throw new ServletException(ex);
		}
	}

	/**
	 * Create a new classloader in which to run the YUI Compressor.
	 * 
	 * @param filterConfig
	 * @throws ServletException
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

		// Check if processing is disabled
		if (Boolean.getBoolean(DISABLE_PROCESSING_FLAG)) {
			disableProcessing = true;
			return;
		}

		// Set location YUI Compressor JAR
		yuicompressorjar = filterConfig.getInitParameter(YUI_COMPRESSOR_JAR);

		try {
			// Create the YUI classloader
			ServletContext context = filterConfig.getServletContext();
			yuiclassloader = new URLClassLoader(new URL[]{
					context.getResource(yuicompressorjar)},
					ClassLoader.getSystemClassLoader().getParent());

			// Load CSS Compressor using the YUI classloader
			Class<?> csscompressorclass = Class.forName(
					"com.yahoo.platform.yui.compressor.CssCompressor", true, yuiclassloader);
			csscompressorconstructor = csscompressorclass.getConstructor(Reader.class);
			csscompressormethod = csscompressorclass.getMethod("compress", Writer.class, Integer.TYPE);

			// Stuff needed by the JavaScript Compressor - ErrorReporter, EvaluatorException

			Class<?> evaluatorexceptionclass = Class.forName(
					"org.mozilla.javascript.EvaluatorException", true, yuiclassloader);
			final Constructor<?> evaluatorexceptionconstructor = evaluatorexceptionclass.getConstructor(
					String.class, String.class, Integer.TYPE, String.class, Integer.TYPE);

			errorreporterclass = Class.forName("org.mozilla.javascript.ErrorReporter", true, yuiclassloader);
			errorreporter = Proxy.newProxyInstance(yuiclassloader, new Class[]{errorreporterclass}, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					switch (method.getName()) {
					case "warning":
						logger.warn("Issue encountered in {}:{} - {}", new Object[]{args[1], args[2], args[0]});
						break;
					case "error":
						logger.error("Error encountered in {}:{} - {}", new Object[]{args[1], args[2], args[0]});
						break;
					case "runtimeError":
						return evaluatorexceptionconstructor.newInstance(args);
					}
					return null;
				}
			});

			// Load JS Compressor using the YUI classloader
			Class<?> jscompressorclass = Class.forName(
					"com.yahoo.platform.yui.compressor.JavaScriptCompressor", true, yuiclassloader);
			jscompressorconstructor = jscompressorclass.getConstructor(Reader.class, errorreporterclass);
			jscompressormethod = jscompressorclass.getMethod("compress", Writer.class, Integer.TYPE,
					Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE);

		}
		catch (MalformedURLException | ClassNotFoundException | NoSuchMethodException ex) {
			throw new ServletException(ex);
		}
	}
}
