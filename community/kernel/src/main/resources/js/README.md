# Neo4j Kernel JS

This contains the traceur es6-compiler and associated runtime. It also contains a small shim for us to invoke the 
compiler and handle errors on the java side. 

This is used to allow modern javascript syntax while still running on the spiffy Nashorn platform.

We chose to use Traceur over the more popular BabelJS because BabelJS cannot be loaded into Nashorn directly since it
depends heavily on NodeJS APIs, and the browser version of BabelJS contains multi-megabyte method bodies, which the
JVM cannot handle. 

Once Nashorn adds support for modern JavaScript, we should be able to eject this compiler.
