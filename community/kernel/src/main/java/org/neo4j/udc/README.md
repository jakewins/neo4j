This package contains in-memory usage data collection, it does not contain the publishing code. 
 
The publishing code still lives in the 'udc' maven module, pending move here. 
It is not yet moved here as it requires a careful incision in the build system to ensure all flags are set correctly 
depending on how the artifacts are produced and published, and this is deemed better done in its own commit.