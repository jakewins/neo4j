'use strict';

var App;

App = angular.module('app', ['ui', 'ngCookies', 'ngResource', 'app.controllers', 'app.directives', 'app.filters', 'app.services']);

App.config([
  '$routeProvider', '$locationProvider', '$httpProvider', function($routeProvider, $locationProvider, $httpProvider, config) {
    $routeProvider.when('/', {
      templateUrl: 'partials/splash.html',
      controller: 'SplashController'
    }).when('/data/browser', {
      templateUrl: 'partials/data/browser.html',
      controller: 'DatabrowserController'
    }).when('/data/console', {
      templateUrl: 'partials/data/console.html',
      controller: 'ConsoleController'
    }).otherwise({
      redirectTo: '/'
    });
    $locationProvider.html5Mode(false);
    return $httpProvider.defaults.headers.common['X-stream'] = true;
  }
]);
'use strict';

/* Controllers
*/

angular.module('app.controllers', ['app.controllers.sidebar', 'app.controllers.data.browser', 'app.controllers.data.console', 'app.controllers.splash']).controller('AppCtrl', ['$scope', '$location', '$resource', '$rootScope', function($scope, $location, $resource, $rootScope) {}]);
'use strict';

angular.module('app.controllers.data.browser', ['app.services.graph', 'app.services.paginator']).controller('DatabrowserController', [
  '$scope', 'graphService', 'paginatorService', function($scope, graphService, paginatorService) {
    var PAGE_SIZE, synchronizeWithGraphData;
    PAGE_SIZE = 20;
    $scope.query = graphService.query;
    $scope.page = 1;
    $scope.execute = function() {
      return graphService.executeQuery($scope.query);
    };
    $scope.updatePagination = function(page) {
      var buttons, end, numberOfPages, numberOfRows, start;
      if (page === '_PREV') {
        page = $scope.page - 1;
      }
      if (page === '_NEXT') {
        page = $scope.page + 1;
      }
      numberOfRows = $scope.allRows.length;
      numberOfPages = Math.ceil(numberOfRows / PAGE_SIZE);
      buttons = paginatorService.calculateNiceButtons(page, numberOfPages);
      start = PAGE_SIZE * (page - 1);
      end = start + PAGE_SIZE;
      end = end < numberOfRows ? end : numberOfRows;
      $scope.page = page;
      $scope.numberOfPages = numberOfPages;
      $scope.pageButtons = buttons;
      return $scope.rows = $scope.allRows.slice(start, end);
    };
    synchronizeWithGraphData = function() {
      $scope.allRows = graphService.rows;
      $scope.columns = graphService.columns;
      $scope.error = graphService.error;
      $scope.isLoading = graphService.isLoading;
      return $scope.updatePagination(1);
    };
    $scope.graphService = graphService;
    $scope.$on('graphService.changed', synchronizeWithGraphData);
    return synchronizeWithGraphData();
  }
]);
'use strict';

angular.module('app.controllers.data.console', []).controller('ConsoleController', [
  '$scope', '$rootScope', 'consoleService', function($scope, $rootScope, consoleService) {
    var synchronizeWithConsoleService, _ref;
    if ((_ref = $rootScope.currentConsoleEngine) == null) {
      $rootScope.currentConsoleEngine = "shell";
    }
    synchronizeWithConsoleService = function() {
      var engineState;
      $scope.availableEngines = consoleService.engines;
      if (consoleService.engines[$scope.currentConsoleEngine] != null) {
        engineState = consoleService.engines[$scope.currentConsoleEngine];
        $scope.interactions = engineState.interactions;
        return $scope.engineName = engineState.name;
      } else {
        $scope.interactions = [];
        return $scope.engineName = $scope.currentConsoleEngine;
      }
    };
    $scope.$on('consoleService.changed', synchronizeWithConsoleService);
    synchronizeWithConsoleService();
    $scope.changeEngine = function(engine) {
      $rootScope.currentConsoleEngine = engine;
      return synchronizeWithConsoleService();
    };
    $scope.prevHistory = function() {
      return $scope.statement = "Prev";
    };
    $scope.nextHistory = function() {
      return $scope.statement = "Next";
    };
    return $scope.execute = function() {
      consoleService.execute($scope.statement, $scope.currentConsoleEngine);
      return $scope.statement = "";
    };
  }
]);
'use strict';

