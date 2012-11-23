Controller = require 'controllers/base/controller'
IndexesView = require 'views/indexes_view'

module.exports = class IndexesController extends Controller
  historyURL: 'indexes'

  index: ->
    @view = new IndexesView()
