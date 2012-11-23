View = require 'views/base/view'
template = require 'views/templates/sidebar'

module.exports = class SidebarView extends View
  template: template
  id: 'sidebar'
  className: 'sidebar'
  container: '#sidebar-container'
  autoRender: true

  initialize: ->
    super
    @subscribeEvent 'startupController', @render
    @modelBind 'change:items', @onSidebarChanged

  onSidebarChanged : () ->
    @render()