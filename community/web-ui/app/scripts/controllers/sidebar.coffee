'use strict'

angular.module('app.controllers.sidebar', [])

.controller('SidebarController', [
  '$scope'
  '$location'
  'consoleService'

  ($scope, $location, consoleService) ->
    
    $scope.menuItems = [
      {
        title: 'Data',
        icon: 'heart', active: no,
        items : [
          { # BROWSER
            href: '#/data/browser',
            title: 'Query Tool',
            icon: 'list', active: no,
          }
          
          { # BROWSER
            href: '#/data/visualizer',
            title: 'Explorer',
            icon: 'map-marker', active: no,
          }

          
          { # CONSOLE
            title: 'Shell',
            icon: 'list-alt', active: no,
            href: '#/data/console'
          }
        ]
      }
      {
        title: 'Schema',
        active: no,
        items : [
          # { # CONSTRAINTS
          #  href: '#/schema/constraints', title: 'Constraints',
          #  icon: 'tags', active: no,
          #},
          
          { # INDEXES
            href: '#/schema/indexes', title: 'Indexes',
            icon: 'filter', active: no,
          }
          
          { # LEGACY INDEXES
            href: '#/schema/legacy-indexes', title: 'Legacy indexes',
            icon: 'filter', active: no,
          }
        ]
      }
      
      {
        title: 'System',
        active: no,
        items : [
          { # JMX Browser
            href: '#/schema/indexes', title: 'JMX Browser',
            icon: 'cog', active: no,
          }
          
        ]
      }
    ]
    
    $scope.$location = $location
    $scope.$watch('$location.path()', (path) ->
      path = "#" + (path || '/')
      
      setActive = (items) ->
        for item in items
          item.active = item.href == path
          setActive item.items if item.items?
      setActive $scope.menuItems
    )
])