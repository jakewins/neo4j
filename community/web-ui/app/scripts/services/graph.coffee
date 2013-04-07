'use strict'

###
A stateful service for executing statements and reading results,
used by the query tool.
###

angular.module('app.services.graph', [])

.factory('graphService', [
  '$http'
  '$rootScope'
  ($http, $rootScope)->
    class GraphService
    
      constructor : () ->
        @_clear()
        @query = "// Enter query "
    
      executeQuery : (query) ->
        @_clear()
        @query     = query
        @isLoading = true
        
        @_broadcastChange()
        $http.post("/db/data/cypher", { query : query })
          .success(@_onSuccessfulExecution)
          .error(  @_onFailedExecution)
      
      _onSuccessfulExecution : (result) =>
        @_clear()
        @rows    = result.data.map @_cleanResultRow
        @columns = result.columns
        @_broadcastChange()
      
      _onFailedExecution : (error) =>
        @_clear()
        @error = error
        @_broadcastChange()
        
      _broadcastChange : ->
        $rootScope.$broadcast 'graphService.changed', [this]
        
      _cleanResultRow : (row) ->
        for cell in row
          if not (cell?)
            null
          else if cell.self?
            cell.data
          else
            cell
            
      _clear : ->
        @rows    = []
        @columns = []
        @error   = null
        @isLoading = false
      
    new GraphService
  
])
