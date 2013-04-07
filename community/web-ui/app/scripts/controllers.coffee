'use strict'

### Controllers ###

angular.module('app.controllers', [
  'app.controllers.sidebar'
  'app.controllers.data.querytool'
  'app.controllers.data.explorer'
  'app.controllers.data.console'
  'app.controllers.schema.indexes'
  'app.controllers.system.jmx'
  'app.controllers.splash'
])

.controller('AppCtrl', [
  '$scope'
  '$location'
  '$resource'
  '$rootScope'

($scope, $location, $resource, $rootScope) ->
  # NOTE: This is the root controller, you'll find specific controllers in the
  # 'controllers' subdirectory.
])

