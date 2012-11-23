Controller = require 'controllers/base/controller'
Header = require 'models/header'
HeaderView = require 'views/header_view'

mediator = require 'mediator'

module.exports = class HeaderController extends Controller
  initialize: ->
    super
    
    @model = new Header()
    @view = new HeaderView({@model})