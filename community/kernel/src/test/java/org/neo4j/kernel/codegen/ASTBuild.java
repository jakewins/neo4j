package org.neo4j.kernel.codegen;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.FINAL_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PRIVATE_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PUBLIC_KEYWORD;
import static org.neo4j.kernel.codegen.ASTType.ensureNoTypeParams;
import static org.neo4j.kernel.codegen.ASTType.type;

/**
 * Higher level methods for creating common patterns, like assignments and methods.
 */
public class ASTBuild
{
    private final AST ast;

    public ASTBuild( AST ast )
    {
        this.ast = ast;
    }

    public FieldDeclaration newField( String type, String name)
    {
        VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
        frag.setName( ast.newSimpleName( name ) );
        FieldDeclaration field = ast.newFieldDeclaration( frag );

        field.setType( type( type, ast ) );
        field.modifiers().add( ast.newModifier( PRIVATE_KEYWORD ) );
        field.modifiers().add( ast.newModifier(FINAL_KEYWORD) );
        return field;
    }

    /**
     * Build a method that throws UnsupportedOperation, given a java reflection method. Useful for implementing
     * interfaces and so on.
     */
    public MethodDeclaration newUnsupportedMethod( Method method )
    {
        MethodDeclaration declaration = newEmptyMethod( method.getName(), PUBLIC_KEYWORD, FINAL_KEYWORD );
        declaration.setReturnType2( type( method.getReturnType().getCanonicalName(), ast ) );

        int count = 0;
        for ( Class<?> param : method.getParameterTypes() )
        {
            SingleVariableDeclaration e = ast.newSingleVariableDeclaration();
            e.setName( ast.newSimpleName( "_ignore" + count ) );
            e.setType( type( param.getCanonicalName(), ast ) );
            declaration.parameters().add( e );
            count++;
        }

        ThrowStatement throwIt = ast.newThrowStatement();
        ClassInstanceCreation newUnsupportedOperation = ast.newClassInstanceCreation();
        newUnsupportedOperation.setType( type( UnsupportedOperationException.class, ast ) );
        throwIt.setExpression( newUnsupportedOperation );
        declaration.getBody().statements().add( throwIt );
        return declaration;
    }

    public MethodDeclaration newEmptyMethod(String name, Modifier.ModifierKeyword ... modifiers)
    {
        MethodDeclaration method = ast.newMethodDeclaration();
        method.setName( ast.newSimpleName( name ) );
        for ( Modifier.ModifierKeyword modifier : modifiers )
        {
            method.modifiers().add( ast.newModifier( modifier ) );
        }
        method.setBody( ast.newBlock() );
        return method;
    }

    public FieldAccess newFieldAccess(String fieldName)
    {
        FieldAccess fieldAccess = ast.newFieldAccess();
        fieldAccess.setExpression( ast.newThisExpression() );
        fieldAccess.setName( ast.newSimpleName( fieldName ) );
        return fieldAccess;
    }

    public Map<String, Type> fieldTypeMap( TypeDeclaration node )
    {
        Map<String, Type> fieldTypes = new HashMap<>();
        for ( FieldDeclaration fieldDeclaration : node.getFields() )
        {
            for ( Object o : fieldDeclaration.fragments() )
            {
                fieldTypes.put( o.toString(), ensureNoTypeParams( fieldDeclaration.getType() ) );
            }
        }
        return fieldTypes;
    }

    /** Build a list of variable name -> type for a method, including vars in body and in arguments */
    public Map<String, Type> variableTypeMap( MethodDeclaration node )
    {
        final Map<String, Type> types = new HashMap<>();
        node.accept( new ASTVisitor()
        {
            @Override
            public boolean visit( VariableDeclarationStatement node )
            {
                for ( Object o : node.fragments() )
                {
                    types.put( ((VariableDeclarationFragment) o).getName().toString(),
                            ensureNoTypeParams(node.getType()) );
                }
                return true;
            }
        });
        for ( Object o : node.parameters() )
        {
            SingleVariableDeclaration var = (SingleVariableDeclaration)o;
            types.put( var.getName().toString(), var.getType() );
        }

        return types;
    }
}
