Controller = require 'controllers/base/controller'
ConsoleView = require 'views/console_view'

module.exports = class ConsoleController extends Controller
  historyURL: 'console'

  index: ->
    @view = new ConsoleView()
