module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('<!DOCTYPE html>\n<html lang="en" ng-app="app">\n  <head>\n    <meta charset="utf-8">\n    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">\n    <meta name="viewport" content="width=device-width" initial-scale="1.0">\n    <meta name="description" content="The Neo4j Web UI">\n    <meta name="author" content="Neo Technology">\n    <title ng-bind-template="{{pageTitle}}"></title>\n    <link rel="stylesheet" href="css/app.css">\n    <link rel="shortcut icon" href="img/favicon.ico"><!--[if lte IE 7]>\n    <script src="http://cdnjs.cloudflare.com/ajax/libs/json2/20110223/json2.js"></script><![endif]--><!--[if lte IE 8]>\n    <script src="//html5shiv.googlecode.com/svn/trunk/html5.js"></script>\n    <script src="build/angular-ui-ieshiv.js"></script><![endif]-->\n    <script>\n      window.brunch = window.brunch || {};\n      window.brunch[\'auto-reload\'] = {\n        enabled: true\n      };\n    </script>\n    <script src="js/vendor.js"></script>\n    <script src="js/app.js"></script>\n  </head>\n  <body ng-controller="AppCtrl">\n    <div class="wrapper">\n      <div class="navbar navbar navbar-inverse navbar-fixed-top">\n        <div class="navbar-inner">\n          <div class="container-fluid"><a ng-href="#/" class="brand"><img src="img/logo.png"></a></div>\n        </div>\n      </div>\n      <div ng-include="\'partials/sidebar.html\'" class="sidebar-container"></div>\n      <div ng-view class="main-content"></div>\n    </div>\n  </body>\n</html>');
}
return buf.join("");
};module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('\n<div class="row-fluid data-console">\n  <div class="span10">\n    <div ng-repeat="interaction in interactions">\n      <pre ng-show="interaction.statement" class="data-console-statement">{{ interaction.statement }}</pre>\n      <pre class="data-console-result">{{ interaction.result }}</pre>\n    </div>\n    <div scroll-here-on-change="interactions.length" class="prefix">>\n      <textarea ui-codemirror="{theme:\'eclipse\',mode:\'cypher\', extraKeys:{\'Ctrl-Space\': \'cypherAutoComplete\',\'Enter\': execute, \'Up\': prevHistory, \'Down\': nextHistory}}" ng-model="statement" autofocus="autofocus"></textarea>\n    </div>\n  </div>\n  <div class="span2">\n    <div class="dropdown"><a data-toggle="dropdown" role="button" href="#" class="btn dropdown-toggle">Engine: {{ engineName }} <b class="caret"></b></a>\n      <ul role="menu" class="dropdown-menu">\n        <li ng-repeat="(key,engine) in availableEngines"> <a ng-click="changeEngine(key)" data-toggle="dropdown">{{engine.name}}</a></li>\n      </ul>\n    </div>\n  </div>\n</div>');
}
return buf.join("");
};module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('\n<h1>Explore</h1>\n<div class="row-fluid">\n  <div visualization="yep" style="height:400px;" class="span12 well"></div>\n  <div class="muted text-small"> <code>SCROLL</code> Zooms  <code>CLICK DRAG</code> Moves node, or whole graph</div>\n</div>');
}
return buf.join("");
};module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('\n<div class="data-browser">\n  <div class="data-browser-dash"> \n    <div class="data-browser-query-wrap">\n      <div ng-click="execute()" class="btn btn-primary data-browser-exec">Execute &raquo;</div>\n      <div class="code-mirror-wrap">\n        <textarea ui-codemirror="{theme:\'eclipse\',mode:\'cypher\', extraKeys:{\'Ctrl-Space\': \'cypherAutoComplete\',\'Ctrl-Enter\': execute}}" ng-model="query"></textarea>\n      </div>\n      <div class="muted text-small"> <code>CTRL RETURN</code> Executes statement <code>CTRL SPACE</code> Auto-completes</div>\n    </div>\n  </div>\n  <div ng-show="isLoading" class="row-fluid">\n    <div class="loading-screen well">\n      <h3>Hold on to your chair!</h3>\n      <p>Neo4j is executing your statement..</p>\n    </div>\n  </div>\n  <div ng-show="!isLoading">\n    <div ng-show="error" class="row-fluid">\n      <div class="span12">\n        <h4>{{ error.exception }}</h4>\n        <pre>{{ error.message }}</pre>\n        <p>\n          <div data-toggle="collapse" data-target=".data-browser ul.stacktrace" class="btn btn-mini">Stacktrace</div>\n        </p>\n        <ul class="stacktrace collapse">\n          <li ng-repeat="stack in error.stacktrace"> \n            <pre>{{ stack }}</pre>\n          </li>\n        </ul>\n      </div>\n    </div>\n    <div ng-show="rows.length" class="row-fluid">\n      <div class="span10">\n        <table class="table table-striped table-bordered">\n          <tr>\n            <th ng-repeat="column in columns">{{ column }}</th>\n          </tr>\n          <tr ng-repeat="row in rows">\n            <td ng-repeat="cell in row">{{ cell }}</td>\n          </tr>\n        </table>\n      </div>\n      <div class="span2">\n        <div class="pagination pagination-small pagination-centered">\n          <ul>\n            <li class="disabled"><a>Page {{ page }} of {{ numberOfPages }}</a></li>\n            <li ng-repeat="button in pageButtons" ng-class="{disabled:button.disabled}"> <a ng-click="updatePagination(button.action)">{{ button.text }}</a></li>\n          </ul>\n        </div>\n      </div>\n    </div>\n    <div ng-show="!rows.length &amp;&amp; !error" class="row-fluid">\n      <div class="span12 well">The result set is empty</div>\n    </div>\n  </div>\n</div>');
}
return buf.join("");
};module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('\n<li ng-repeat="item in items" ng-class="{active: item.active}"><a ng-href="{{ item.href }}" ng-show="item.href"><span class="icon icon-{{ item.icon }}"></span>{{ item.title }}</a>\n  <div ng-show="!item.href" class="title">{{ item.title }} »\n    <div class="subtitle">{{ item.subtitle }}</div>\n  </div>\n  <ul ng-include="\'partials/menu_recursive.html\'" ng-init="items=item.items"></ul>\n</li>');
}
return buf.join("");
};module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('\n<div ng-show="showIndexDropWarning" class="alert alert-block alert-warning">\n  <h4>Warning!</h4>\n  <p>Dropping an index cannot be undone, are you sure you want to </p>drop the index for nodes labelled \'{{ indexToDrop.label }}\' on property key \'{{ indexToDrop.propertyKeys[0] }}\'?\n  <p>\n    <div ng-click="dropIndex(indexToDrop)" class="btn btn-warning">Drop it</div>\n    <div ng-click="showIndexDropWarning = false" class="btn">Get me out of here</div>\n  </p>\n</div>\n<table class="table table-striped table-bordered">\n  <tr>\n    <th>Label</th>\n    <th>Property</th>\n    <th>Status</th>\n    <th></th>\n  </tr>\n  <tr ng-repeat="index in indexes"> \n    <td>{{ index.label }}</td>\n    <td>{{ index.propertyKeys }}</td>\n    <td>{{ index.state }}</td>\n    <td> \n      <div ng-click="promptDropIndex(index)" class="btn btn-danger btn-small"> <i class="icon-fire icon-white"></i> Drop</div>\n    </td>\n  </tr>\n</table>\n<p>\n  <div ng-click="showCreateIndex()" class="btn btn-small"><i class="icon-plus"></i> New index</div>\n</p>\n<div modal="showCreateModal">\n  <div class="modal-header">\n    <h4>Create new index</h4>\n  </div>\n  <div class="modal-body">\n    <p>Create a new index for all nodes labelled</p>\n    <input ng-model="newIndexLabel" placeholder="Label" type="text" class="input-xlarge"/>\n    <p>On the property</p>\n    <input ng-model="newIndexProperty" placeholder="Property" type="text" class="input-xlarge"/>\n  </div>\n  <div class="modal-footer">\n    <div ng-click="newIndex()" class="btn btn-primary">Create</div>\n    <div ng-click="showCreateModal=false" class="btn cancel">Cancel</div>\n  </div>\n</div>');
}
return buf.join("");
};module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('\n<div ng-show="showIndexDropWarning" class="alert alert-block alert-warning">\n  <h4>Warning!</h4>\n  <p>Dropping an index cannot be undone, are you sure you want to drop the {{ toDropType }} index named \'{{ toDropName }}\'?</p>\n  <p>\n    <div ng-click="dropIndex(toDropType, toDropName)" class="btn btn-warning">Drop it</div>\n    <div ng-click="showIndexDropWarning = false" class="btn">Get me out of here</div>\n  </p>\n</div>\n<h4>Node indexes\n  <div ng-click="showCreateNodeIndex()" class="btn btn-small"><i class="icon-plus"></i> New index</div>\n</h4>\n<table class="table table-striped table-bordered">\n  <tr>\n    <th>Index name</th>\n    <th>Configuration</th>\n    <th></th>\n  </tr>\n  <tr ng-repeat="(name, index) in nodeIndexes"> \n    <td>{{ name }}</td>\n    <td>{{ index }}</td>\n    <td> \n      <div ng-click="promptDropNodeIndex(name)" class="btn btn-danger btn-small"><i class="icon-fire icon-white"></i> Drop</div>\n    </td>\n  </tr>\n</table>\n<h4>Relationship indexes\n  <div ng-click="showCreateRelationshipIndex()" class="btn btn-small"><i class="icon-plus"></i> New index</div>\n</h4>\n<table class="table table-striped table-bordered">\n  <tr>\n    <th>Index name</th>\n    <th>Configuration</th>\n    <th></th>\n  </tr>\n  <tr ng-repeat="(name, index) in relationshipIndexes"> \n    <td>{{ name }}</td>\n    <td>{{ index }}</td>\n    <td> \n      <div ng-click="promptDropRelationshipIndex(name)" class="btn btn-danger btn-small"> <i class="icon-fire icon-white"></i> Drop</div>\n    </td>\n  </tr>\n</table>\n<div modal="showCreateModal">\n  <div class="modal-header">\n    <h4>Create new index</h4>\n  </div>\n  <div class="modal-body">\n    <input ng-model="newIndexName" placeholder="Enter index name" type="text" class="input-xlarge"/>\n  </div>\n  <div class="modal-footer">\n    <div ng-click="newIndex()" class="btn btn-primary">Create</div>\n    <div ng-click="showCreateModal=false" class="btn cancel">Cancel</div>\n  </div>\n</div>');
}
return buf.join("");
};module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('\n<div ng-controller="SidebarController" class="sidebar">\n  <ul ng-include="\'partials/menu_recursive.html\'" ng-init="items=menuItems" class="main-menu"></ul>\n  <div class="sidebar-resources"><small>Resources</small><br/><a href="http://docs.neo4j.org/chunked/stable/tutorials-rest.html" class="btn btn-link btn-mini">Getting started</a><a href="http://docs.neo4j.org/chunked/stable/" class="btn btn-link btn-mini">Documentation</a></a><a href="https://github.com/neo4j/neo4j/issues/new" class="btn btn-link btn-mini">Report an issue</a></div>\n</div>');
}
return buf.join("");
};module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('\n<div class="row-fluid">\n  <div class="span12">\n    <div class="hero-unit hero-unit-inverse neo-20-announcement-hero">\n      <div class="hero-query-wrapper">\n        <div class="hero-query-logo-wrapper"><img src="img/big-logo.png" class="hero-query-logo"/>\n          <div ng-click="execute()" class="btn btn-large btn-primary splash-exec">Execute &raquo;</div>\n          <div class="code-mirror-wrap">\n            <textarea ui-codemirror="{theme:\'eclipse\',mode:\'cypher\', extraKeys:{\'Ctrl-Space\': \'cypherAutoComplete\',\'Ctrl-Enter\': execute}}" ng-model="query"></textarea>\n          </div>\n        </div>\n      </div>\n    </div>\n  </div>\n</div>\n<div class="row-fluid">\n  <div class="span4">\n    <h2>New to Neo4j?</h2>\n    <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui.</p>\n    <p><a class="btn">View details &raquo;</a></p>\n  </div>\n  <div class="span4">\n    <h2>Labels!</h2>\n    <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui.</p>\n    <p><a class="btn">View details &raquo;</a></p>\n  </div>\n  <div class="span4">\n    <h2>Cypher performance</h2>\n    <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>\n    <p><a class="btn">View details &raquo;</a></p>\n  </div>\n</div>');
}
return buf.join("");
};module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('\n<div class="row-fluid">\n  <div class="span3">\n    <h4>Domains</h4>\n    <ul class="nav nav-tabs nav-stacked">\n      <li ng-repeat="(domain, beans) in domains"> <a ng-click="setDomain(domain)">{{ domain }}</a></li>\n    </ul>\n  </div>\n  <div class="span3">\n    <div ng-show="beans.length">\n      <h4>Domain - {{ domain }}</h4>\n      <ul ng-show="beans.length" class="nav nav-tabs nav-stacked">\n        <li ng-repeat="bean in beans"> <a ng-click="setBean(bean)">{{ bean.simpleName }} </a></li>\n      </ul>\n    </div>\n    <div ng-show="!beans.length">\n      <h4>Domain</h4>\n      <h5>No domain selected</h5>\n    </div>\n  </div>\n  <div class="span6">\n    <div ng-show="bean">\n      <h4>Bean - {{ bean.simpleName }}</h4>\n      <p>{{ bean.description }}</p>\n      <dl class="dl-horizontal">\n        <div ng-repeat="attr in simpleAttributes">\n          <dt>{{ attr.name }}</dt>\n          <dd>{{ attr.value }}</dd>\n        </div>\n      </dl>\n      <div ng-repeat="attr in complexAttributes">\n        <h5>{{ attr.name }}</h5>\n        <pre>{{ attr.value }}</pre>\n      </div>\n    </div>\n    <div ng-show="!bean">\n      <h4>Bean</h4>\n      <h5>No bean selected</h5>\n    </div>\n  </div>\n</div>');
}
return buf.join("");
};