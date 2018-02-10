# Description

In Spring Boot 1.5.10.RELEASE, jar resources are not available from the ServletContext if the
they are served from the file system and exist in a location with a path that contains a space.

For example, this would occur, when running a web application via the Spring Boot Maven plugin and
with the local Maven repository existing at something like `C:\foo bar\.m2\repository`. It would
also occur when running directly from an IDE e.g. IntelliJ.

I've only checked for this bug on Windows. I've also verified that this bug occurs in 2.0.0.RC1.

This results in "works on my machine" problems as an application will work correctly on developer
machines that do not contain a space in their local Maven repository but will fail otherwise. There
is also no obvious indication that the space in the path was the cause of the issue.

# Steps to reproduce

* Checkout the [example repository](https://github.com/spring-projects/spring-boot)
  * Note that it depends on the Bootstrap Webjar in pom.xml
  * Note that it attempts to retrieve a valid CSS file from the ServletContext and will throw an
    exception during startup if it is not found in com.github.rupert654.spaceinpathbug.Application.
* Run with a local repository that does not contain a space:
  * `mvn clean spring-boot:run -Dmaven.repo.local="C:\foo\.m2\repository"`
* Verify that the application starts up without any exception.
* Run with a local repository that does contain a space:
  * `mvn clean spring-boot:run -Dmaven.repo.local="C:\foo bar\.m2\repository"`
* Verify that the application fails to start up with an exception.

# Root Cause

Resource jars are jars that contain a "META-INF/resources" directory.

All jars are retrieved from the class loader by: `((URLClassLoader) classLoader).getURLs()`. These
URLs are then filtered to those that contain this directory and are then
added to the ServletContext.

In 2.0.0.RC1, this process begins in `org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory.StaticResourceConfigurer.lifecycleEvent`.
In 1.5.10.RELEASE, it begins in `org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory.prepareContext`

The URLs are eventually passed into the constructor of `java.util.jar.JarFile` which
throws an IOException if it cannot load the jar.

However, the URLs are URL encoded yet `JarFile` does not expect them to be URL encoded. This means
that characters such as a space are translated to %20 but `JarFile` treats the encoded form
literally resulting in an exception.

Unfortunately, the problem is exacerbated as the exception is then swallowed meaning that jars that
are not resource jars and jars that cannot be loaded are treated equivalently. As a result, these
failures are silent.

# Proposal

Use `UrlDecoder.decode` on `URL.getFile` before it is used to construct a JarFile.

Add a WARN log line to the `isResourcesJar` methods if an IOException occurs so it is easier to
detect similar issues in the future. These methods are at
`org.springframework.boot.web.servlet.server.StaticResourceJars` in 2.0.0.RC1 and 
`org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory` in
1.5.10.RELEASE.

I will work on a PR against the 1.5.x and master branches so please let me know if you are not
happy with this proposal.