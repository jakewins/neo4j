'use strict'

angular.module('app.services.console', [])

.factory('consoleService', [
  '$http'
  '$rootScope'
  ($http, $rootScope)->
    
    class HttpEngine
      
      statementRegex : /^((GET)|(PUT)|(POST)|(DELETE)) ([^ ]+)( (.+))?$/i
      
      constructor : (@_engineKey, @_out) ->
        @_out(@_engineKey, "", [
                "Welcome to the REST Shell!",
                "Usage: <VERB> <PATH> [JSON]",
                'Eg: GET /db/data or POST /db/data/node {"name":"My Node"}'])
        
      execute : (statement, out) ->
        if @statementRegex.test statement
          result = @statementRegex.exec statement
          [method, url, data] = [result[1], result[6], result[8]]
          if data?
            try
              data = JSON.parse(data)
            catch e
              @_out(@_engineKey, statement, ["Invalid JSON payload."])
              return
          $http(method:method, url:url, data:data,
                headers:{'Content-Type': 'application/json'})
            .success(@_onResponse(statement))
            .error(  @_onResponse(statement))
        else
          @_out(@_engineKey, statement,
              ["Invalid syntax, syntax is: <VERB> <URI> <JSON DATA>"])
    
      _onResponse : (statement) ->
        (payload, status, meta) =>
          if typeof(payload) is 'object'
            payload = JSON.stringify(payload,null, "  ")
          @_out(@_engineKey, statement, ["#{status}", payload])
    
    class RemoteEngine
      
      constructor : (@_engineKey, @_out) ->
        @_sendStatement("init()")
          .error @_onInitFailed
      
      execute : (statement) ->
        @_sendStatement(statement)
          .success(@_onStatementExecuted(statement))
          .error (@_onStatementFailed(statement))
      
      _sendStatement : (statement) ->
        $http.post '/db/manage/server/console', {
          command : statement, engine:@_engineKey }

      _onStatementExecuted : (statement) ->
        (result) =>
          [lines, prompt] = result
          
          @_out(@_engineKey, statement, lines.split '\n')
        
      _onStatementFailed : (statement) ->
        (error) =>
          @_out(@_engineKey, statement,
            ["Unable to execute statement, please see the server logs."])

      _onInitFailed : (error)=>
        init_error_msg =
            "The server failed to initialize this shell. It responded with:"
        @_out(@_engineKey, "", [init_error_msg, error])
    
    
    class ConsoleService
    
      constructor : ->
        @engines = {}
        @_initializeEngines()
          
      execute : (statement, engine) ->
        if @engines[engine]?
          @engines[engine].engine.execute statement
        else
          throw new Exception("Unknown shell engine #{engine}.")
        
      _appendInteraction : (engine, statement, result) =>
        if not @engines[engine]?
          @_defineEngine(engine, engine, null)
          
        result = result.join '\n'
          
        @engines[engine].interactions.push
          statement : statement
          result : result
          
        $rootScope.$broadcast 'consoleService.changed',
          [engine, statement, result]
          
      _initializeEngines : ->
        # Local HTTP shell
        @_defineEngine('http', 'REST Shell',
                       new HttpEngine('http', @_appendInteraction))

        # Server-provided remote shells
        $http.get('/db/manage/server/console')
          .success( (response) =>
            @_initRemoteEngine(engine) for engine in response.engines
            $rootScope.$broadcast 'consoleService.changed'
          )
          .error @_onInitializingRemoteEnginesFailed
  
      _onInitializingRemoteEnginesFailed : (err)=>
      
      _initRemoteEngine : (key) ->
        engine = new RemoteEngine( key, @_appendInteraction )
        @_defineEngine(key, @_humanReadableEngineName(key), engine)
      
      _defineEngine : (key, name, engine) ->
        def = @engines[key] ?= {}
        def.key           = key
        def.name          = name
        def.interactions ?= []
        def.engine        = engine
      
      _humanReadableEngineName : (engineKey) ->
        knownEngines =
          'shell'   : "Neo4j Shell"
          'gremlin' : "Gremlin"
        if knownEngines[engineKey]?
          knownEngines[engineKey]
        else
          engineKey
        
  
    new ConsoleService
  
])
