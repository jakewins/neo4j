'use strict'

angular.module('app.controllers.data.browser', [])

.controller('DatabrowserController', [
  '$scope'
  'graphService'

  ($scope, graphService) ->
    
    $scope.query    = graphService.query
    
    # Auto-update the displayed rows if the graph service changes
    synchronizeWithGraphData = ->
      $scope.rows     = graphService.rows
      $scope.columns  = graphService.columns
      $scope.error    = graphService.error
    
    $scope.graphService = graphService
    $scope.$watch 'graphService.rows', synchronizeWithGraphData
    synchronizeWithGraphData()
    
    $scope.execute = ->
      graphService.executeQuery $scope.query
    
    
])