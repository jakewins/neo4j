'use strict'

### Directives ###

# register the module with Angular
angular.module('app.directives', [
  # require the 'app.service' module
  'app.services'
])

.directive('appVersion', [
  'version', 'edition'

  (version, edition) ->

    (scope, elm, attrs) ->
      elm.text("#{version} #{edition} Edition")
])

.directive('scrollHereOnChange', ->
  (scope, element, attrs) ->
    scope.$watch attrs.scrollHereOnChange, ->
      setTimeout((->
        if window.innerHeight < (element[0].offsetTop + 50)
          $('html, body').stop().animate({
            scrollTop : element[0].offsetTop
          }, 400)
          #window.scrollTo(0, element[0].offsetTop)
      ),0)
)

.directive('visualization', ->
  (scope, el, attrs) ->
    graph = Viva.Graph.graph()
    layout = Viva.Graph.Layout.forceDirected(graph, {
      springLength : 10,
      springCoeff : 0.0008,
      dragCoeff : 0.02,
      gravity : -1.2
    })

    # TODO: Enable WebGL if browser supports it
    graphics = Viva.Graph.View.svgGraphics()
    graphics.node( (node) ->
      Viva.Graph.svg('rect')
        .attr('width', 10)
        .attr('height', 10)
        .attr('fill', '#00a2e8')
    )

    renderer = Viva.Graph.View.renderer(graph, {
      layout     : layout,
      graphics   : graphics,
      container  : el[0],
      renderLinks : true
    })

    renderer.run(50)

    i = 0; m = 10; n = 20
    addInterval = setInterval( (->
      graph.beginUpdate()

      for j in [0..m]
        node = i + j * n
        if i > 0
          graph.addLink(node, i - 1 + j * n)
          graph.addLink(node, i - 1 + j * n)
        if (j > 0) then graph.addLink(node, i + (j - 1) * n)
      
      i++
      graph.endUpdate()

      if i >= n
        clearInterval(addInterval)
    ), 100)
)