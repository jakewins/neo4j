
mediator = require 'mediator'

Controller = require 'controllers/base/controller'
Sidebar = require 'models/sidebar'
SidebarView = require 'views/sidebar_view'

module.exports = class NavigationController extends Controller

  initialize: ->
    super
    @model = new Sidebar()
    
    @subscribeEvent 'navigationChanged', @onNavigationChanged
    @onNavigationChanged(mediator.navigation)
    
    @view = new SidebarView({@model})
    
  onNavigationChanged : (navigation) =>
    
    @model.setCurrentPattern navigation.get('pattern', '')