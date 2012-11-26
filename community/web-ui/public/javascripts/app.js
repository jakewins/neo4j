(function(/*! Brunch !*/) {
  'use strict';

  var globals = typeof window !== 'undefined' ? window : global;
  if (typeof globals.require === 'function') return;

  var modules = {};
  var cache = {};

  var has = function(object, name) {
    return ({}).hasOwnProperty.call(object, name);
  };

  var expand = function(root, name) {
    var results = [], parts, part;
    if (/^\.\.?(\/|$)/.test(name)) {
      parts = [root, name].join('/').split('/');
    } else {
      parts = name.split('/');
    }
    for (var i = 0, length = parts.length; i < length; i++) {
      part = parts[i];
      if (part === '..') {
        results.pop();
      } else if (part !== '.' && part !== '') {
        results.push(part);
      }
    }
    return results.join('/');
  };

  var dirname = function(path) {
    return path.split('/').slice(0, -1).join('/');
  };

  var localRequire = function(path) {
    return function(name) {
      var dir = dirname(path);
      var absolute = expand(dir, name);
      return globals.require(absolute);
    };
  };

  var initModule = function(name, definition) {
    var module = {id: name, exports: {}};
    definition(module.exports, localRequire(name), module);
    var exports = cache[name] = module.exports;
    return exports;
  };

  var require = function(name) {
    var path = expand(name, '.');

    if (has(cache, path)) return cache[path];
    if (has(modules, path)) return initModule(path, modules[path]);

    var dirIndex = expand(path, './index');
    if (has(cache, dirIndex)) return cache[dirIndex];
    if (has(modules, dirIndex)) return initModule(dirIndex, modules[dirIndex]);

    throw new Error('Cannot find module "' + name + '"');
  };

  var define = function(bundle) {
    for (var key in bundle) {
      if (has(bundle, key)) {
        modules[key] = bundle[key];
      }
    }
  }

  globals.require = require;
  globals.require.define = define;
  globals.require.brunch = true;
})();

window.require.define({"application": function(exports, require, module) {
  var Application, Chaplin, HeaderController, Layout, NavigationController, Profiler, SessionController, SidebarController, mediator, routes,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  mediator = require('mediator');

  routes = require('routes');

  SessionController = require('controllers/session_controller');

  HeaderController = require('controllers/header_controller');

  NavigationController = require('controllers/navigation_controller');

  SidebarController = require('controllers/sidebar_controller');

  Layout = require('views/layout');

  Profiler = require('models/profiler');

  module.exports = Application = (function(_super) {

    __extends(Application, _super);

    function Application() {
      return Application.__super__.constructor.apply(this, arguments);
    }

    Application.prototype.title = 'Neo4j';

    Application.prototype.initialize = function() {
      Application.__super__.initialize.apply(this, arguments);
      this.initDispatcher();
      this.initLayout();
      this.initMediator();
      this.initControllers();
      this.initRouter(routes, {
        pushState: false
      });
      return typeof Object.freeze === "function" ? Object.freeze(this) : void 0;
    };

    Application.prototype.initLayout = function() {
      return this.layout = new Layout({
        title: this.title
      });
    };

    Application.prototype.initControllers = function() {
      new NavigationController();
      new SessionController();
      new HeaderController();
      return new SidebarController();
    };

    Application.prototype.initMediator = function() {
      mediator.user = null;
      mediator.navigation = null;
      mediator.profiler = new Profiler();
      console.log("Asd", mediator);
      return mediator.seal();
    };

    return Application;

  })(Chaplin.Application);
  
}});

window.require.define({"controllers/base/controller": function(exports, require, module) {
  var Chaplin, Controller,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  module.exports = Controller = (function(_super) {

    __extends(Controller, _super);

    function Controller() {
      return Controller.__super__.constructor.apply(this, arguments);
    }

    return Controller;

  })(Chaplin.Controller);
  
}});

window.require.define({"controllers/console_controller": function(exports, require, module) {
  var ConsoleController, ConsoleView, Controller,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Controller = require('controllers/base/controller');

  ConsoleView = require('views/console_view');

  module.exports = ConsoleController = (function(_super) {

    __extends(ConsoleController, _super);

    function ConsoleController() {
      return ConsoleController.__super__.constructor.apply(this, arguments);
    }

    ConsoleController.prototype.historyURL = 'console';

    ConsoleController.prototype.index = function() {
      return this.view = new ConsoleView();
    };

    return ConsoleController;

  })(Controller);
  
}});

window.require.define({"controllers/header_controller": function(exports, require, module) {
  var Controller, Header, HeaderController, HeaderView, mediator,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Controller = require('controllers/base/controller');

  Header = require('models/header');

  HeaderView = require('views/header_view');

  mediator = require('mediator');

  module.exports = HeaderController = (function(_super) {

    __extends(HeaderController, _super);

    function HeaderController() {
      return HeaderController.__super__.constructor.apply(this, arguments);
    }

    HeaderController.prototype.initialize = function() {
      HeaderController.__super__.initialize.apply(this, arguments);
      this.model = new Header();
      return this.view = new HeaderView({
        model: this.model
      });
    };

    return HeaderController;

  })(Controller);
  
}});

window.require.define({"controllers/home_controller": function(exports, require, module) {
  var Controller, HomeController, HomePageView,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Controller = require('controllers/base/controller');

  HomePageView = require('views/home_page_view');

  module.exports = HomeController = (function(_super) {

    __extends(HomeController, _super);

    function HomeController() {
      return HomeController.__super__.constructor.apply(this, arguments);
    }

    HomeController.prototype.historyURL = 'home';

    HomeController.prototype.index = function() {
      return this.view = new HomePageView();
    };

    return HomeController;

  })(Controller);
  
}});

window.require.define({"controllers/indexes_controller": function(exports, require, module) {
  var Controller, IndexesController, IndexesView,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Controller = require('controllers/base/controller');

  IndexesView = require('views/indexes_view');

  module.exports = IndexesController = (function(_super) {

    __extends(IndexesController, _super);

    function IndexesController() {
      return IndexesController.__super__.constructor.apply(this, arguments);
    }

    IndexesController.prototype.historyURL = 'indexes';

    IndexesController.prototype.index = function() {
      return this.view = new IndexesView();
    };

    return IndexesController;

  })(Controller);
  
}});

