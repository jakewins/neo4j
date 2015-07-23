
function ES6ToES5( script, name ) {
    try {
        return traceur.Compiler.script(script);
    }
    catch (e) {
        print(e);
    }
}