'use strict'

# Declare app level module which depends on filters, and services
App = angular.module('app', [
  'ui'
  'ui.bootstrap'
  'ngCookies'
  'ngResource'
  'app.controllers'
  'app.directives'
  'app.filters'
  'app.services'
])

App.config([
  '$routeProvider'
  '$locationProvider'
  '$httpProvider'

($routeProvider, $locationProvider, $httpProvider, config) ->

  goTo = (tmpl, ctrl) -> ({
    templateUrl : "partials/#{tmpl}.html"
    controller  : ctrl
  })

  $routeProvider

    .when('/',             goTo "splash",       "SplashController")
    .when('/data/browser', goTo "data/browser", "DatabrowserController")
    .when('/data/console', goTo "data/console", "ConsoleController")

    .when('/schema/indexes',        goTo "schema/indexes",
                                         "IndexController")
    .when('/schema/legacy-indexes', goTo "schema/legacyIndexes",
                                          "LegacyIndexController")

    .when('/system/jmx',   goTo "system/jmx", "JmxController")

    # Catch all
    .otherwise({redirectTo: '/'})

  # Without server side support html5 must be disabled.
  $locationProvider.html5Mode(false)
  
  # Common HTTP headers for the networking layer
  $httpProvider.defaults.headers.common['X-stream'] = true
  $httpProvider.defaults.headers.common['Content-Type'] = 'application/json'

  $httpProvider.defaults.transformRequest.push (payload, headers) ->
    headers()['Content-Type'] = 'application/json'
    console.log headers()
    payload

  for a in $httpProvider.defaults.transformRequest
    console.log a.toString()
])