window.require.define({"controllers/info_controller": function(exports, require, module) {
  var Controller, InfoController, InfoView,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Controller = require('controllers/base/controller');

  InfoView = require('views/info_view');

  module.exports = InfoController = (function(_super) {

    __extends(InfoController, _super);

    function InfoController() {
      return InfoController.__super__.constructor.apply(this, arguments);
    }

    InfoController.prototype.historyURL = 'info';

    InfoController.prototype.index = function() {
      return this.view = new InfoView();
    };

    return InfoController;

  })(Controller);
  
}});

window.require.define({"controllers/navigation_controller": function(exports, require, module) {
  var Controller, Navigation, NavigationController, mediator,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  mediator = require('mediator');

  Controller = require('controllers/base/controller');

  Navigation = require('models/navigation');

  module.exports = NavigationController = (function(_super) {

    __extends(NavigationController, _super);

    function NavigationController() {
      this.onUrlChanged = __bind(this.onUrlChanged, this);
      return NavigationController.__super__.constructor.apply(this, arguments);
    }

    NavigationController.prototype.initialize = function() {
      NavigationController.__super__.initialize.apply(this, arguments);
      this.model = new Navigation();
      mediator.navigation = this.model;
      return this.subscribeEvent('matchRoute', this.onUrlChanged);
    };

    NavigationController.prototype.onUrlChanged = function(route, _) {
      this.model.set({
        pattern: route.pattern
      });
      return this.publishEvent("navigationChanged", this.model);
    };

    return NavigationController;

  })(Controller);
  
}});

window.require.define({"controllers/profiler_controller": function(exports, require, module) {
  var Controller, ProfilerController, ProfilerView, mediator,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Controller = require('controllers/base/controller');

  ProfilerView = require('views/profiler/profiler_view');

  mediator = require('mediator');

  module.exports = ProfilerController = (function(_super) {

    __extends(ProfilerController, _super);

    function ProfilerController() {
      return ProfilerController.__super__.constructor.apply(this, arguments);
    }

    ProfilerController.prototype.historyURL = 'profiler';

    ProfilerController.prototype.index = function() {
      console.log(mediator);
      return this.view = new ProfilerView({
        profiler: mediator.profiler
      });
    };

    return ProfilerController;

  })(Controller);
  
}});

window.require.define({"controllers/session_controller": function(exports, require, module) {
  var Controller, LoginView, SessionController, User, mediator,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  mediator = require('mediator');

  Controller = require('controllers/base/controller');

  User = require('models/user');

  LoginView = require('views/login_view');

  module.exports = SessionController = (function(_super) {

    __extends(SessionController, _super);

    function SessionController() {
      this.logout = __bind(this.logout, this);

      this.serviceProviderSession = __bind(this.serviceProviderSession, this);

      this.triggerLogin = __bind(this.triggerLogin, this);
      return SessionController.__super__.constructor.apply(this, arguments);
    }

    SessionController.serviceProviders = {};

    SessionController.prototype.loginStatusDetermined = false;

    SessionController.prototype.loginView = null;

    SessionController.prototype.serviceProviderName = null;

    SessionController.prototype.initialize = function() {
      this.subscribeEvent('serviceProviderSession', this.serviceProviderSession);
      this.subscribeEvent('logout', this.logout);
      this.subscribeEvent('userData', this.userData);
      this.subscribeEvent('!showLogin', this.showLoginView);
      this.subscribeEvent('!login', this.triggerLogin);
      this.subscribeEvent('!logout', this.triggerLogout);
      return this.getSession();
    };

    SessionController.prototype.loadServiceProviders = function() {
      var name, serviceProvider, _ref, _results;
      _ref = SessionController.serviceProviders;
      _results = [];
      for (name in _ref) {
        serviceProvider = _ref[name];
        _results.push(serviceProvider.load());
      }
      return _results;
    };

    SessionController.prototype.createUser = function(userData) {
      return mediator.user = new User(userData);
    };

    SessionController.prototype.getSession = function() {
      var name, serviceProvider, _ref, _results;
      this.loadServiceProviders();
      _ref = SessionController.serviceProviders;
      _results = [];
      for (name in _ref) {
        serviceProvider = _ref[name];
        _results.push(serviceProvider.done(serviceProvider.getLoginStatus));
      }
      return _results;
    };

    SessionController.prototype.showLoginView = function() {
      if (this.loginView) {
        return;
      }
      this.loadServiceProviders();
      return this.loginView = new LoginView({
        serviceProviders: SessionController.serviceProviders
      });
    };

    SessionController.prototype.triggerLogin = function(serviceProviderName) {
      var serviceProvider;
      serviceProvider = SessionController.serviceProviders[serviceProviderName];
      if (!serviceProvider.isLoaded()) {
        this.publishEvent('serviceProviderMissing', serviceProviderName);
        return;
      }
      this.publishEvent('loginAttempt', serviceProviderName);
      return serviceProvider.triggerLogin();
    };

    SessionController.prototype.serviceProviderSession = function(session) {
      this.serviceProviderName = session.provider.name;
      this.disposeLoginView();
      session.id = session.userId;
      delete session.userId;
      this.createUser(session);
      return this.publishLogin();
    };

    SessionController.prototype.publishLogin = function() {
      this.loginStatusDetermined = true;
      this.publishEvent('login', mediator.user);
      return this.publishEvent('loginStatus', true);
    };

    SessionController.prototype.triggerLogout = function() {
      return this.publishEvent('logout');
    };

    SessionController.prototype.logout = function() {
      this.loginStatusDetermined = true;
      this.disposeUser();
      this.serviceProviderName = null;
      this.showLoginView();
      return this.publishEvent('loginStatus', false);
    };

    SessionController.prototype.userData = function(data) {
      return mediator.user.set(data);
    };

    SessionController.prototype.disposeLoginView = function() {
      if (!this.loginView) {
        return;
      }
      this.loginView.dispose();
      return this.loginView = null;
    };

    SessionController.prototype.disposeUser = function() {
      if (!mediator.user) {
        return;
      }
      mediator.user.dispose();
      return mediator.user = null;
    };

    return SessionController;

  })(Controller);
  
}});

