'use strict'

angular.module('app.controllers.data.console', [])

.controller('ConsoleController', [
  '$scope'
  '$rootScope'
  'consoleService'

  ($scope, $rootScope, consoleService) ->
    
    # Store the current console in the global state, so we don't loose it
    # when the user switches between views
    state = $rootScope.consoleState ?=
      engine : "shell"
      # Current statement
      statement : ""
      # Steps back in history we are currently looking at,
      # 0 is latest, higher is older.
      historyIndex : 0
    $scope.statement = state.statement
    
    synchronizeWithConsoleService = ->
      engine = state.engine
      $scope.availableEngines = consoleService.engines
      if consoleService.engines[engine]?
        engineState = consoleService.engines[engine]
        $scope.interactions = engineState.interactions
        $scope.engineName = engineState.name
      else
        $scope.interactions = []
        $scope.engineName = engine
    
    $scope.$on('consoleService.changed', synchronizeWithConsoleService)
    synchronizeWithConsoleService()
    
    $scope.changeEngine = (engine)->
      state.engine = engine
      state.historyIndex = 0
      setStatement ""
      synchronizeWithConsoleService()
      
    $scope.prevHistory = ->
      interactions = $scope.interactions
      idx = state.historyIndex + 1
      if idx <= interactions.length
        state.historyIndex = idx
        setStatement interactions[interactions.length-idx].statement
      
    $scope.nextHistory = ->
      interactions = $scope.interactions
      idx = state.historyIndex - 1
      if idx > 0
        state.historyIndex = idx
        setStatement interactions[interactions.length-idx].statement
      else if idx is 0
        state.historyIndex = idx
        setStatement ""
    
    $scope.execute = ->
      state.historyIndex = 0
      consoleService.execute $scope.statement, state.engine
      setStatement ""
      
    setStatement = (s)->
      state.statement = $scope.statement = s
    
])