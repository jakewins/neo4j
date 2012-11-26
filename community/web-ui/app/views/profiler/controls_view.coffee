template = require 'views/templates/profiler/controls'
View = require 'views/base/view'

Profiler = require 'models/profiler'

module.exports = class ProfilerControlsView extends View

  template : template
  
  events :
    "click .profiler-stop"   : "onStopProfilingClicked"
    "click .profiler-pause"  : "onPauseProfilingClicked"
    "click .profiler-resume" : "onResumeProfilingClicked"
  
  initialize : ->
    super
    @model.on "change", => @render()

  getTemplateData : ->
    if @model.getState() is Profiler.State.STOPPED
      {disabled : true}
    else if @model.getState() is Profiler.State.PAUSED
      {paused : true}
    else
      @model.serialize()
      
  onStopProfilingClicked : ->
    @model.stopProfiling()
    
  onPauseProfilingClicked : ->
    @model.pauseProfiling()
    
  onResumeProfilingClicked : ->
    @model.startProfiling()