'use strict'

### Sevices ###

angular.module('app.services', [
  'app.services.graph'
  'app.services.console'
])

.factory 'version', -> "2.0"
.factory 'edition', -> "Community"
