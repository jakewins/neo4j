'use strict'

angular.module('app.services.console', [])

.factory('consoleService', [
  '$http'
  ($http)->
    class ConsoleService
    
      constructor : ->
        @interactions = []
        @_sendStatement "init()"
          
      execute : (statement) ->
        @_sendStatement(statement)
          .success(@_onStatementExecuted(statement))
          .error(  @_onStatementFailed(statement))
        
      _onStatementExecuted : (statement) ->
        (result) =>
          [lines, prompt] = result
          
          @interactions.push { statement : statement, result : lines }
          console.log @interactions
        
      _onStatementFailed : (statement) ->
        (error) =>
          console.log error
        
      _sendStatement : (statement) ->
        $http.post '/db/manage/server/console', {
          command : statement, engine:"SHELL" }
        
  
    new ConsoleService
  
])
