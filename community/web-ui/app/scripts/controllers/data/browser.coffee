'use strict'

angular.module('app.controllers.data.browser', [
  'app.services.graph'
  'app.services.paginator'])

.controller('DatabrowserController', [
  '$scope'
  'graphService'
  'paginatorService'

  ($scope, graphService, paginatorService) ->
    
    PAGE_SIZE = 20
    
    $scope.query    = graphService.query
    $scope.page     = 1
    
    $scope.execute = ->
      graphService.executeQuery $scope.query

    # Logaritmic paginator
    $scope.updatePagination = (page)->
      
      if page is '_PREV' then page = $scope.page - 1
      if page is '_NEXT' then page = $scope.page + 1
      
      # State
      numberOfRows      = $scope.allRows.length
      numberOfPages     = Math.ceil(numberOfRows / PAGE_SIZE)
      buttons = paginatorService.calculateNiceButtons page, numberOfPages
      
      # Do the actual slicing of the rows for this page
      start = PAGE_SIZE * (page-1)
      end   = start+PAGE_SIZE
      end   = if end < numberOfRows then end else numberOfRows
      
      $scope.page = page
      $scope.numberOfPages = numberOfPages
      $scope.pageButtons = buttons
      $scope.rows = $scope.allRows.slice(start, end)
    
    
    # Auto-update the displayed rows if the graph service changes
    synchronizeWithGraphData = ->
      $scope.allRows   = graphService.rows
      $scope.columns   = graphService.columns
      $scope.error     = graphService.error
      $scope.isLoading = graphService.isLoading
      
      $scope.updatePagination(1)

    $scope.graphService = graphService
    $scope.$on 'graphService.changed', synchronizeWithGraphData
    synchronizeWithGraphData()
])