window.require.define({"controllers/sidebar_controller": function(exports, require, module) {
  var Controller, NavigationController, Sidebar, SidebarView, mediator,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  mediator = require('mediator');

  Controller = require('controllers/base/controller');

  Sidebar = require('models/sidebar');

  SidebarView = require('views/sidebar_view');

  module.exports = NavigationController = (function(_super) {

    __extends(NavigationController, _super);

    function NavigationController() {
      this.onNavigationChanged = __bind(this.onNavigationChanged, this);
      return NavigationController.__super__.constructor.apply(this, arguments);
    }

    NavigationController.prototype.initialize = function() {
      NavigationController.__super__.initialize.apply(this, arguments);
      this.model = new Sidebar();
      this.subscribeEvent('navigationChanged', this.onNavigationChanged);
      this.onNavigationChanged(mediator.navigation);
      return this.view = new SidebarView({
        model: this.model
      });
    };

    NavigationController.prototype.onNavigationChanged = function(navigation) {
      return this.model.setCurrentPattern(navigation.get('pattern', ''));
    };

    return NavigationController;

  })(Controller);
  
}});

window.require.define({"initialize": function(exports, require, module) {
  var Application;

  Application = require('application');

  $(document).on('ready', function() {
    var app;
    app = new Application();
    return app.initialize();
  });
  
}});

window.require.define({"lib/services/service_provider": function(exports, require, module) {
  var Chaplin, ServiceProvider, utils;

  utils = require('lib/utils');

  Chaplin = require('chaplin');

  module.exports = ServiceProvider = (function() {

    _(ServiceProvider.prototype).extend(Chaplin.EventBroker);

    ServiceProvider.prototype.loading = false;

    function ServiceProvider() {
      _(this).extend($.Deferred());
      utils.deferMethods({
        deferred: this,
        methods: ['triggerLogin', 'getLoginStatus'],
        onDeferral: this.load
      });
    }

    ServiceProvider.prototype.disposed = false;

    ServiceProvider.prototype.dispose = function() {
      if (this.disposed) {
        return;
      }
      this.unsubscribeAllEvents();
      this.disposed = true;
      return typeof Object.freeze === "function" ? Object.freeze(this) : void 0;
    };

    return ServiceProvider;

  })();

  /*

    Standard methods and their signatures:

    load: ->
      # Load a script like this:
      utils.loadLib 'http://example.org/foo.js', @loadHandler, @reject

    loadHandler: =>
      # Init the library, then resolve
      ServiceProviderLibrary.init(foo: 'bar')
      @resolve()

    isLoaded: ->
      # Return a Boolean
      Boolean window.ServiceProviderLibrary and ServiceProviderLibrary.login

    # Trigger login popup
    triggerLogin: (loginContext) ->
      callback = _(@loginHandler).bind(this, loginContext)
      ServiceProviderLibrary.login callback

    # Callback for the login popup
    loginHandler: (loginContext, response) =>

      eventPayload = {provider: this, loginContext}
      if response
        # Publish successful login
        @publishEvent 'loginSuccessful', eventPayload

        # Publish the session
        @publishEvent 'serviceProviderSession',
          provider: this
          userId: response.userId
          accessToken: response.accessToken
          # etc.

      else
        @publishEvent 'loginFail', eventPayload

    getLoginStatus: (callback = @loginStatusHandler, force = false) ->
      ServiceProviderLibrary.getLoginStatus callback, force

    loginStatusHandler: (response) =>
      return unless response
      @publishEvent 'serviceProviderSession',
        provider: this
        userId: response.userId
        accessToken: response.accessToken
        # etc.
  */

  
}});

window.require.define({"lib/support": function(exports, require, module) {
  var Chaplin, support, utils;

  Chaplin = require('chaplin');

  utils = require('lib/utils');

  support = utils.beget(Chaplin.support);

  module.exports = support;
  
}});

window.require.define({"lib/utils": function(exports, require, module) {
  var Chaplin, utils,
    __hasProp = {}.hasOwnProperty;

  Chaplin = require('chaplin');

  utils = Chaplin.utils.beget(Chaplin.utils);

  _(utils).extend({
    /*
      Wrap methods so they can be called before a deferred is resolved.
      The actual methods are called once the deferred is resolved.
    
      Parameters:
    
      Expects an options hash with the following properties:
    
      deferred
        The Deferred object to wait for.
    
      methods
        Either:
        - A string with a method name e.g. 'method'
        - An array of strings e.g. ['method1', 'method2']
        - An object with methods e.g. {method: -> alert('resolved!')}
    
      host (optional)
        If you pass an array of strings in the `methods` parameter the methods
        are fetched from this object. Defaults to `deferred`.
    
      target (optional)
        The target object the new wrapper methods are created at.
        Defaults to host if host is given, otherwise it defaults to deferred.
    
      onDeferral (optional)
        An additional callback function which is invoked when the method is called
        and the Deferred isn't resolved yet.
        After the method is registered as a done handler on the Deferred,
        this callback is invoked. This can be used to trigger the resolving
        of the Deferred.
    
      Examples:
    
      deferMethods(deferred: def, methods: 'foo')
        Wrap the method named foo of the given deferred def and
        postpone all calls until the deferred is resolved.
    
      deferMethods(deferred: def, methods: def.specialMethods)
        Read all methods from the hash def.specialMethods and
        create wrapped methods with the same names at def.
    
      deferMethods(
        deferred: def, methods: def.specialMethods, target: def.specialMethods
      )
        Read all methods from the object def.specialMethods and
        create wrapped methods at def.specialMethods,
        overwriting the existing ones.
    
      deferMethods(deferred: def, host: obj, methods: ['foo', 'bar'])
        Wrap the methods obj.foo and obj.bar so all calls to them are postponed
        until def is resolved. obj.foo and obj.bar are overwritten
        with their wrappers.
    */

    deferMethods: function(options) {
      var deferred, func, host, methods, methodsHash, name, onDeferral, target, _i, _len, _results;
      deferred = options.deferred;
      methods = options.methods;
      host = options.host || deferred;
      target = options.target || host;
      onDeferral = options.onDeferral;
      methodsHash = {};
      if (typeof methods === 'string') {
        methodsHash[methods] = host[methods];
      } else if (methods.length && methods[0]) {
        for (_i = 0, _len = methods.length; _i < _len; _i++) {
          name = methods[_i];
          func = host[name];
          if (typeof func !== 'function') {
            throw new TypeError("utils.deferMethods: method " + name + " notfound on host " + host);
          }
          methodsHash[name] = func;
        }
      } else {
        methodsHash = methods;
      }
      _results = [];
      for (name in methodsHash) {
        if (!__hasProp.call(methodsHash, name)) continue;
        func = methodsHash[name];
        if (typeof func !== 'function') {
          continue;
        }
        _results.push(target[name] = utils.createDeferredFunction(deferred, func, target, onDeferral));
      }
      return _results;
    },
    createDeferredFunction: function(deferred, func, context, onDeferral) {
      if (context == null) {
        context = deferred;
      }
      return function() {
        var args;
        args = arguments;
        if (deferred.state() === 'resolved') {
          return func.apply(context, args);
        } else {
          deferred.done(function() {
            return func.apply(context, args);
          });
          if (typeof onDeferral === 'function') {
            return onDeferral.apply(context);
          }
        }
      };
    }
  });

  module.exports = utils;
  
}});

