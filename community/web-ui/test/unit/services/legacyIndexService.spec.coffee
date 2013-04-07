'use strict'

describe "app.services.indexes.LegacyIndexService", ->

  beforeEach(module "app.services.indexes")

  NODE_INDEX_URI = "/db/data/index/node"
  REL_INDEX_URI = "/db/data/index/relationship"

  describe "basic operations",  ->

    nodeIndexes = {}

    beforeEach inject ($httpBackend) -> 
      nodeIndexes = {
        "myIndex" : { "provider" : "lucene" }
      }
      $httpBackend.expect 'GET', NODE_INDEX_URI
      $httpBackend.expect 'GET', REL_INDEX_URI
      $httpBackend.whenGET(NODE_INDEX_URI).respond(nodeIndexes)
      $httpBackend.whenGET(REL_INDEX_URI).respond({})

    it "should populate state correctly on init", inject( ($httpBackend, legacyIndexService, $rootScope) ->
  
      # GIVEN
      events = []
      $rootScope.$on "legacyIndexService.changed", (ev) -> events.push(ev)

      # WHEN
      $httpBackend.flush()
      
      # THEN
      expect(legacyIndexService.nodeIndexes).toEqual {
        "myIndex" : { "provider" : "lucene" }
      }

      expect(events.length).toEqual 2
    )

    it "should delete indexes", inject( ($httpBackend, legacyIndexService, $rootScope) ->
  
      # GIVEN
      events = []
      $rootScope.$on "legacyIndexService.changed", (ev) -> events.push(ev)

      $httpBackend.flush()

      $httpBackend.expect 'DELETE', NODE_INDEX_URI + "/myIndex"
      $httpBackend.whenDELETE(NODE_INDEX_URI + "/myIndex").respond(200)

      # WHEN
      legacyIndexService.deleteNodeIndex("myIndex")
      
      # THEN
      expect(legacyIndexService.nodeIndexes).toEqual {
        "myIndex" : { "provider" : "lucene" }
      }

      expect(events.length).toEqual 2

      # AND WHEN
      $httpBackend.flush()

      # THEN
      expect(legacyIndexService.nodeIndexes).toEqual {}
      expect(events.length).toEqual 3
    )

    it "should create indexes", inject( ($httpBackend, legacyIndexService, $rootScope) ->
  
      # GIVEN
      events = []
      $rootScope.$on "legacyIndexService.changed", (ev) -> events.push(ev)

      $httpBackend.flush()

      $httpBackend.expect 'POST', NODE_INDEX_URI
      $httpBackend.whenPOST(NODE_INDEX_URI).respond({
        "template": "http://localhost:7474/db/data/index/node/Bob/{key}/{value}"  
      })
      $httpBackend.expect 'GET', NODE_INDEX_URI
      nodeIndexes["Bob"] = { "provider" : "lucene" }

      # WHEN
      legacyIndexService.newNodeIndex("Bob")
      
      # THEN no event should have been triggered yet
      expect(events.length).toEqual 2

      # AND WHEN
      $httpBackend.flush()

      # THEN we should see the change locally
      expect(legacyIndexService.nodeIndexes).toEqual { 
        myIndex : { provider : 'lucene' }, 
        Bob : { provider : 'lucene' }}

      expect(events.length).toEqual 3
    )