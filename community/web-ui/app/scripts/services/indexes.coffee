'use strict'

### A service that manages a common view of the graph for the entire app ###

angular.module('app.services.indexes', [])

.factory('legacyIndexService', [
  '$http'
  '$rootScope'
  ($http, $rootScope)->
    new class LegacyIndexService
    
      constructor : () ->
        @nodeIndexes = {}
        @relationshipIndexes = {}

        $http.get("/db/data/index/node")
          .success @_updateNodeIndexes

        $http.get("/db/data/index/relationship")
          .success @_updateRelationshipIndexes

      newNodeIndex : (name) ->
        $http.post("/db/data/index/node", {'name':name})
          .success =>
            $http.get("/db/data/index/node/#{name}")
              .success (idx) =>
                @nodeIndexes[name] = idx
                @_triggerChangedEvent()

      newRelationshipIndex : (name) ->
        $http.post("/db/data/index/relationship", {'name':name})
          .success =>
            $http.get("/db/data/index/relationship/#{name}")
              .success (idx) =>
                @relationshipIndexes[name] = idx
                @_triggerChangedEvent()

      deleteNodeIndex : (name) ->
        $http.delete("/db/data/index/node/#{name}")
          .success =>
            delete @nodeIndexes[name]
            @_triggerChangedEvent()

      deleteRelationshipIndex : (name) ->
        $http.delete("/db/data/index/relationship/#{name}")
          .success =>
            delete @relationshipIndexes[name]
            @_triggerChangedEvent()

      _updateNodeIndexes : (indexes) =>
        @nodeIndexes = indexes
        @_triggerChangedEvent()

      _updateRelationshipIndexes : (indexes) =>
        @relationshipIndexes = indexes
        @_triggerChangedEvent()

      _triggerChangedEvent : ->
        $rootScope.$broadcast "legacyIndexService.changed"
  
])

.factory('indexService', [
  '$http'
  '$rootScope'
  ($http, $rootScope)->
    new class IndexService
    
      constructor : () ->
  
])
