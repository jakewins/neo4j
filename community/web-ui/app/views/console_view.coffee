template = require 'views/templates/console'
PageView = require 'views/base/page_view'

module.exports = class HomePageView extends PageView
  template: template
  className: 'console-page'
