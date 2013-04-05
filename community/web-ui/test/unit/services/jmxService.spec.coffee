'use strict'

describe "app.services.jmx.JmxService", ->

  JMX_URI = "/db/manage/server/jmx/domain/*/*"

  beforeEach(module "app.services.jmx")

  describe "load jmx beans",  ->

    beforeEach inject ($httpBackend) -> 
      $httpBackend.expect 'GET', JMX_URI
      $httpBackend.whenGET(JMX_URI).respond([
        {
          "description": "Information on the management interface of the MBean",
          "name": "java.lang:name=CMS Old Gen,type=MemoryPool",
          "attributes": [
            {
              "description": "Name",
              "name": "Name",
              "value": "CMS Old Gen",
              "isReadable": "true",
              "type": "java.lang.String",
              "isWriteable": "false ",
              "isIs": "false "
            },
            {
              "description": "Type",
              "name": "Type",
              "value": "HEAP",
              "isReadable": "true",
              "type": "java.lang.String",
              "isWriteable": "false ",
              "isIs": "false "
            }
          ]  
        }
      ])

    it "should populate state correctly on non-empty result", inject( ($httpBackend, jmxService, $rootScope) ->
  
      # GIVEN
      events = []
      $rootScope.$on "jmxService.changed", (ev) -> events.push(ev)
      # WHEN
      $httpBackend.flush()
      
      # THEN
      expect(jmxService.domains).toEqual {
        "java.lang" : [{
          "description": "Information on the management interface of the MBean",
          "name": "java.lang:name=CMS Old Gen,type=MemoryPool",
          "simpleName" : "CMS Old Gen",
          "attributes": [
            {
              "description": "Name",
              "name": "Name",
              "value": "CMS Old Gen",
              "isReadable": "true",
              "type": "java.lang.String",
              "isWriteable": "false ",
              "isIs": "false "
            },
            {
              "description": "Type",
              "name": "Type",
              "value": "HEAP",
              "isReadable": "true",
              "type": "java.lang.String",
              "isWriteable": "false ",
              "isIs": "false "
            }
          ]  
        }]
      }


      expect(events.length).toEqual 1
    )