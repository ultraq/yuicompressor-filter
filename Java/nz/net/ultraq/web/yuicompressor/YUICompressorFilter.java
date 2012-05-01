
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
	urlPatterns = {"*.css", "*.js"}
)
public class YUICompressorFilter extends ResourceProcessingFilter<JSCSSResourceFile> {

	private static final String YUI_ERRORREPORTER_CLASS      = "org.mozilla.javascript.ErrorReporter";
	private static final String YUI_EVALUATOREXCEPTION_CLASS = "org.mozilla.javascript.EvaluatorException";
	private static final String YUI_JSCOMPRESSOR_CLASS       = "com.yahoo.platform.yui.compressor.JavaScriptCompressor";

	private static final Logger logger = LoggerFactory.getLogger(YUICompressorFilter.class);

	private ClassLoader yuiclassloader;

	private Class<?> jscompressorclass;
	private Constructor<?> jscompressorconstructor;
	private Method jscompressormethod;

	private Class<?> errorreporterclass;
	private Object errorreporter;
	private InvocationHandler errorreporterhandler = new InvocationHandler() {
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
	};

	private Class<?> evaluatorexceptionclass;
	private Constructor<?> evaluatorexceptionconstructor;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected JSCSSResourceFile buildResource(String path, String resourcecontent) throws IOException {

		return new JSCSSResourceFile(path, resourcecontent);
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void destroy() {
	}

	/**
	 * Minify a JavaScript or CSS file using the YUI Compressor.
	 * 
	 * @param resource
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	protected JSCSSResourceFile doProcessing(JSCSSResourceFile resource) throws IOException, ServletException {

		// Do not process already-minified resources
		if (resource.isAlreadyMinified()) {
			return resource;
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

		return resource;
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
					context.getResource("/WEB-INF/lib/yuicompressor-2.4.7.jar")},
					ClassLoader.getSystemClassLoader().getParent());

			// Stuff needed for ErrorReporter, used by the JavaScript Compressor
			errorreporterclass = Class.forName(YUI_ERRORREPORTER_CLASS, true, yuiclassloader);
			errorreporter = Proxy.newProxyInstance(yuiclassloader, new Class[]{errorreporterclass}, errorreporterhandler);

			evaluatorexceptionclass = Class.forName(YUI_EVALUATOREXCEPTION_CLASS, true, yuiclassloader);
			evaluatorexceptionconstructor = evaluatorexceptionclass.getConstructor(String.class, String.class,
					Integer.TYPE, String.class, Integer.TYPE);

			// Load YUI Compressor using the YUI classloader
			jscompressorclass = Class.forName(YUI_JSCOMPRESSOR_CLASS, true, yuiclassloader);
			jscompressorconstructor = jscompressorclass.getConstructor(Reader.class, errorreporterclass);
			jscompressormethod = jscompressorclass.getMethod("compress", Writer.class, Integer.TYPE,
					Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE);
		}
		catch (MalformedURLException | ClassNotFoundException | NoSuchMethodException ex) {
			throw new ServletException(ex);
		}
	}
}
