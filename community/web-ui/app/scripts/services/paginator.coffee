'use strict'

### A service that manages a common view of the graph for the entire app ###

angular.module('app.services.paginator', [])

.factory('paginatorService', [
  
  ()->
    class PaginatorService
      
      calculateNiceButtons : (currentPage, numberOfPages) ->
        # This is here to leave space for a fancy-pants button generator later,
        # for now it just generates back/forward, and not full pagination.
        
        buttons = [
          {text:'«', action:'_PREV', disabled: currentPage == 1}
          {text:'»', action:'_NEXT', disabled: currentPage == numberOfPages}]
        
        buttons
      
    new PaginatorService
  
])
