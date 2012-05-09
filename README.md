
YUI Compressor Filter
=====================

YUI Compressor Filter for Java web applications.

This filter applies the [YUI Compressor](http://developer.yahoo.com/yui/compressor/)
over any URLs ending with `.less`, `.css`, or `.js`, creating minified versions
of those resources for serving to users.  Once the file is processed, the result
is cached to save on having to process it again.  Any changes to these files
will be picked up and cause the file to be processed the next time it is
requested.


Requirements
------------

 - Java 7
 - A Servlet 3.0 compliant servlet container if you're taking advantage of
   servlet 3.0 annotations, otherwise a Servlet 2.5 compliant servlet container


Installation
------------

1. Download a copy of of the pre-compiled JAR from [my website](http://www.ultraq.net.nz/downloads/projects/YUI Compressor Filter 1.0.zip)
   or build the project from the source code here on GitHub.
2. Place the JAR in the `WEB-INF/lib` directory of your web application.


Usage
-----

1. That's it!  Unless...
2. ...if you're _not_ taking advantage of servlet 3.0 annotations, then you'll need
   to specify the filter in your `web.xml` file:

	<filter>
		<filter-name>YUICompressorFilter</filter-name>
		<filter-class>nz.net.ultraq.web.yuicompressor.YUICompressorFilter</filter-class>
	</filter>
	<filter-mapping>
		<filter-name>YUICompressorFilter</filter-name>
		<url-pattern>*.less</url-pattern>
		<url-pattern>*.css</url-pattern>
		<url-pattern>*.js</url-pattern>
	</filter-mapping>


Limitations
-----------

 - This filter only works on URLs which locate a file on the file system.  This
   is a limitation of the way I've chosen to detect changes to the underlying
   file, which I've done using Java 7's NIO 2 package.  I do have plans to fix
   this though by providing fallbacks for other ways a stylesheet or JavaScript
   file can be retrieved.

