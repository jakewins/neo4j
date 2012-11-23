Model = require 'models/base/model'

class HttpClient
  
  constructor : (@baseUrl) ->
  
  get : (path, params, cb) -> @_http "GET", path, params, cb
    
  _http : (method, path, data, cb) ->
    $.ajax({
      url : @baseUrl + path,
      type : method,
      data : data,
      timeout: 1000 * 60 * 60 * 6,
      cache: false,
      # Let jquery turn data map into query string
      # only on GET requests.
      processData : method == "GET",
      contentType : "application/json",
      success : cb,
      error   : cb,
      dataType : "json"
    })

class ProfilingService
  
  constructor : () ->
    #@_http = new HttpClient("#{location.protocol}//#{location.host}/db/manage")
    @_http = new HttpClient("http://localhost:7474/db/manage")
  
  attachListener : (listener) ->
    @_http.get("", {}, ()->console.log arguments)
    @_listener = listener
    
    if not (@_timer?)
      @_timer = setInterval(@_poll, 500)
  
  stop : () ->
    if @_timer?
      clearInterval @_timer
      delete @_timer
  
  _poll : =>
    @_listener([{
        timestamp : new Date().getTime() / 1000,
        query : "START blah",
        wasCached: false,
        parseTime : 12 }])
  

module.exports = class Profiler extends Model
  
  @State :
    STOPPED    : 0
    RUNNING    : 1
    PAUSED     : 2
  
  defaults:
    state: Profiler.State.STOPPED
    events : []
    
  constructor : ->
    super
    @_profilingService = new ProfilingService()
    @eventLimit = 1000
    
  getState : -> @get 'state'
  
  startProfiling : ->
    @set state : Profiler.State.RUNNING
    @_profilingService.attachListener(@_onNewProfilingEvents)
  
  stopProfiling : ->
    @_profilingService.stop()
    
  pauseProfiling : ->
    @_profilingService.stop()
  
  _onNewProfilingEvents : (newEvents) =>
    events = @get 'events'
    for event in newEvents
      events.splice(0, 0, event)
      
    if events.length > @eventLimit
      events.splice(@eventLimit, events.length - @eventLimit)
      
    @trigger 'change:events', events