angular.module('app.controllers.sidebar', []).controller('SidebarController', [
  '$scope', '$location', 'consoleService', function($scope, $location, consoleService) {
    $scope.menuItems = [
      {
        title: 'Data',
        icon: 'heart',
        active: false,
        items: [
          {
            href: '#/data/browser',
            title: 'Query Tool',
            icon: 'list',
            active: false
          }, {
            href: '#/data/visualizer',
            title: 'Explorer',
            icon: 'map-marker',
            active: false
          }, {
            title: 'Shell',
            icon: 'list-alt',
            active: false,
            href: '#/data/console'
          }
        ]
      }, {
        title: 'Schema',
        active: false,
        items: [
          {
            href: '#/schema/indexes',
            title: 'Indexes',
            icon: 'filter',
            active: false
          }, {
            href: '#/schema/legacy-indexes',
            title: 'Legacy indexes',
            icon: 'filter',
            active: false
          }
        ]
      }, {
        title: 'System',
        active: false,
        items: [
          {
            href: '#/schema/indexes',
            title: 'JMX Browser',
            icon: 'cog',
            active: false
          }
        ]
      }
    ];
    $scope.$location = $location;
    return $scope.$watch('$location.path()', function(path) {
      var setActive;
      path = "#" + (path || '/');
      setActive = function(items) {
        var item, _i, _len, _results;
        _results = [];
        for (_i = 0, _len = items.length; _i < _len; _i++) {
          item = items[_i];
          item.active = item.href === path;
          if (item.items != null) {
            _results.push(setActive(item.items));
          } else {
            _results.push(void 0);
          }
        }
        return _results;
      };
      return setActive($scope.menuItems);
    });
  }
]);
'use strict';

angular.module('app.controllers.splash', []).controller('SplashController', [
  '$scope', '$location', 'graphService', function($scope, $location, graphService) {
    $scope.query = '\
// Try me!\n\
CREATE myNode { name : "My First Node" }\n\
RETURN myNode';
    return $scope.execute = function() {
      graphService.executeQuery($scope.query);
      return $location.path('/data/browser');
    };
  }
]);
'use strict';

/* Directives
*/

angular.module('app.directives', ['app.services']).directive('appVersion', [
  'version', 'edition', function(version, edition) {
    return function(scope, elm, attrs) {
      return elm.text("" + version + " " + edition + " Edition");
    };
  }
]).directive('scrollHereOnChange', function() {
  return function(scope, element, attrs) {
    return scope.$watch(attrs.scrollHereOnChange, function() {
      return setTimeout((function() {
        if (window.innerHeight < (element[0].offsetTop + 50)) {
          return window.scrollTo(0, element[0].offsetTop);
        }
      }), 0);
    });
  };
});
'use strict';

/* Filters
*/

angular.module('app.filters', []).filter('interpolate', [
  'version', function(version) {
    return function(text) {
      return String(text).replace(/\%VERSION\%/mg, version);
    };
  }
]);
'use strict';

/* Sevices
*/

angular.module('app.services', ['app.services.console']).factory('version', function() {
  return "2.0".factory('edition', function() {
    return "Community";
  });
});
'use strict';

var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };

