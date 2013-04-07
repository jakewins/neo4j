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
            $http.get("/db/data/index/node")
              .success (idx) =>
                @nodeIndexes[name] = idx[name]
                @_triggerChangedEvent()

      newRelationshipIndex : (name) ->
        $http.post("/db/data/index/relationship", {'name':name})
          .success =>
            $http.get("/db/data/index/relationship")
              .success (idx) =>
                @relationshipIndexes[name] = idx[name]
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
        @nodeIndexes         = if indexes then indexes else {}
        @_triggerChangedEvent()

      _updateRelationshipIndexes : (indexes) =>
        @relationshipIndexes = if indexes then indexes else {}
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
        @indexes = []

        $http.get("/db/data/schema/index")
          .success (idx) =>
            @indexes = idx
            @_triggerChangedEvent()

      createIndex : (label, property, cb=(->)) ->
        $http.post("/db/data/transaction/commit", [{
          "statement" : "CREATE INDEX ON :#{label}(#{property})"
          }]).success (r) =>
            if r.errors.length > 0
              cb(r.errors)
            else
              @_addIndexLocally(label, property)
              @_triggerChangedEvent()

      dropIndex : (label, property, cb=(->)) ->
        $http.post("/db/data/transaction/commit", [{
          "statement" : "DROP INDEX :#{label}(#{property})"
          }]).success (r) =>
            if r.errors.length > 0
              cb(r.errors)
            else
              @_removeIndexLocally(label, property)
              @_triggerChangedEvent()
            
      _removeIndexLocally : (label, prop) ->
        @indexes = _(@indexes).reject (i) ->
          i.label ==label && i['property-keys'].length == 1
            
      _addIndexLocally : (label, prop) ->
        @indexes.push({
          "label" : label,
          "property-keys" : [prop]
        })

      _triggerChangedEvent : ->
        $rootScope.$broadcast "indexService.changed"

])
