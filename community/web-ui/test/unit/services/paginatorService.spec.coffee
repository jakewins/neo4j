'use strict'

describe "app.services.paginator", ->

  beforeEach(module "app.services.paginator")

  describe "PaginatorService",  ->
    
    it "should list disable when result fits in one page", inject( (paginatorService) ->
      
      # WHEN
      buttons = paginatorService.calculateNiceButtons(1, 1)
      
      # THEN
      expect(buttons).toEqual [
        {text:'«', action:'_PREV', disabled:true}
        {text:'»', action:'_NEXT', disabled:true}]
      
    )

    
    it "should list no pages when result fits in one page", inject( (paginatorService) ->
      
      # WHEN
      buttons = paginatorService.calculateNiceButtons(1, 3)
      
      # THEN
      expect(buttons).toEqual [
        {text:'«', action:'_PREV', disabled:true}
        {text:'»', action:'_NEXT', disabled:false}]
      
    )