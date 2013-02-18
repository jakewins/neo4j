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
* [public/](public) - The compiled HTML/JS/CSS and image resources
* [neo4j-home/](neo4j-home) - Home directory for Neo4j during development

## Building

The project uses a combination of [Brunch](http://brunch.io/) and Maven as it's build chain. It uses [Testacular](http://vojtajina.github.com/testacular/) as it's test runner, with the actual tests written with the [Jasmine](http://pivotal.github.com/jasmine/) test framework.

### Prerequisites

You will need to install Node.js in whatever way is suitable for your platform. Afterwards, you should install brunch

    sudo npm install -g brunch
    
And install the project dependencies

    npm install

From the project base dir. For running tests, you also need [PhantomJS](http://phantomjs.org/) installed, it provides the headless browser environment where the tests are executed. To do that, look at [PhantomJS.org](http://phantomjs.org/)

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

    scripts/development.sh
    
Then go to http://localhost:7474/