window.require.define({"lib/view_helper": function(exports, require, module) {
  var mediator, utils;

  mediator = require('mediator');

  utils = require('chaplin/lib/utils');

  Handlebars.registerHelper('if_logged_in', function(options) {
    if (mediator.user) {
      return options.fn(this);
    } else {
      return options.inverse(this);
    }
  });

  Handlebars.registerHelper('with', function(context, options) {
    if (!context || Handlebars.Utils.isEmpty(context)) {
      return options.inverse(this);
    } else {
      return options.fn(context);
    }
  });

  Handlebars.registerHelper('without', function(context, options) {
    var inverse;
    inverse = options.inverse;
    options.inverse = options.fn;
    options.fn = inverse;
    return Handlebars.helpers["with"].call(this, context, options);
  });

  Handlebars.registerHelper('with_user', function(options) {
    var context;
    context = mediator.user || {};
    return Handlebars.helpers["with"].call(this, context, options);
  });
  
}});

window.require.define({"mediator": function(exports, require, module) {
  
  module.exports = require('chaplin').mediator;
  
}});

window.require.define({"models/base/collection": function(exports, require, module) {
  var Chaplin, Collection,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  module.exports = Collection = (function(_super) {

    __extends(Collection, _super);

    function Collection() {
      return Collection.__super__.constructor.apply(this, arguments);
    }

    return Collection;

  })(Chaplin.Collection);
  
}});

window.require.define({"models/base/model": function(exports, require, module) {
  var Chaplin, Model,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  module.exports = Model = (function(_super) {

    __extends(Model, _super);

    function Model() {
      return Model.__super__.constructor.apply(this, arguments);
    }

    return Model;

  })(Chaplin.Model);
  
}});

window.require.define({"models/header": function(exports, require, module) {
  var Header, Model,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Model = require('models/base/model');

  module.exports = Header = (function(_super) {

    __extends(Header, _super);

    function Header() {
      return Header.__super__.constructor.apply(this, arguments);
    }

    return Header;

  })(Model);
  
}});

window.require.define({"models/navigation": function(exports, require, module) {
  var Model, Navigation,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Model = require('models/base/model');

  module.exports = Navigation = (function(_super) {

    __extends(Navigation, _super);

    function Navigation() {
      return Navigation.__super__.constructor.apply(this, arguments);
    }

    return Navigation;

  })(Model);
  
}});

window.require.define({"models/profiler": function(exports, require, module) {
  var HttpClient, Model, Profiler, ProfilingService,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Model = require('models/base/model');

  HttpClient = (function() {

    function HttpClient(baseUrl) {
      this.baseUrl = baseUrl;
    }

    HttpClient.prototype.get = function(path, params, cb) {
      return this._http("GET", path, params, cb);
    };

    HttpClient.prototype.put = function(path, data, cb) {
      return this._http("PUT", path, data, cb);
    };

    HttpClient.prototype._http = function(method, path, data, cb) {
      if (data == null) {
        data = {};
      }
      if (cb == null) {
        cb = (function() {});
      }
      return $.ajax({
        url: this.baseUrl + path,
        type: method,
        data: data,
        timeout: 1000 * 60 * 60 * 6,
        cache: false,
        processData: method === "GET",
        contentType: "application/json",
        success: cb,
        error: cb,
        dataType: "json"
      });
    };

    return HttpClient;

  })();

  ProfilingService = (function() {

    function ProfilingService() {
      this._onRecievedEvents = __bind(this._onRecievedEvents, this);

      this._onServerProfilingStarted = __bind(this._onServerProfilingStarted, this);

      this._poll = __bind(this._poll, this);
      this._http = new HttpClient("" + location.protocol + "//" + location.host + "/db/manage");
      this._lastPoll = 0;
    }

    ProfilingService.prototype.attachListener = function(listener) {
      this._http.put("/server/profile/start", {}, this._onServerProfilingStarted);
      return this._listener = listener;
    };

    ProfilingService.prototype.stop = function() {
      if (this._timer != null) {
        this._lastPoll = 0;
        clearInterval(this._timer);
        delete this._timer;
        return this._http.put("/server/profile/stop", {});
      }
    };

    ProfilingService.prototype._poll = function() {
      return this._http.get("/server/profile/fetch/" + this._lastPoll, {}, this._onRecievedEvents);
    };

    ProfilingService.prototype._onServerProfilingStarted = function() {
      if (!(this._timer != null)) {
        return this._timer = setInterval(this._poll, 1000);
      }
    };

    ProfilingService.prototype._onRecievedEvents = function(events) {
      if (events.length > 0) {
        this._lastPoll = events[0].timestamp;
        return this._listener(events);
      }
    };

    return ProfilingService;

  })();

  module.exports = Profiler = (function(_super) {

    __extends(Profiler, _super);

    Profiler.State = {
      STOPPED: 0,
      RUNNING: 1,
      PAUSED: 2
    };

    Profiler.prototype.defaults = {
      state: Profiler.State.STOPPED,
      events: []
    };

    function Profiler() {
      this._onNewProfilingEvents = __bind(this._onNewProfilingEvents, this);
      Profiler.__super__.constructor.apply(this, arguments);
      this._profilingService = new ProfilingService();
      this.eventLimit = 1000;
    }

    Profiler.prototype.getState = function() {
      return this.get('state');
    };

    Profiler.prototype.startProfiling = function() {
      this.set({
        state: Profiler.State.RUNNING
      });
      return this._profilingService.attachListener(this._onNewProfilingEvents);
    };

    Profiler.prototype.stopProfiling = function() {
      this.set({
        "events": []
      });
      this._profilingService.stop();
      return this.set({
        state: Profiler.State.STOPPED
      });
    };

    Profiler.prototype.pauseProfiling = function() {
      this._profilingService.stop();
      return this.set({
        state: Profiler.State.PAUSED
      });
    };

    Profiler.prototype._onNewProfilingEvents = function(newEvents) {
      var events, spliceArgs;
      events = this.get('events');
      spliceArgs = [0, 0].concat(newEvents);
      events.splice.apply(events, spliceArgs);
      if (events.length > this.eventLimit) {
        events.splice(this.eventLimit, events.length - this.eventLimit);
      }
      return this.trigger('change:events', events);
    };

    return Profiler;

  })(Model);
  
}});

