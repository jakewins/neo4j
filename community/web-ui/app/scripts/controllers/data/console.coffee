'use strict'

angular.module('app.controllers.data.console', [])

.controller('ConsoleController', [
  '$scope'
  'consoleService'

  ($scope, consoleService) ->
    
    $scope.engine = "shell" # Default
    
    synchronizeWithConsoleService = ->
      $scope.availableEngines = consoleService.engines
      if consoleService.engines[$scope.engine]?
        engineState = consoleService.engines[$scope.engine]
        $scope.interactions = engineState.interactions
        $scope.engineName = engineState.name
      else
        $scope.interactions = []
        $scope.engineName = $scope.engine
    
    $scope.$on('consoleService.changed', synchronizeWithConsoleService)
    synchronizeWithConsoleService()
    
    $scope.changeEngine = (engine)->
      $scope.engine = engine
      synchronizeWithConsoleService()
    
    $scope.execute = ->
      consoleService.execute $scope.statement, $scope.engine
      $scope.statement = ""
    
])