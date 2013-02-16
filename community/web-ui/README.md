# Neo4j Web UI

This is the main repository of code relating to UI for Neo4j.

The Web UI is a single-page coffeescript application that handles basic management and monitoring of Neo4j. It uses [AngularJS](http://angularjs.org/) as its application framework.

## Project layout

### Source code

* [app/](app) - The application's main source code 
* [test/](test) - The accompanying unit and integration tests
* [vendor/](vendor) - Runtime dependencies

### Project infrastructure

* [scripts/](scripts) - Helper scripts (run build, run tests, etc)
* [neo4j-home/](neo4j-home) - Home directory for Neo4j during development

## Building

The project uses a combination of [Brunch](http://brunch.io/) and Maven as it's build chain. It uses [Testacular](http://vojtajina.github.com/testacular/) as it's test runner, with the actual tests written with the [Jasmine](http://pivotal.github.com/jasmine/) test framework.

### Prerequisites

You will need to install Node.js in whatever way is suitable for your platform. Afterwards, you should install brunch

    npm install -g brunch
    
And install the project dependencies

    npm install

From the project base dir. For running tests, you also need [PhantomJS](http://phantomjs.org/) installed, it provides the headless browser environment where the tests are executed.

### Running the build
    
    brunch build
    
### Running tests

You can have the tests run automatically on every change, much like the compile cycle from brunch autocompiler.

Do that with

    # Unit tests
    scripts/test.sh
    
    # Integration tests
    scripts/test-e2e.sh
    
from the source base directory. Use the .bat script instead if you are on Windows.
    
## Developing

There are two basic steps to it.

1: Start the brunch autocompiler with

    scripts/development.sh 
  
in the source base directory

2: Start Neo4j

    mvn exec:java
    
again, in the source base directory
