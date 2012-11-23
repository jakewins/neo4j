template = require 'views/templates/profiler/profiler'
PageView = require 'views/base/page_view'

ProfilerControlsView = require 'views/profiler/controls_view'
ProfilerWorkspaceView = require 'views/profiler/workspace_view'

Profiler = require 'models/profiler'

module.exports = class ProfilerView extends PageView
  
  className: 'profiler-page'
  
  template : template
  
  initialize : (opts)->
    super
    @profiler = opts.profiler
    
  renderSubviews : =>
    @workspace = new ProfilerWorkspaceView({
      model : @profiler,
      el    : @$("#profiler-workspace")})
    
    @controls = new ProfilerControlsView({
      model : @profiler,
      el    : @$("#profiler-controls")})
    
    @workspace.render()
    @controls.render()