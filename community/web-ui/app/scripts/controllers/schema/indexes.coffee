'use strict'

angular.module('app.controllers.schema.indexes', [
  'app.services.indexes'])


.controller('IndexController', [
  '$scope'
  '$rootScope'
  'indexService'

  ($scope, $rootScope, indexService) ->

    refreshIndexes = ->
      $scope.indexes = idx = indexService.indexes
      if _(idx).any( (i) -> i.state == "POPULATING" )
        setTimeout(indexService.refresh, 1000)

    $scope.showCreateIndex = ->
      $scope.newIndexLabel = ''
      $scope.newIndexProperty = ''
      $scope.showCreateModal = true

    $scope.newIndex = ->
      indexService.createIndex $scope.newIndexLabel, $scope.newIndexProperty
      $scope.showCreateModal = false

    $scope.promptDropIndex = (idx) ->
      $scope.indexToDrop = idx
      $scope.showIndexDropWarning = true

    $scope.dropIndex = (idx) ->
      indexService.dropIndex idx.label, idx.propertyKeys[0]
      $scope.showIndexDropWarning = false

    $scope.$on "indexService.changed", refreshIndexes
    refreshIndexes()
    indexService.refresh()
])

.controller('LegacyIndexController', [
  '$scope'
  '$rootScope'
  'legacyIndexService'

  ($scope, $rootScope, legacyIndexService) ->
    
    $scope.newIndexType = 'node'

    refreshIndexes = ->
      $scope.nodeIndexes         = legacyIndexService.nodeIndexes
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