angular.module('app.services.console', []).factory('consoleService', [
  '$http', '$rootScope', function($http, $rootScope) {
    var ConsoleService, HttpEngine, RemoteEngine;
    HttpEngine = (function() {

      HttpEngine.prototype.statementRegex = /^((GET)|(PUT)|(POST)|(DELETE)) ([^ ]+)( (.+))?$/i;

      function HttpEngine(_engineKey, _out) {
        this._engineKey = _engineKey;
        this._out = _out;
        this._out(this._engineKey, "", ["Welcome to the REST Shell!", "Usage: <VERB> <PATH> [JSON]", 'Eg: GET /db/data or POST /db/data/node {"name":"My Node"}']);
      }

      HttpEngine.prototype.execute = function(statement, out) {
        var data, method, result, url, _ref;
        if (this.statementRegex.test(statement)) {
          result = this.statementRegex.exec(statement);
          _ref = [result[1], result[6], result[8]], method = _ref[0], url = _ref[1], data = _ref[2];
          if (data != null) {
            try {
              data = JSON.parse(data);
            } catch (e) {
              this._out(this._engineKey, statement, ["Invalid JSON payload."]);
              return;
            }
          }
          return $http({
            method: method,
            url: url,
            data: data
          }).success(this._onResponse(statement)).error(this._onResponse(statement));
        } else {
          return this._out(this._engineKey, statement, ["Invalid syntax, syntax is: <VERB> <URI> <JSON DATA>"]);
        }
      };

      HttpEngine.prototype._onResponse = function(statement) {
        var _this = this;
        return function(payload, status, meta) {
          if (typeof payload === 'object') {
            payload = JSON.stringify(payload, null, "  ");
          }
          return _this._out(_this._engineKey, statement, ["" + status, payload]);
        };
      };

      return HttpEngine;

    })();
    RemoteEngine = (function() {

      function RemoteEngine(_engineKey, _out) {
        this._engineKey = _engineKey;
        this._out = _out;
        this._onInitFailed = __bind(this._onInitFailed, this);

        this._sendStatement("init()").error(this._onInitFailed);
      }

      RemoteEngine.prototype.execute = function(statement) {
        return this._sendStatement(statement).success(this._onStatementExecuted(statement)).error(this._onStatementFailed(statement));
      };

      RemoteEngine.prototype._sendStatement = function(statement) {
        return $http.post('/db/manage/server/console', {
          command: statement,
          engine: this._engineKey
        });
      };

      RemoteEngine.prototype._onStatementExecuted = function(statement) {
        var _this = this;
        return function(result) {
          var lines, prompt;
          lines = result[0], prompt = result[1];
          return _this._out(_this._engineKey, statement, lines.split('\n'));
        };
      };

      RemoteEngine.prototype._onStatementFailed = function(statement) {
        var _this = this;
        return function(error) {
          return _this._out(_this._engineKey, statement, ["Unable to execute statement, please see the server logs."]);
        };
      };

      RemoteEngine.prototype._onInitFailed = function(error) {
        var init_error_msg;
        init_error_msg = "The server failed to initialize this shell. It responded with:";
        return this._out(this._engineKey, "", [init_error_msg, error]);
      };

      return RemoteEngine;

    })();
    ConsoleService = (function() {

      function ConsoleService() {
        this._onInitializingRemoteEnginesFailed = __bind(this._onInitializingRemoteEnginesFailed, this);

        this._appendInteraction = __bind(this._appendInteraction, this);
        this.engines = {};
        this._initializeEngines();
      }

      ConsoleService.prototype.execute = function(statement, engine) {
        if (this.engines[engine] != null) {
          return this.engines[engine].engine.execute(statement);
        } else {
          throw new Exception("Unknown shell engine " + engine + ".");
        }
      };

      ConsoleService.prototype._appendInteraction = function(engine, statement, result) {
        if (!(this.engines[engine] != null)) {
          this._defineEngine(engine, engine, null);
        }
        result = result.join('\n');
        this.engines[engine].interactions.push({
          statement: statement,
          result: result
        });
        return $rootScope.$broadcast('consoleService.changed', [engine, statement, result]);
      };

      ConsoleService.prototype._initializeEngines = function() {
        var _this = this;
        this._defineEngine('http', 'REST Shell', new HttpEngine('http', this._appendInteraction));
        return $http.get('/db/manage/server/console').success(function(response) {
          var engine, _i, _len, _ref;
          _ref = response.engines;
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            engine = _ref[_i];
            _this._initRemoteEngine(engine);
          }
          return $rootScope.$broadcast('consoleService.changed');
        }).error(this._onInitializingRemoteEnginesFailed);
      };

      ConsoleService.prototype._onInitializingRemoteEnginesFailed = function(err) {};

      ConsoleService.prototype._initRemoteEngine = function(key) {
        var engine;
        engine = new RemoteEngine(key, this._appendInteraction);
        return this._defineEngine(key, this._humanReadableEngineName(key), engine);
      };

      ConsoleService.prototype._defineEngine = function(key, name, engine) {
        var def, _base, _ref, _ref1;
        def = (_ref = (_base = this.engines)[key]) != null ? _ref : _base[key] = {};
        def.key = key;
        def.name = name;
        if ((_ref1 = def.interactions) == null) {
          def.interactions = [];
        }
        return def.engine = engine;
      };

      ConsoleService.prototype._humanReadableEngineName = function(engineKey) {
        var knownEngines;
        knownEngines = {
          'shell': "Neo4j Shell",
          'gremlin': "Gremlin"
        };
        if (knownEngines[engineKey] != null) {
          return knownEngines[engineKey];
        } else {
          return engineKey;
        }
      };

      return ConsoleService;

    })();
    return new ConsoleService;
  }
]);
'use strict';