window.require.define({"models/sidebar": function(exports, require, module) {
  var Model, Sidebar,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Model = require('models/base/model');

  module.exports = Sidebar = (function(_super) {

    __extends(Sidebar, _super);

    function Sidebar() {
      return Sidebar.__super__.constructor.apply(this, arguments);
    }

    Sidebar.prototype.defaults = {
      items: [
        {
          href: '/',
          title: 'Data',
          icon: 'th'
        }, {
          href: '/profiler/',
          title: 'Profiler',
          icon: 'signal'
        }, {
          href: '/console/',
          title: 'Console',
          icon: 'list-alt'
        }, {
          href: '/indexes/',
          title: 'Indexes',
          icon: 'filter'
        }, {
          href: '/info/',
          title: 'Server info',
          icon: 'wrench'
        }
      ]
    };

    Sidebar.prototype.setCurrentPattern = function(pattern) {
      var item, items, _i, _len;
      items = this.get('items');
      for (_i = 0, _len = items.length; _i < _len; _i++) {
        item = items[_i];
        item.current = item.href.slice(1) === pattern;
      }
      this.set({
        items: items
      });
      return this.trigger("change:items", items);
    };

    return Sidebar;

  })(Model);
  
}});

window.require.define({"models/user": function(exports, require, module) {
  var Model, User,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Model = require('models/base/model');

  module.exports = User = (function(_super) {

    __extends(User, _super);

    function User() {
      return User.__super__.constructor.apply(this, arguments);
    }

    return User;

  })(Model);
  
}});

window.require.define({"routes": function(exports, require, module) {
  
  module.exports = function(match) {
    match('', 'home#index');
    match('profiler/', 'profiler#index');
    match('console/', 'console#index');
    match('indexes/', 'indexes#index');
    return match('info/', 'info#index');
  };
  
}});

window.require.define({"views/base/collection_view": function(exports, require, module) {
  var Chaplin, CollectionView, View,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  View = require('views/base/view');

  module.exports = CollectionView = (function(_super) {

    __extends(CollectionView, _super);

    function CollectionView() {
      return CollectionView.__super__.constructor.apply(this, arguments);
    }

    CollectionView.prototype.getTemplateFunction = View.prototype.getTemplateFunction;

    return CollectionView;

  })(Chaplin.CollectionView);
  
}});

window.require.define({"views/base/page_view": function(exports, require, module) {
  var PageView, View,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  View = require('views/base/view');

  module.exports = PageView = (function(_super) {

    __extends(PageView, _super);

    function PageView() {
      return PageView.__super__.constructor.apply(this, arguments);
    }

    PageView.prototype.container = '#page-container';

    PageView.prototype.autoRender = true;

    PageView.prototype.renderedSubviews = false;

    PageView.prototype.initialize = function() {
      var rendered,
        _this = this;
      PageView.__super__.initialize.apply(this, arguments);
      if (this.model || this.collection) {
        rendered = false;
        return this.modelBind('change', function() {
          if (!rendered) {
            _this.render();
          }
          return rendered = true;
        });
      }
    };

    PageView.prototype.renderSubviews = function() {};

    PageView.prototype.render = function() {
      PageView.__super__.render.apply(this, arguments);
      if (!this.renderedSubviews) {
        this.renderSubviews();
        return this.renderedSubviews = true;
      }
    };

    return PageView;

  })(View);
  
}});

window.require.define({"views/base/view": function(exports, require, module) {
  var Chaplin, View,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  require('lib/view_helper');

  module.exports = View = (function(_super) {

    __extends(View, _super);

    function View() {
      return View.__super__.constructor.apply(this, arguments);
    }

    View.prototype.getTemplateFunction = function() {
      return this.template;
    };

    return View;

  })(Chaplin.View);
  
}});

window.require.define({"views/console_view": function(exports, require, module) {
  var HomePageView, PageView, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  template = require('views/templates/console');

  PageView = require('views/base/page_view');

  module.exports = HomePageView = (function(_super) {

    __extends(HomePageView, _super);

    function HomePageView() {
      return HomePageView.__super__.constructor.apply(this, arguments);
    }

    HomePageView.prototype.template = template;

    HomePageView.prototype.className = 'console-page';

    return HomePageView;

  })(PageView);
  
}});

window.require.define({"views/header_view": function(exports, require, module) {
  var HeaderView, View, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  View = require('views/base/view');

  template = require('views/templates/header');

  module.exports = HeaderView = (function(_super) {

    __extends(HeaderView, _super);

    function HeaderView() {
      return HeaderView.__super__.constructor.apply(this, arguments);
    }

    HeaderView.prototype.template = template;

    HeaderView.prototype.id = 'header';

    HeaderView.prototype.className = 'header';

    HeaderView.prototype.container = '#header-container';

    HeaderView.prototype.autoRender = true;

    HeaderView.prototype.initialize = function() {
      HeaderView.__super__.initialize.apply(this, arguments);
      this.subscribeEvent('startupController', this.render);
      return this.modelBind('change:items', this.onHeaderModelChanged);
    };

    HeaderView.prototype.onHeaderModelChanged = function() {
      return console.log(arguments);
    };

    return HeaderView;

  })(View);
  
}});

