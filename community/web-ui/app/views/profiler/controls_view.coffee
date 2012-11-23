template = require 'views/templates/profiler/controls'
View = require 'views/base/view'

Profiler = require 'models/profiler'

module.exports = class ProfilerControlsView extends View

  template : template
  
  initialize : ->
    super
    @model.on "change", => @render()

  getTemplateData : ->
    if @model.getState() is Profiler.State.STOPPED
      {disabled : true}
    else
      @model.serialize()