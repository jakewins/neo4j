'use strict'

angular.module('app.controllers.system.jmx', [
  'app.services.jmx'])

.controller('JmxController', [
  '$scope'
  'jmxService'

  ($scope, jmxService) ->
    
    $scope.domains = ["org.neo4j", "java.lang", "java.util.logging"]
    $scope.beans   = [{ instance:"kernel#0",name:"Kernel" }]
    $scope.bean    = {
        name : "Kernel",
        attributes : [{ name:"ReadOnly", value : false, description : "Hello" }]
    }
])