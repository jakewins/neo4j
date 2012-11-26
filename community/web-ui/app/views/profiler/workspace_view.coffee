start_template = require 'views/templates/profiler/start'
running_template = require 'views/templates/profiler/running'

View = require 'views/base/view'

Profiler = require 'models/profiler'

module.exports = class ProfilerWorkspaceView extends View

  events :
    "click .profiling-start" : "onStartProfilingClicked"
      
  initialize : ->
    super
    @model.on "change:state", => @render()
    @model.on "change:events", => @render()
      
  getTemplateFunction: ->
    console.log @model, @model.getState()
    switch @model.getState()
      when Profiler.State.STOPPED then start_template
      when Profiler.State.RUNNING then running_template
      when Profiler.State.PAUSED  then running_template
      
  onStartProfilingClicked : ->
    @model.startProfiling()