window.require.define({"views/home_page_view": function(exports, require, module) {
  var HomePageView, PageView, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  template = require('views/templates/home');

  PageView = require('views/base/page_view');

  module.exports = HomePageView = (function(_super) {

    __extends(HomePageView, _super);

    function HomePageView() {
      return HomePageView.__super__.constructor.apply(this, arguments);
    }

    HomePageView.prototype.template = template;

    HomePageView.prototype.className = 'home-page';

    return HomePageView;

  })(PageView);
  
}});

window.require.define({"views/indexes_view": function(exports, require, module) {
  var IndexesView, PageView, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  template = require('views/templates/indexes');

  PageView = require('views/base/page_view');

  module.exports = IndexesView = (function(_super) {

    __extends(IndexesView, _super);

    function IndexesView() {
      return IndexesView.__super__.constructor.apply(this, arguments);
    }

    IndexesView.prototype.template = template;

    IndexesView.prototype.className = 'indexes-page';

    return IndexesView;

  })(PageView);
  
}});

window.require.define({"views/info_view": function(exports, require, module) {
  var InfoView, PageView, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  template = require('views/templates/info');

  PageView = require('views/base/page_view');

  module.exports = InfoView = (function(_super) {

    __extends(InfoView, _super);

    function InfoView() {
      return InfoView.__super__.constructor.apply(this, arguments);
    }

    InfoView.prototype.template = template;

    InfoView.prototype.className = 'indexes-page';

    return InfoView;

  })(PageView);
  
}});

window.require.define({"views/layout": function(exports, require, module) {
  var Chaplin, Layout,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  module.exports = Layout = (function(_super) {

    __extends(Layout, _super);

    function Layout() {
      return Layout.__super__.constructor.apply(this, arguments);
    }

    Layout.prototype.initialize = function() {
      return Layout.__super__.initialize.apply(this, arguments);
    };

    return Layout;

  })(Chaplin.Layout);
  
}});

window.require.define({"views/login_view": function(exports, require, module) {
  var LoginView, View, template, utils,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  utils = require('lib/utils');

  View = require('views/base/view');

  template = require('views/templates/login');

  module.exports = LoginView = (function(_super) {

    __extends(LoginView, _super);

    function LoginView() {
      return LoginView.__super__.constructor.apply(this, arguments);
    }

    LoginView.prototype.template = template;

    LoginView.prototype.id = 'login';

    LoginView.prototype.container = '#content-container';

    LoginView.prototype.autoRender = true;

    LoginView.prototype.initialize = function(options) {
      LoginView.__super__.initialize.apply(this, arguments);
      return this.initButtons(options.serviceProviders);
    };

    LoginView.prototype.initButtons = function(serviceProviders) {
      var buttonSelector, failed, loaded, loginHandler, serviceProvider, serviceProviderName, _results;
      _results = [];
      for (serviceProviderName in serviceProviders) {
        serviceProvider = serviceProviders[serviceProviderName];
        buttonSelector = "." + serviceProviderName;
        this.$(buttonSelector).addClass('service-loading');
        loginHandler = _(this.loginWith).bind(this, serviceProviderName, serviceProvider);
        this.delegate('click', buttonSelector, loginHandler);
        loaded = _(this.serviceProviderLoaded).bind(this, serviceProviderName, serviceProvider);
        serviceProvider.done(loaded);
        failed = _(this.serviceProviderFailed).bind(this, serviceProviderName, serviceProvider);
        _results.push(serviceProvider.fail(failed));
      }
      return _results;
    };

    LoginView.prototype.loginWith = function(serviceProviderName, serviceProvider, event) {
      event.preventDefault();
      if (!serviceProvider.isLoaded()) {
        return;
      }
      this.publishEvent('login:pickService', serviceProviderName);
      return this.publishEvent('!login', serviceProviderName);
    };

    LoginView.prototype.serviceProviderLoaded = function(serviceProviderName) {
      return this.$("." + serviceProviderName).removeClass('service-loading');
    };

    LoginView.prototype.serviceProviderFailed = function(serviceProviderName) {
      return this.$("." + serviceProviderName).removeClass('service-loading').addClass('service-unavailable').attr('disabled', true).attr('title', "Error connecting. Please check whether you areblocking " + (utils.upcase(serviceProviderName)) + ".");
    };

    return LoginView;

  })(View);
  
}});

window.require.define({"views/profiler/controls_view": function(exports, require, module) {
  var Profiler, ProfilerControlsView, View, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  template = require('views/templates/profiler/controls');

  View = require('views/base/view');

  Profiler = require('models/profiler');

  module.exports = ProfilerControlsView = (function(_super) {

    __extends(ProfilerControlsView, _super);

    function ProfilerControlsView() {
      return ProfilerControlsView.__super__.constructor.apply(this, arguments);
    }

    ProfilerControlsView.prototype.template = template;

    ProfilerControlsView.prototype.events = {
      "click .profiler-stop": "onStopProfilingClicked",
      "click .profiler-pause": "onPauseProfilingClicked",
      "click .profiler-resume": "onResumeProfilingClicked"
    };

    ProfilerControlsView.prototype.initialize = function() {
      var _this = this;
      ProfilerControlsView.__super__.initialize.apply(this, arguments);
      return this.model.on("change", function() {
        return _this.render();
      });
    };

    ProfilerControlsView.prototype.getTemplateData = function() {
      if (this.model.getState() === Profiler.State.STOPPED) {
        return {
          disabled: true
        };
      } else if (this.model.getState() === Profiler.State.PAUSED) {
        return {
          paused: true
        };
      } else {
        return this.model.serialize();
      }
    };

    ProfilerControlsView.prototype.onStopProfilingClicked = function() {
      return this.model.stopProfiling();
    };

    ProfilerControlsView.prototype.onPauseProfilingClicked = function() {
      return this.model.pauseProfiling();
    };

    ProfilerControlsView.prototype.onResumeProfilingClicked = function() {
      return this.model.startProfiling();
    };

    return ProfilerControlsView;

  })(View);
  
}});

