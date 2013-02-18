
YUI Compressor Filter
=====================

YUI Compressor Filter for Java web applications.

This filter applies the [YUI Compressor](http://developer.yahoo.com/yui/compressor/)
over any URLs ending with `.less`, `.css`, or `.js`, creating minified versions
of those resources for serving to users.  Once the file is processed, the result
is cached to save on having to process it again.  Any changes to these files
will be picked up and cause the file to be processed the next time it is
requested.

 - Current version: 1.0.5-SNAPSHOT
 - Release date: ?? ??? 2013


Requirements
------------

 - Java 7
 - A Servlet 3.0 compliant servlet container if you're taking advantage of
   servlet 3.0 annotations, otherwise a Servlet 2.5 compliant servlet container


Installation
------------

### Standalone distribution
1. Copy the JAR from [the latest download bundle](http://www.ultraq.net.nz/downloads/programming/YUI Compressor Filter 1.0.4.zip),
   or build the project from the source code here on GitHub.
2. Place the JAR in the `WEB-INF/lib` directory of your web application.

### For Maven and Maven-compatible dependency managers
Add a dependency to your project with the following co-ordinates:

 - GroupId: `nz.net.ultraq.web.yuicompressor`
 - ArtifactId: `yuicompressor-filter`
 - Version: `1.0.5-SNAPSHOT`


Usage
-----

Annoyingly, the YUI Compressor uses a heavily-modified version of Rhino so you
can't easily put the official distributions of Rhino into the classpath and
expect things to work.  To get around this, the filter launches the YUI
Compressor in a separate classloader.  This filter also needs to be told of the
location of the YUI Compressor JAR though since you might want to have this JAR
outside of the normal classpath so it doesn't interfere with existing
installations of Rhino.  If not specified, the filter will look in `/WEB-INF/lib-sandbox/yuicompressor-2.4.7.jar`,
but this can be overridden by providing the necessary init parameter in your `web.xml`
file:

	<filter>
		<filter-name>YUICompressorFilter</filter-name>
		<filter-class>nz.net.ultraq.web.yuicompressor.YUICompressorFilter</filter-class>
		<init-param>
			<param-name>YUI_COMPRESSOR_JAR</param-name>
			<param-value>location-of/yuicompressor.jar</param-value>
		</init-param>
	</filter>

If you're _not_ taking advantage of servlet 3.0 annotations, then you'll also
need to specify the filter mappings in your `web.xml` file:

	<filter>
		<filter-name>YUICompressorFilter</filter-name>
		<filter-class>nz.net.ultraq.web.yuicompressor.YUICompressorFilter</filter-class>
		<init-param>
			<param-name>YUI_COMPRESSOR_JAR</param-name>
			<param-value>location-of/yuicompressor.jar</param-value>
		</init-param>
	</filter>
	<filter-mapping>
		<filter-name>YUICompressorFilter</filter-name>
		<url-pattern>*.less</url-pattern>
		<url-pattern>*.css</url-pattern>
		<url-pattern>*.js</url-pattern>
	</filter-mapping>


Limitations
-----------

This filter only works on URLs which locate a file on the file system.  This is
a limitation of the way I've chosen to detect changes to the underlying file,
which I've done using Java 7's NIO 2 package.  I do have plans to fix this
though by providing fallbacks for other ways a stylesheet or JavaScript file can
be retrieved.


Changelog
---------

### 1.0.5
 - Project structure reorganization after major fixes to the Gradle build
   scripts.
 - Changed default location of the YUI Compressor JAR to `/WEB-INF/lib-sandbox/yuicompressor-2.4.7.jar`
   since it should reside outside of the normal classpath anyway.

### 1.0.4
 - Update [post-processing-filter](https://github.com/ultraq/post-processing-filter)
   dependency to 1.0.2
 - Minor fixes from the updated [maven-support](https://github.com/ultraq/gradle-support)
   Gradle script.

### 1.0.3
 - Switched from Ant to Gradle as a build tool.
 - Made project available from Maven Central.  Maven co-ordinates added to the
   [Installation](#installation) section.

### 1.0.2
 - Allowed for filter to co-exist with existing Rhino installations.

### 1.0.1
 - Added the ability to disable the filter from minifying resources with a
   system property (an annoyance when doing any client-side debugging).  Just
   add `-Dnz.net.ultraq.web.yuicompressor.DisableProcessing=true` to your VM
   arguments.

### 1.0
 - Initial release
