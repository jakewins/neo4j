'use strict'

describe "app.services.console.ConsoleService", ->

  CONSOLE_URI = '/db/manage/server/console'

  beforeEach(module "app.services.console")

  describe "remote shells",  ->
    
    beforeEach inject ($httpBackend) -> 
      $httpBackend.expect 'GET', CONSOLE_URI
      $httpBackend.whenGET(CONSOLE_URI).respond({"engines":["A_TEST_ENGINE"]})
    
    it "should correctly execute statements", inject( ($httpBackend, consoleService) ->
      # GIVEN initial handshake has taken place
      $httpBackend.expect 'POST', CONSOLE_URI, {"command":"init()","engine":"A_TEST_ENGINE"}
      $httpBackend.whenPOST(CONSOLE_URI, {"command":"init()","engine":"A_TEST_ENGINE"}).respond([])
      $httpBackend.flush()
      
      # AND GIVEN
      result = 'SOME RESULT\nON MULTIPLE LINES'
                  
      $httpBackend.expect 'POST', CONSOLE_URI, {"command":'TESTQUERY',"engine":"A_TEST_ENGINE"}
      $httpBackend.whenPOST(CONSOLE_URI).respond([result, 'someprompt>'])
  
      # WHEN
      consoleService.execute 'TESTQUERY', 'A_TEST_ENGINE'
      $httpBackend.flush()
      
      # THEN
      interactions = consoleService.engines['A_TEST_ENGINE'].interactions
      expect(interactions).toEqual [ { statement : 'TESTQUERY', result : result } ] 
    )
    
    it "should handle initialization failing", inject( ($httpBackend, consoleService) ->
      # GIVEN initial handshake has taken place
      $httpBackend.expect 'POST', CONSOLE_URI, {"command":"init()", "engine":"A_TEST_ENGINE"}
      $httpBackend.whenPOST(CONSOLE_URI, {"command":"init()","engine":"A_TEST_ENGINE"}).respond(500, "Some arbitrary text")
      
      # WHEN
      $httpBackend.flush()
      
      # THEN
      interations = consoleService.engines['A_TEST_ENGINE'].interactions
      expect(interations).toEqual [ { statement : '', result : "The server failed to initialize this shell. It responded with:\nSome arbitrary text" } ]
    )

    it "should handle statement failing", inject( ($rootScope, $httpBackend, consoleService) ->
      # GIVEN initial handshake has taken place
      $httpBackend.expect 'POST', CONSOLE_URI, {"command":"init()","engine":"A_TEST_ENGINE"}
      $httpBackend.whenPOST(CONSOLE_URI, {"command":"init()","engine":"A_TEST_ENGINE"}).respond([])
      $httpBackend.flush()

      # AND GIVEN
      events = []
      $rootScope.$on "consoleService.changed", (ev) -> events.push(ev)
      $httpBackend.expect 'POST', CONSOLE_URI, {"command":'TESTQUERY',"engine":"A_TEST_ENGINE"}
      $httpBackend.whenPOST(CONSOLE_URI).respond(400, ["SYNTAX ERROR!!!", 'someprompt>'])

      # WHEN 
      consoleService.execute 'TESTQUERY', 'A_TEST_ENGINE'  
      $httpBackend.flush()

      # THEN
      interations = consoleService.engines['A_TEST_ENGINE'].interactions
      expect(interations).toEqual [ { 
        statement : 'TESTQUERY', 
        result : 'Unable to execute statement, please see the server logs.' } ]
      expect(events.length).toEqual 1
    )
  
  describe 'REST Shell', ->
    
    beforeEach inject ($httpBackend) -> 
      $httpBackend.expect 'GET', CONSOLE_URI
      $httpBackend.whenGET(CONSOLE_URI).respond({"engines":[]})
    
    it "should be able to send REST commands", inject( ($httpBackend, consoleService) ->
      
      $httpBackend.flush()
      
      # GIVEN
      response = ["this","is","the","response"]
      $httpBackend.expect 'GET', '/someurl'
      $httpBackend.whenGET('/someurl').respond(response)
      
      # WHEN
      consoleService.execute("GET /someurl", "http")
      $httpBackend.flush()
      
      # THEN
      interactions = consoleService.engines['http'].interactions[1]
      expect(interactions).toEqual { statement : 'GET /someurl', result : '''200\n[
      	  "this",
      	  "is",
      	  "the",
      	  "response"
      	]''' }
    )
    
    