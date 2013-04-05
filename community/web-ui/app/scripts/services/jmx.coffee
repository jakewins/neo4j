'use strict'

### A service that manages a common view of the graph for the entire app ###

angular.module('app.services.jmx', [])

.factory('jmxService', [
  '$http'
  '$rootScope'
  ($http, $rootScope)->
    class JmxService
    
      constructor : () ->
        
        @domains = {}

        # Load initial beans
        $http.get("/db/manage/server/jmx/domain/*/*")
          .success(@_populateWithInitialData)

      beanParameters : (name) ->
        params = {}
        for p in name.split(":",2)[1].split ','
          [k,v] = p.split '=', 2
          params[k] = v
        return params

      _populateWithInitialData : (data) =>
        @domains = {}
        for bean in data

          params = @beanParameters(bean.name)
          bean.simpleName = if params.name? then params.name else params.type

          domain = bean.name.split ":", 1
          @domains[domain] ?= []
          @domains[domain].push(bean)

        $rootScope.$broadcast 'jmxService.changed', [@domains]

    new JmxService
  
])
