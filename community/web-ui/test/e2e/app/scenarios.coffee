"use strict"

# http://docs.angularjs.org/guide/dev_guide.e2e-testing 
describe "Neo4j Web UI", ->
  beforeEach ->
    browser().navigateTo "/"

  it "should show a set of frontpage articles", ->
    expect(repeater("[ng-view] h2").count()).toEqual 3

  it "should have a try-me cypher statement", ->
    query = "START n=node(0) RETURN 'hello', 'world'"
    input(".hero-query-wrapper textarea").enter query
    element("[ng-view] .hero-query-wrapper .btn").click()

    expect(repeater("[ng-view] table tr").count()).toEqual 2

  describe "data browser", ->
    beforeEach ->
      browser().navigateTo "#/data/browser"