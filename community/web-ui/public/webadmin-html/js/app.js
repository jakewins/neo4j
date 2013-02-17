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
    $scope.$watch('graphService.rows', synchronizeWithGraphData);
    return synchronizeWithGraphData();
  }
]);
'use strict';

angular.module('app.controllers.data.console', []).controller('ConsoleController', [
  '$scope', 'consoleService', function($scope, consoleService) {
    var synchronizeWithConsoleService;
    $scope.interations = consoleService.interactions;
    synchronizeWithConsoleService = function() {
      return $scope.interactions = consoleService.interactions;
    };
    $scope.consoleService = consoleService;
    $scope.$watch('consoleService.interactions.length', synchronizeWithConsoleService);
    synchronizeWithConsoleService();
    return $scope.execute = function() {
      consoleService.execute($scope.statement);
      return $scope.statement = "";
    };
  }
]);
'use strict';

angular.module('app.controllers.sidebar', []).controller('SidebarController', [
  '$scope', '$location', function($scope, $location) {
    $scope.menuItems = [
      {
        href: '#/data/browser',
        title: 'Data',
        icon: 'th',
        active: true
      }, {
        href: '#/data/console',
        title: 'Console',
        icon: 'list-alt',
        active: false
      }
    ];
    $scope.$location = $location;
    return $scope.$watch('$location.path()', function(path) {
      var item, _i, _len, _ref, _results;
      path = "#" + (path || '/');
      _ref = $scope.menuItems;
      _results = [];
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        item = _ref[_i];
        _results.push(item.active = item.href === path);
      }
      return _results;
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
]);
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

angular.module('app.services.console', []).factory('consoleService', [
  '$http', function($http) {
    var ConsoleService;
    ConsoleService = (function() {

      function ConsoleService() {
        this.interactions = [];
        this._sendStatement("init()");
      }

      ConsoleService.prototype.execute = function(statement) {
        return this._sendStatement(statement).success(this._onStatementExecuted(statement)).error(this._onStatementFailed(statement));
      };

      ConsoleService.prototype._onStatementExecuted = function(statement) {
        var _this = this;
        return function(result) {
          var lines, prompt;
          lines = result[0], prompt = result[1];
          _this.interactions.push({
            statement: statement,
            result: lines
          });
          return console.log(_this.interactions);
        };
      };

      ConsoleService.prototype._onStatementFailed = function(statement) {
        var _this = this;
        return function(error) {
          return console.log(error);
        };
      };

      ConsoleService.prototype._sendStatement = function(statement) {
        return $http.post('/db/manage/server/console', {
          command: statement,
          engine: "SHELL"
        });
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
  '$http', function($http) {
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
        return $http.post("/db/data/cypher", {
          query: query
        }).success(this._onSuccessfulExecution).error(this._onFailedExecution);
      };

      GraphService.prototype._onSuccessfulExecution = function(result) {
        this._clear();
        this.rows = result.data.map(this._cleanResultRow);
        return this.columns = result.columns;
      };

      GraphService.prototype._onFailedExecution = function(error) {
        this._clear();
        return this.error = error;
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
