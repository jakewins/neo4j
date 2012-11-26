Model = require 'models/base/model'

class HttpClient
  
  constructor : (@baseUrl) ->
  
  get : (path, params, cb) -> @_http "GET", path, params, cb
  
  put : (path, data, cb) -> @_http "PUT", path, data, cb
    
  _http : (method, path, data={}, cb=(->)) ->
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
    @_http = new HttpClient("#{location.protocol}//#{location.host}/db/manage")
    @_lastPoll = 0
  
  attachListener : (listener) ->
    @_http.put("/server/profile/start", {}, @_onServerProfilingStarted)
    @_listener = listener
  
  stop : () ->
    if @_timer?
      @_lastPoll = 0
      clearInterval @_timer
      delete @_timer
      @_http.put "/server/profile/stop", {}
  
  _poll : =>
    @_http.get "/server/profile/fetch/#{@_lastPoll}", {}, @_onRecievedEvents
        
  _onServerProfilingStarted : =>
    if not (@_timer?)
      @_timer = setInterval(@_poll, 1000)
      
  _onRecievedEvents : (events)=>
    if events.length > 0
      @_lastPoll = events[0].timestamp
      @_listener events

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
    @set "events" : []
    @_profilingService.stop()
    @set state : Profiler.State.STOPPED
    
  pauseProfiling : ->
    @_profilingService.stop()
    @set state : Profiler.State.PAUSED
  
  _onNewProfilingEvents : (newEvents) =>
    events = @get 'events'
    spliceArgs = [0, 0].concat(newEvents)
    events.splice.apply(events, spliceArgs)
      
    if events.length > @eventLimit
      events.splice(@eventLimit, events.length - @eventLimit)
      
    @trigger 'change:events', events