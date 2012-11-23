template = require 'views/templates/info'
PageView = require 'views/base/page_view'

module.exports = class InfoView extends PageView
  template: template
  className: 'indexes-page'
