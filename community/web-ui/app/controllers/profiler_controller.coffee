Controller = require 'controllers/base/controller'
ProfilerView = require 'views/profiler/profiler_view'

Profiler = require 'models/profiler'

module.exports = class ProfilerController extends Controller
  historyURL: 'profiler'

  initialize: ->
    super
    @profiler = new Profiler()

  index: ->
    @view = new ProfilerView({@profiler})