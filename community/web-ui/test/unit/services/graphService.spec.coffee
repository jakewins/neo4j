'use strict'

describe "app.services.graph.GraphService", ->

  beforeEach(module "app.services.graph")

  describe "executeQuery",  ->
    
    it "should populate state correctly on non-empty result", inject( ($httpBackend, graphService) ->
      
      result = {
          "columns" : [ "myNode" ],
          "data" : [ [ {
            "data" : {
              "name" : "My First Node"
            },
            "self" : "http://localhost:7474/db/data/node/33256",
          } ] ]
        }
                  
      $httpBackend.expect 'POST', '/db/data/cypher'
      $httpBackend.whenPOST('/db/data/cypher').respond(result)
  
      # WHEN
      graphService.executeQuery 'TESTQUERY'
      $httpBackend.flush()
      
      # THEN
      expect(graphService.query).toEqual 'TESTQUERY'
      expect(graphService.columns).toEqual [ "myNode" ]
      expect(graphService.rows).toEqual [ [ { "name" : "My First Node" } ] ]
    )
    
    it "should populate state correctly on empty result", inject( ($httpBackend, graphService) ->
      
      result = {
          "columns" : [ "myNode" ],
          "data" : [ ]
        }
                  
      $httpBackend.expect 'POST', '/db/data/cypher'
      $httpBackend.whenPOST('/db/data/cypher').respond(result)
  
      # WHEN
      graphService.executeQuery 'TESTQUERY'
      $httpBackend.flush()
      
      # THEN
      expect(graphService.query).toEqual 'TESTQUERY'
      expect(graphService.columns).toEqual [ "myNode" ]
      expect(graphService.rows).toEqual [ ]
    )
    
    it "should populate state correctly on failed result", inject( ($httpBackend, graphService) ->
      
      result = {
        "message" : "expected START or CREATE\n\"// Enter query \"\n                ^",
        "exception" : "SyntaxException",
        "stacktrace" : [ "org.neo4j.cypher.internal.parser.v2_0.CypherParserImpl.parse(CypherParserImpl.scala:46)", "org.neo4j.cypher.CypherParser.parse(CypherParser.scala:44)", "org.neo4j.cypher.ExecutionEngine.prepare(ExecutionEngine.scala:69)", "org.neo4j.cypher.ExecutionEngine.execute(ExecutionEngine.scala:59)", "org.neo4j.cypher.ExecutionEngine.execute(ExecutionEngine.scala:65)", "org.neo4j.cypher.javacompat.ExecutionEngine.execute(ExecutionEngine.java:79)", "org.neo4j.server.rest.web.CypherService.cypher(CypherService.java:70)", "java.lang.reflect.Method.invoke(Method.java:597)" ]
      }
                  
      $httpBackend.expect 'POST', '/db/data/cypher'
      $httpBackend.whenPOST('/db/data/cypher').respond(400, result)
  
      # WHEN
      graphService.executeQuery 'TESTQUERY'
      $httpBackend.flush()
      
      # THEN
      expect(graphService.query).toEqual 'TESTQUERY'
      expect(graphService.columns).toEqual [ ]
      expect(graphService.rows).toEqual [ ]
      expect(graphService.error).toEqual result
    )
    
    it "should go to a loading state while waiting for a reply", inject( ($httpBackend, graphService) ->
                  
      $httpBackend.expect 'POST', '/db/data/cypher', {"query":"TESTQUERY"}
      $httpBackend.whenPOST('/db/data/cypher').respond({data:[],columns:[]})
  
      # WHEN
      graphService.executeQuery 'TESTQUERY'
      
      # THEN
      expect(graphService.isLoading).toEqual true
      
      # AND WHEN
      $httpBackend.flush()
      
      # THEN
      expect(graphService.isLoading).toEqual false
      
    )
    
    it "should send the correct query to the database", inject( ($httpBackend, graphService) ->
                  
      $httpBackend.expect 'POST', '/db/data/cypher', {"query":"TESTQUERY"}
      $httpBackend.whenPOST('/db/data/cypher').respond({data:[],columns:[]})
  
      # WHEN
      graphService.executeQuery 'TESTQUERY'
    )
    
    it "should broadcast a change event the service state changes", inject( ($rootScope, $httpBackend, graphService) ->
      
      changeEvents = []
      result = {
          "columns" : [ "myNode" ],
          "data" : [ [ {
            "data" : {
              "name" : "My First Node"
            },
            "self" : "http://localhost:7474/db/data/node/33256",
          } ] ]
        }
                  
      $httpBackend.expect 'POST', '/db/data/cypher'
      $httpBackend.whenPOST('/db/data/cypher').respond(result)
      $rootScope.$on "graphService.changed", (ev) -> changeEvents.push(ev)
  
      # WHEN
      graphService.executeQuery 'TESTQUERY'
      
      # THEN
      expect(changeEvents.length).toEqual 1
      expect(graphService.query).toEqual 'TESTQUERY'
      expect(graphService.isLoading).toEqual true
      
      # AND WHEN (the server has actually responded)
      $httpBackend.flush()
      
      # THEN
      expect(changeEvents.length).toEqual 2
      expect(graphService.query).toEqual 'TESTQUERY'
      expect(graphService.isLoading).toEqual false
      expect(graphService.columns).toEqual [ "myNode" ]
      expect(graphService.rows).toEqual [ [ { "name" : "My First Node" } ] ]
    )