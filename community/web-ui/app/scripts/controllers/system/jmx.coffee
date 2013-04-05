'use strict'

angular.module('app.controllers.system.jmx', [
  'app.services.jmx'])

.controller('JmxController', [
  '$scope'
  '$rootScope'
  'jmxService'

  ($scope, $rootScope, jmxService) ->
    
    $scope.domains = jmxService.domains
    $scope.beans   = []
    $scope.domain  = null
    $scope.bean    = null

    $scope.setDomain = (domain) ->
      if $scope.domains[domain]?
        $scope.beans = $scope.domains[domain]
        $scope.domain = $rootScope.currentJmxDomain = domain
        $scope.bean = null

    $scope.setBean = (bean) ->
      $scope.bean = $rootScope.currentJmxBean = bean

      $scope.simpleAttributes = simple = []
      $scope.complexAttributes = complex = []
      for attr in bean.attributes
        if attr.type in ["java.lang.String","boolean", "long",
                         "int", "float", "double"]
          simple.push attr
        else complex.push attr

      # Alphabetical order
      simple.sort (a,b) -> if a.name < b.name then -1 else 1
      complex.sort (a,b) -> if a.name < b.name then -1 else 1


    $scope.$on 'jmxService.changed', (ev, args) ->
      $scope.domains = args[0]

      # Select kernel as default bean if none is selected.
      if $scope.domain == null
        $scope.setDomain "org.neo4j"
      if $scope.bean == null
        for b in $scope.domains["org.neo4j"]
          if b.name.indexOf('Kernel') != -1
            $scope.setBean b
            break

    # Restore any application global state
    if $rootScope.currentJmxDomain?
      $scope.setDomain $rootScope.currentJmxDomain
    else $scope.setDomain "org.neo4j"
    if $rootScope.currentJmxBean?
      $scope.setBean $rootScope.currentJmxBean
])