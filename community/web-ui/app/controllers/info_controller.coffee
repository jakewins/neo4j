Controller = require 'controllers/base/controller'
InfoView = require 'views/info_view'

module.exports = class InfoController extends Controller
  historyURL: 'info'

  index: ->
    @view = new InfoView()