window.require.define({"views/profiler/profiler_view": function(exports, require, module) {
  var PageView, Profiler, ProfilerControlsView, ProfilerView, ProfilerWorkspaceView, template,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  template = require('views/templates/profiler/profiler');

  PageView = require('views/base/page_view');

  ProfilerControlsView = require('views/profiler/controls_view');

  ProfilerWorkspaceView = require('views/profiler/workspace_view');

  Profiler = require('models/profiler');

  module.exports = ProfilerView = (function(_super) {

    __extends(ProfilerView, _super);

    function ProfilerView() {
      this.renderSubviews = __bind(this.renderSubviews, this);
      return ProfilerView.__super__.constructor.apply(this, arguments);
    }

    ProfilerView.prototype.className = 'profiler-page';

    ProfilerView.prototype.template = template;

    ProfilerView.prototype.initialize = function(opts) {
      ProfilerView.__super__.initialize.apply(this, arguments);
      return this.profiler = opts.profiler;
    };

    ProfilerView.prototype.renderSubviews = function() {
      this.workspace = new ProfilerWorkspaceView({
        model: this.profiler,
        el: this.$("#profiler-workspace")
      });
      this.controls = new ProfilerControlsView({
        model: this.profiler,
        el: this.$("#profiler-controls")
      });
      this.workspace.render();
      return this.controls.render();
    };

    return ProfilerView;

  })(PageView);
  
}});

window.require.define({"views/profiler/workspace_view": function(exports, require, module) {
  var Profiler, ProfilerWorkspaceView, View, running_template, start_template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  start_template = require('views/templates/profiler/start');

  running_template = require('views/templates/profiler/running');

  View = require('views/base/view');

  Profiler = require('models/profiler');

  module.exports = ProfilerWorkspaceView = (function(_super) {

    __extends(ProfilerWorkspaceView, _super);

    function ProfilerWorkspaceView() {
      return ProfilerWorkspaceView.__super__.constructor.apply(this, arguments);
    }

    ProfilerWorkspaceView.prototype.events = {
      "click .profiling-start": "onStartProfilingClicked"
    };

    ProfilerWorkspaceView.prototype.initialize = function() {
      var _this = this;
      ProfilerWorkspaceView.__super__.initialize.apply(this, arguments);
      this.model.on("change:state", function() {
        return _this.render();
      });
      return this.model.on("change:events", function() {
        return _this.render();
      });
    };

    ProfilerWorkspaceView.prototype.getTemplateFunction = function() {
      console.log(this.model, this.model.getState());
      switch (this.model.getState()) {
        case Profiler.State.STOPPED:
          return start_template;
        case Profiler.State.RUNNING:
          return running_template;
        case Profiler.State.PAUSED:
          return running_template;
      }
    };

    ProfilerWorkspaceView.prototype.onStartProfilingClicked = function() {
      return this.model.startProfiling();
    };

    return ProfilerWorkspaceView;

  })(View);
  
}});

window.require.define({"views/sidebar_view": function(exports, require, module) {
  var SidebarView, View, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  View = require('views/base/view');

  template = require('views/templates/sidebar');

  module.exports = SidebarView = (function(_super) {

    __extends(SidebarView, _super);

    function SidebarView() {
      return SidebarView.__super__.constructor.apply(this, arguments);
    }

    SidebarView.prototype.template = template;

    SidebarView.prototype.id = 'sidebar';

    SidebarView.prototype.className = 'sidebar';

    SidebarView.prototype.container = '#sidebar-container';

    SidebarView.prototype.autoRender = true;

    SidebarView.prototype.initialize = function() {
      SidebarView.__super__.initialize.apply(this, arguments);
      this.subscribeEvent('startupController', this.render);
      return this.modelBind('change:items', this.onSidebarChanged);
    };

    SidebarView.prototype.onSidebarChanged = function() {
      return this.render();
    };

    return SidebarView;

  })(View);
  
}});

window.require.define({"views/templates/console": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<h1>Console!</h1>";});
}});

window.require.define({"views/templates/header": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<div class=\"navbar-inner\">\n  <div class=\"container-fluid\">\n    <a class=\"brand\" href=\"/\"><img src=\"img/logo.png\" /></a>\n    \n    <ul class=\"nav pull-right\">\n      <!--li>\n        <div class=\"btn-group\">\n          <a class=\"btn btn-mini header-mini-btn\" data-toggle=\"dropdown\" href=\"#\">\n            Settings\n          </a>\n          <button class=\"btn btn-mini header-mini-btn\">Logged in</button>\n        </div>\n      </li-->\n    </ul>\n  </div>\n</div>";});
}});

window.require.define({"views/templates/home": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<div class=\"row-fluid\">\n  <div class=\"span12\">\n    <div class=\"hero-unit hero-unit-inverse neo-19-announcement-hero \">\n      <h1>Welcome to Neo4j 1.9!</h1>\n      <p>With massive [REDACTED] for your [REDACTED] and support for dead-simple [REDACTED], Neo4j 1.9 raises the bar to a whole new level.</p>\n      <p><a class=\"btn btn-primary btn-large\">Learn more &raquo;</a></p>\n    </div>\n    <div class=\"row-fluid\">\n      <div class=\"span4\">\n        <h2>New to Neo4j?</h2>\n        <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>\n        <p><a class=\"btn\" href=\"#\">View details &raquo;</a></p>\n      </div><!--/span-->\n      <div class=\"span4\">\n        <h2>Labels!</h2>\n        <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>\n        <p><a class=\"btn\" href=\"#\">View details &raquo;</a></p>\n      </div><!--/span-->\n      <div class=\"span4\">\n        <h2>Cypher performance</h2>\n        <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>\n        <p><a class=\"btn\" href=\"#\">View details &raquo;</a></p>\n      </div><!--/span-->\n    </div><!--/row-->\n  </div><!--/span-->\n</div><!--/row-->\n\n<hr>\n\n<footer>\n  <p>&copy; Neo Technology 2012</p>\n</footer>";});
}});

window.require.define({"views/templates/indexes": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<h1>Indexes!</h1>";});
}});

window.require.define({"views/templates/info": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<h1>Info!</h1>";});
}});

window.require.define({"views/templates/login": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", foundHelper, self=this;


    return buffer;});
}});

