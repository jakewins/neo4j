Model = require 'models/base/model'

module.exports = class Sidebar extends Model
  defaults:
    items: [
      {href: '/',          title: 'Data',        icon: 'th'},
      {href: '/profiler/', title: 'Profiler',    icon: 'signal'},
      {href: '/console/',  title: 'Console',     icon: 'list-alt'},
      {href: '/indexes/',  title: 'Indexes',     icon: 'filter'},
      {href: '/info/',     title: 'Server info', icon: 'wrench'},
    ]
    
  setCurrentPattern : (pattern) ->
    items = @get 'items'
    for item in items
      item.current = item.href[1..] == pattern
    @set {items}
    @trigger "change:items", items