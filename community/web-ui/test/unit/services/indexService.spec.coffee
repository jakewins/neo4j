'use strict'

describe "app.services.indexes.IndexService", ->

  INDEX_URI = "/db/data/schema/index"

  beforeEach(module "app.services.indexes")

  describe "the index service",  ->

    indexes = []

    beforeEach inject ($httpBackend) -> 
      indexes = [{
        "label": "User",
        "property-keys": [
          "name"
        ],
        "state" : "ONLINE"
      }]
      $httpBackend.expect 'GET', INDEX_URI
      $httpBackend.whenGET(INDEX_URI).respond(indexes)

    it "should list available indexes", inject( ($httpBackend, indexService, $rootScope) ->
  
      # GIVEN
      events = []
      $rootScope.$on "indexService.changed", (ev) -> events.push(ev)
      
      # WHEN
      $httpBackend.flush()
      
      # THEN
      expect(indexService.indexes).toEqual [{
        "label": "User",
        "propertyKeys": [
          "name"
        ],
        "state" : "ONLINE"
      }]
      expect(events.length).toEqual 1

    )

    it "should correctly drop indexes", inject( ($httpBackend, indexService, $rootScope) ->
  
      # GIVEN
      events = []
      $rootScope.$on "indexService.changed", (ev) -> events.push(ev)
      $httpBackend.flush()

      $httpBackend.expect 'POST', "/db/data/transaction/commit"
      $httpBackend.whenPOST("/db/data/transaction/commit").respond(200, {
        "results" : [],
        "errors"  : []
      })
      
      # WHEN
      indexService.dropIndex("User", "name")
      $httpBackend.flush()

      # THEN
      expect(indexService.indexes).toEqual []
      expect(events.length).toEqual 2
      
    )

    it "should correctly create indexes", inject( ($httpBackend, indexService, $rootScope) ->
  
      # GIVEN
      events = []
      $rootScope.$on "indexService.changed", (ev) -> events.push(ev)
      $httpBackend.flush()

      $httpBackend.expect 'POST', "/db/data/transaction/commit"
      $httpBackend.whenPOST("/db/data/transaction/commit").respond(200, {
        "results" : [],
        "errors"  : []
      })
      
      # WHEN
      indexService.createIndex("SomeLabel", "name")
      $httpBackend.flush()

      # THEN
      expect(indexService.indexes).toEqual [ { label : 'User', 'propertyKeys' : [ 'name' ], "state" : "ONLINE" }, { label : 'SomeLabel', 'propertyKeys' : [ 'name' ], "state" : "POPULATING" } ]
      expect(events.length).toEqual 2
      
    )

    it "should trigger callback if creation fails", inject( ($httpBackend, indexService, $rootScope) ->
  
      # GIVEN
      events = []
      errors = []
      $rootScope.$on "indexService.changed", (ev) -> events.push(ev)
      $httpBackend.flush()

      $httpBackend.expect 'POST', "/db/data/transaction/commit"
      $httpBackend.whenPOST("/db/data/transaction/commit").respond(200, {
        "results" : [],
        "errors"  : [{ code:1, message:"Hello, world!" }]
      })
      
      # WHEN
      indexService.createIndex("SomeLabel", "name", (e) -> errors.push(e))
      $httpBackend.flush()

      # THEN
      expect(indexService.indexes).toEqual [ { label : 'User', 'propertyKeys' : [ 'name' ], "state" : "ONLINE" } ]
      expect(events.length).toEqual 1
      expect(errors).toEqual [[{ code:1, message:"Hello, world!" }]]
      
    )