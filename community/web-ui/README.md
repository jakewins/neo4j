# Neo4j Web UI

This is the main repository of code relating to UI for Neo4j.

It's meant to be bundled with the Neo4j Server, as the webadmin component.

## Project layout

## Building

### Prerequisites

You will need to install Node.js in whatever way is suitable for your platform. Afterwards, you should use

    npm install -g brunch
    
That's it.

### Running the build
    
    brunch build
    
### Running tests

    brunch test
    
## Developing

There are two basic steps to it.

1. Start the brunch autocompiler with

    brunch watch
  
in the source base directory

2. Start Neo4j

    mvn exec:java
    
again, in the source base directory

## Starting the automatic test runner

You can have the tests run automatically on every change, much like the compile cycle from brunch autocompiler.
Do that with

    scripts/test.sh
    
from the source base directory. Use the .bat script instead if you are on Windows.

