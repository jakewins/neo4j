'use strict'

### Directives ###

# register the module with Angular
angular.module('app.directives', [
  # require the 'app.service' module
  'app.services'
])

.directive('appVersion', [
  'version', 'edition'

  (version, edition) ->

    (scope, elm, attrs) ->
      elm.text("#{version} #{edition} Edition")
])
