'use strict'

angular.module('app.controllers.system.jmx', [
  'app.services.jmx'])

.controller('JmxController', [
  '$scope'
  'jmxService'

  ($scope, jmxService) ->
    
    $scope.domains = ["org.neo4j", "java.lang", "java.util.logging"]
    
    $scope.beans = []
])