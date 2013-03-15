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

.directive('scrollHereOnChange', ->
  (scope, element, attrs) ->
    scope.$watch attrs.scrollHereOnChange, ->
      setTimeout((->
        if window.innerHeight < (element[0].offsetTop + 50)
          window.scrollTo(0, element[0].offsetTop)
      ),0)
)