window.require.define({"views/templates/profiler/controls": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, foundHelper, tmp1, self=this, functionType="function", blockHelperMissing=helpers.blockHelperMissing;

  function program1(depth0,data) {
    
    
    return "\n            <div class=\"btn-group\">\n              <button class=\"btn profiler-resume\" disabled>Resume</button>\n              <button class=\"btn profiler-pause\" disabled>Pause</button>\n              <button class=\"btn btn-danger\" disabled>Stop</button>\n            </div>\n        ";}

  function program3(depth0,data) {
    
    var buffer = "", stack1;
    buffer += "\n          <div class=\"btn-group\">\n            <button class=\"btn profiler-resume\" ";
    foundHelper = helpers.paused;
    stack1 = foundHelper || depth0.paused;
    tmp1 = self.noop;
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.program(4, program4, data);
    if(foundHelper && typeof stack1 === functionType) { stack1 = stack1.call(depth0, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += ">Resume</button>\n            <button class=\"btn profiler-pause\" ";
    foundHelper = helpers.paused;
    stack1 = foundHelper || depth0.paused;
    tmp1 = self.program(6, program6, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack1 === functionType) { stack1 = stack1.call(depth0, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += ">Pause</button>\n            <button class=\"btn btn-danger profiler-stop\">Stop</button>\n          </div>\n        ";
    return buffer;}
  function program4(depth0,data) {
    
    
    return "disabled";}

  function program6(depth0,data) {
    
    
    return "disabled";}

    buffer += "<div class=\"navbar\">\n  <div class=\"navbar-inner\">\n    \n    <div class=\"container\">\n      <a class=\"brand\">Profiler</a>\n      <form class=\"navbar-form pull-left\" action=\"\">\n        ";
    foundHelper = helpers.disabled;
    stack1 = foundHelper || depth0.disabled;
    tmp1 = self.program(1, program1, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack1 === functionType) { stack1 = stack1.call(depth0, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n        ";
    foundHelper = helpers.disabled;
    stack1 = foundHelper || depth0.disabled;
    tmp1 = self.noop;
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.program(3, program3, data);
    if(foundHelper && typeof stack1 === functionType) { stack1 = stack1.call(depth0, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n      </form>\n    </div>\n  </div>\n</div>";
    return buffer;});
}});

window.require.define({"views/templates/profiler/profiler": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<div id=\"profiler-controls\"></div>\n<div class=\"row-fluid\" id=\"profiler-workspace\"></div>";});
}});

window.require.define({"views/templates/profiler/running": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, foundHelper, tmp1, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression, blockHelperMissing=helpers.blockHelperMissing;

  function program1(depth0,data) {
    
    var buffer = "", stack1;
    buffer += "\n    <tr>\n      <td>";
    foundHelper = helpers.timestamp;
    stack1 = foundHelper || depth0.timestamp;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "timestamp", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</td>\n      <td>";
    foundHelper = helpers.query;
    stack1 = foundHelper || depth0.query;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "query", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</td>\n      <td>";
    foundHelper = helpers.wasCached;
    stack1 = foundHelper || depth0.wasCached;
    tmp1 = self.program(2, program2, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack1 === functionType) { stack1 = stack1.call(depth0, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    foundHelper = helpers.wasCached;
    stack1 = foundHelper || depth0.wasCached;
    tmp1 = self.noop;
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.program(4, program4, data);
    if(foundHelper && typeof stack1 === functionType) { stack1 = stack1.call(depth0, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "</td>\n      <td>";
    foundHelper = helpers.parseTime;
    stack1 = foundHelper || depth0.parseTime;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "parseTime", { hash: {} }); }
    buffer += escapeExpression(stack1) + "ms</td>\n    </tr>\n    ";
    return buffer;}
  function program2(depth0,data) {
    
    
    return "yes";}

  function program4(depth0,data) {
    
    
    return "no";}

    buffer += "<div class=\"container\">\n<h4>Events</h4>\n<table class=\"table\">\n  <thead>\n    <tr>\n      <th>Timestamp</th>\n      <th>Query</th>\n      <th>Cached</th>\n      <th>Parse time</th>\n    </tr>\n  </thead>\n  <tbody>\n    ";
    foundHelper = helpers.events;
    stack1 = foundHelper || depth0.events;
    tmp1 = self.program(1, program1, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack1 === functionType) { stack1 = stack1.call(depth0, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n  </tbody>\n</table>\n</div>";
    return buffer;});
}});

window.require.define({"views/templates/profiler/start": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<div class=\"row-fluid centerpiece\">\n  <div class=\"span4\"></div>\n  <div class=\"span4\">\n    <h1>Profiler</h1>\n    <p>To help you find out how your graph is feeling today.</p>\n    <p><button class=\"btn profiling-start btn-primary\">Start profiling &raquo;</button></p>\n  </div><!--/span-->\n</div><!--/row-->";});
}});

window.require.define({"views/templates/sidebar": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, foundHelper, tmp1, self=this, functionType="function", blockHelperMissing=helpers.blockHelperMissing, helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression;

  function program1(depth0,data) {
    
    var buffer = "", stack1;
    buffer += "\n    <li ";
    foundHelper = helpers.current;
    stack1 = foundHelper || depth0.current;
    tmp1 = self.program(2, program2, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack1 === functionType) { stack1 = stack1.call(depth0, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "><a href=\"#";
    foundHelper = helpers.href;
    stack1 = foundHelper || depth0.href;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "href", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\"><span class=\"icon icon-";
    foundHelper = helpers.icon;
    stack1 = foundHelper || depth0.icon;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "icon", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\"></span>";
    foundHelper = helpers.title;
    stack1 = foundHelper || depth0.title;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "title", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</a></li>\n  ";
    return buffer;}
  function program2(depth0,data) {
    
    
    return "class=\"active\"";}

    buffer += "<p>\n  <span class=\"\">1.9 Community Edition</span>\n  <!--a class=\"btn btn-link btn-mini\">Upgrade!</a-->\n</p>\n<ul class=\"main-menu\">\n  ";
    foundHelper = helpers.items;
    stack1 = foundHelper || depth0.items;
    tmp1 = self.program(1, program1, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack1 === functionType) { stack1 = stack1.call(depth0, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n</ul>\n<p class=\"sidebar-resources\">\n  <small>Resources</small>\n  <br />\n  <a href=\"http://docs.neo4j.org/chunked/1.8/tutorials-rest.html\" class=\"btn btn-link btn-mini\">Getting started</a>\n  <a href=\"http://docs.neo4j.org/chunked/1.8/\" class=\"btn btn-link btn-mini\">Documentation</a>\n  <a href=\"https://github.com/neo4j/neo4j/issues/new\" class=\"btn btn-link btn-mini\">Report an issue</a>\n</p>";
    return buffer;});
}});

