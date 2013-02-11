'use strict'

angular.module('app.controllers.data.console', [])

.controller('ConsoleController', [
  '$scope'
  'consoleService'

  ($scope, consoleService) ->
    
    $scope.interations = consoleService.interactions
    
    synchronizeWithConsoleService = ->
      $scope.interactions = consoleService.interactions
    
    $scope.consoleService = consoleService
    $scope.$watch('consoleService.interactions.length',
                  synchronizeWithConsoleService)
    synchronizeWithConsoleService()
    
    $scope.execute = ->
      consoleService.execute $scope.statement
      $scope.statement = ""
    
])