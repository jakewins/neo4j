
mediator = require 'mediator'

Controller = require 'controllers/base/controller'
Navigation = require 'models/navigation'

module.exports = class NavigationController extends Controller

  initialize: ->
    super
    @model = new Navigation()
    mediator.navigation = @model
    
    # And then listen for changes
    @subscribeEvent 'matchRoute', @onUrlChanged
    
    
  onUrlChanged : (route, _ ) =>
    @model.set {pattern:route.pattern}
    @publishEvent "navigationChanged", @model