/* A service that manages a common view of the graph for the entire app
*/

var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };

angular.module('app.services.graph', []).factory('graphService', [
  '$http', '$rootScope', function($http, $rootScope) {
    var GraphService;
    GraphService = (function() {

      function GraphService() {
        this._onFailedExecution = __bind(this._onFailedExecution, this);

        this._onSuccessfulExecution = __bind(this._onSuccessfulExecution, this);
        this._clear();
        this.query = "// Enter query ";
      }

      GraphService.prototype.executeQuery = function(query) {
        this._clear();
        this.query = query;
        this.isLoading = true;
        this._broadcastChange();
        return $http.post("/db/data/cypher", {
          query: query
        }).success(this._onSuccessfulExecution).error(this._onFailedExecution);
      };

      GraphService.prototype._onSuccessfulExecution = function(result) {
        this._clear();
        this.rows = result.data.map(this._cleanResultRow);
        this.columns = result.columns;
        return this._broadcastChange();
      };

      GraphService.prototype._onFailedExecution = function(error) {
        this._clear();
        this.error = error;
        return this._broadcastChange();
      };

      GraphService.prototype._broadcastChange = function() {
        return $rootScope.$broadcast('graphService.changed', [this]);
      };

      GraphService.prototype._cleanResultRow = function(row) {
        var cell, _i, _len, _results;
        _results = [];
        for (_i = 0, _len = row.length; _i < _len; _i++) {
          cell = row[_i];
          if (!(cell != null)) {
            _results.push(null);
          } else if (cell.self != null) {
            _results.push(cell.data);
          } else {
            _results.push(cell);
          }
        }
        return _results;
      };

      GraphService.prototype._clear = function() {
        this.rows = [];
        this.columns = [];
        this.error = null;
        return this.isLoading = false;
      };

      return GraphService;

    })();
    return new GraphService;
  }
]);
'use strict';

/* A service that manages a common view of the graph for the entire app
*/

angular.module('app.services.paginator', []).factory('paginatorService', [
  function() {
    var PaginatorService;
    PaginatorService = (function() {

      function PaginatorService() {}

      PaginatorService.prototype.calculateNiceButtons = function(currentPage, numberOfPages) {
        var buttons;
        buttons = [
          {
            text: '«',
            action: '_PREV',
            disabled: currentPage === 1
          }, {
            text: '»',
            action: '_NEXT',
            disabled: currentPage === numberOfPages
          }
        ];
        return buttons;
      };

      return PaginatorService;

    })();
    return new PaginatorService;
  }
]);
