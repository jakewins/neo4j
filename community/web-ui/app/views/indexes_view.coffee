template = require 'views/templates/indexes'
PageView = require 'views/base/page_view'

module.exports = class IndexesView extends PageView
  template: template
  className: 'indexes-page'
