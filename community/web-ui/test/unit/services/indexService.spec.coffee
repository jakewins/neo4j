'use strict'

describe "app.services.indexes.IndexService", ->

  JMX_URI = "/db/manage/server/jmx/domain/*/*"

  beforeEach(module "app.services.indexes")

  describe "the index service",  ->

    beforeEach inject ($httpBackend) -> 


    it "should list available indexes", inject( ($httpBackend, indexService, $rootScope) ->
  
      # GIVEN
      events = []
      $rootScope.$on "indexService.changed", (ev) -> events.push(ev)
      
      # WHEN
      # $httpBackend.flush()
      
      # THEN
      
    )