'use strict'

angular.module('app.controllers.sidebar', [])

.controller('SidebarController', [
  '$scope'
  '$location'

  ($scope, $location) ->
    
    $scope.menuItems = [
      {
        href: '#/data/browser', title: 'Data',
        icon: 'th', active: yes
      }
      
      {
        href: '#/data/console',      title: 'Console',
        icon: 'list-alt', active: false
      }
    ]
    
    $scope.$location = $location
    $scope.$watch('$location.path()', (path) ->
      path = "#" + (path || '/')
      for item in $scope.menuItems
        item.active = item.href == path
    )
])