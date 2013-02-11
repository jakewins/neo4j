'use strict'

# Declare app level module which depends on filters, and services
App = angular.module('app', [
  'ui'
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

($routeProvider, $locationProvider, config) ->

  $routeProvider

    .when('/', {
      templateUrl: 'partials/splash.html',
      controller:  'SplashController'})
    .when('/data/browser', {
      templateUrl: 'partials/data/browser.html',
      controller:  'DatabrowserController'})
    .when('/data/console', {
      templateUrl: 'partials/data/console.html',
      controller:  'ConsoleController'})

    # Catch all
    .otherwise({redirectTo: '/'})

  # Without server side support html5 must be disabled.
  $locationProvider.html5Mode(false)
])
