
package nz.net.ultraq.web.yuicompressor;

import nz.net.ultraq.web.filter.ResourceProcessingFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.platform.yui.compressor.CssCompressor;

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

/**
 * Filter to minify JavaScript and CSS resources.  Works on those file types
 * that don't end in .min.js/.min.css since they're minified already.
 * <p>
 * Annoyingly, the YUI Compressor (for JavaScript) uses a heavily-modified
 * version of Rhino so you can't easily put the official distributions of Rhino
 * into the classpath and expect things to work.  To get around this, this
 * filter launches the YUI Compressor in a separate classloader.
 * 
 * @author Emanuel Rabina
 */
@WebFilter(
	filterName = "YUICompressorFilter",
	urlPatterns = {
			"*.css",
			"*.js",
			"*.less"
	})
public class YUICompressorFilter extends ResourceProcessingFilter<JSCSSResourceFile> {

	private static final String YUI_COMPRESSOR_JAR = "/WEB-INF/lib/yuicompressor-2.4.7.jar";

	private static final Logger logger = LoggerFactory.getLogger(YUICompressorFilter.class);

	private ClassLoader yuiclassloader;

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
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	protected void doProcessing(JSCSSResourceFile resource) throws IOException, ServletException {

		// Do not process already-minified resources
		if (resource.isAlreadyMinified()) {
			return;
		}

		// Select the appropriate compressor
		if (resource.isCSSFile()) {
			CssCompressor compressor = new CssCompressor(new StringReader(resource.getSourceContent()));
			StringWriter writer = new StringWriter();
			compressor.compress(writer, -1);
			resource.setProcessedContent(writer.toString());
		}
		else if (resource.isJSFile()) {
			try {
				StringWriter writer = new StringWriter();
				Object compressor = jscompressorconstructor.newInstance(new StringReader(resource.getSourceContent()), errorreporter);
				jscompressormethod.invoke(compressor, writer, -1, true, true, false, false);
				resource.setProcessedContent(writer.toString());
			}
			catch (InvocationTargetException | InstantiationException | IllegalAccessException ex) {
				throw new ServletException(ex);
			}
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

		try {
			// Create the YUI classloader
			ServletContext context = filterConfig.getServletContext();
			yuiclassloader = new URLClassLoader(new URL[]{
					context.getResource(YUI_COMPRESSOR_JAR)},
					ClassLoader.getSystemClassLoader().getParent());

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

			// Load YUI Compressor using the YUI classloader
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
