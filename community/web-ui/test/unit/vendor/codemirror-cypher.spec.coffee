'use strict'

describe "cypher syntax highlighter", ->

  it "should lex stuff correctly", ->
    
    lexing 'START',        shouldYield ['keyword']
    lexing 'START RETURN', shouldYield ['keyword', null, 'keyword']
    lexing 'MATCH (myNode:Label1:Label2)-[:KNOWS]->other',
      shouldYield [ 'keyword', null, null, 'variable', 'atom', 'atom', null, null, null, 'atom', null, null, 'variable' ]

  lexing = (query, expected) ->
    highlighter = codeMirrorCypherHighlighter({}) 
    stream = new CodeMirror.StringStream(query, 4);
    
    state = highlighter.startState()
    actual = while not stream.eol() 
      style = highlighter.token stream, state
      substr = stream.current()
      stream.start = stream.pos
      [style, substr] # For debugging
      style
    
    expect(actual).toEqual expected
      
  shouldYield = (expected) -> expected
