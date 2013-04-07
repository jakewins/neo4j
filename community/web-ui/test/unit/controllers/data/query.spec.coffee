'use strict'

describe "app.controllers.data.querytool.QueryToolController", ->

  beforeEach -> module "app.controllers.data.querytool"

  describe "pagination",  ->
    
    it "should list no pages when result fits in one page", inject( ($rootScope, $controller, graphService) ->
      
      scope = $rootScope.$new()
      ctrl = $controller("QueryToolController", {$scope: scope })
      
      scope.allRows = [1,2,3]
      
      # WHEN
      scope.updatePagination(1)
      
      # THEN
      expect(scope.rows).toEqual [1,2,3]
      
    )

    it "should show first page correctly", inject( ($rootScope, $controller, graphService) ->

      scope = $rootScope.$new()
      ctrl = $controller("QueryToolController", {$scope: scope })
      scope.allRows = for i in [1...200]
        i

      # WHEN
      scope.updatePagination(1)

      # THEN
      expect(scope.rows).toEqual [ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 ]

    )
    

    it "should show middle page correctly", inject( ($rootScope, $controller, graphService) ->

      scope = $rootScope.$new()
      ctrl = $controller("QueryToolController", {$scope: scope })
      scope.allRows = for i in [1...1000]
        i

      # WHEN
      scope.updatePagination(25)

      # THEN
      expect(scope.rows).toEqual [ 481, 482, 483, 484, 485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 497, 498, 499, 500 ] 

    )