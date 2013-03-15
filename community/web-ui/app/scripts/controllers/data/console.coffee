'use strict'

angular.module('app.controllers.data.console', [])

.controller('ConsoleController', [
  '$scope'
  '$rootScope'
  'consoleService'

  ($scope, $rootScope, consoleService) ->
    
    # Store the current console in the global state, so we don't loose it
    # when the user switches between views
    $rootScope.currentConsoleEngine ?= "shell"
    
    synchronizeWithConsoleService = ->
      $scope.availableEngines = consoleService.engines
      if consoleService.engines[$scope.currentConsoleEngine]?
        engineState = consoleService.engines[$scope.currentConsoleEngine]
        $scope.interactions = engineState.interactions
        $scope.engineName = engineState.name
      else
        $scope.interactions = []
        $scope.engineName = $scope.currentConsoleEngine
    
    $scope.$on('consoleService.changed', synchronizeWithConsoleService)
    synchronizeWithConsoleService()
    
    $scope.changeEngine = (engine)->
      $rootScope.currentConsoleEngine = engine
      synchronizeWithConsoleService()
      
    
    $scope.prevHistory = ->
      $scope.statement = "Prev"
      
    $scope.nextHistory = ->
      $scope.statement = "Next"
    
    $scope.execute = ->
      consoleService.execute $scope.statement, $scope.currentConsoleEngine
      $scope.statement = ""
    
])