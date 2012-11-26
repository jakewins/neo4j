Controller = require 'controllers/base/controller'
ProfilerView = require 'views/profiler/profiler_view'

mediator = require 'mediator'

module.exports = class ProfilerController extends Controller
  historyURL: 'profiler'

  index: ->
    console.log mediator
    @view = new ProfilerView({profiler:mediator.profiler})