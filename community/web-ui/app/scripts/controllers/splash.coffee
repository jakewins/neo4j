'use strict'

angular.module('app.controllers.splash', [])

.controller('SplashController', [
  '$scope',
  '$location'
  'graphService'
  
  ($scope, $location, graphService) ->
    
    $scope.query = '
// Try me!\n
CREATE myNode { name : "My First Node" }\n
RETURN myNode'
    
    $scope.execute = () ->
      graphService.executeQuery $scope.query
      $location.path '/data/browser'
  
])