'use strict'

angular.module('app.controllers.schema.indexes', [
  'app.services.indexes'])


.controller('IndexController', [
  '$scope'
  '$rootScope'
  'indexService'

  ($scope, $rootScope, indexService) ->


])

.controller('LegacyIndexController', [
  '$scope'
  '$rootScope'
  'legacyIndexService'

  ($scope, $rootScope, legacyIndexService) ->
    
    $scope.newIndexType = 'node'

    refreshIndexes = ->
      $scope.nodeIndexes = legacyIndexService.nodeIndexes
      $scope.relationshipIndexes = legacyIndexService.relationshipIndexes

    $scope.newIndex = ->
      if $scope.newIndexType is 'node'
        legacyIndexService.newNodeIndex($scope.newIndexName)
      else
        legacyIndexService.newRelationshipIndex($scope.newIndexName)
      $scope.showCreateModal = false

    $scope.showCreateNodeIndex = ->
      $scope.newIndexName = ''
      $scope.newIndexType = 'node'
      $scope.showCreateModal = true

    $scope.showCreateRelationshipIndex = ->
      $scope.newIndexName = ''
      $scope.newIndexType = 'relationship'
      $scope.showCreateModal = true

    $scope.dropIndex = (type, name) ->
      $scope.showIndexDropWarning = false
      if type is 'node'
        legacyIndexService.deleteNodeIndex(name)
      else
        legacyIndexService.deleteRelationshipIndex(name)

    $scope.promptDropRelationshipIndex = (indexName) ->
      $scope.toDropName = indexName
      $scope.toDropType = "relationship"
      $scope.showIndexDropWarning = true

    $scope.promptDropNodeIndex = (indexName) ->
      $scope.toDropName = indexName
      $scope.toDropType = "node"
      $scope.showIndexDropWarning = true

    $scope.$on "legacyIndexService.changed", refreshIndexes
    refreshIndexes()
    
])