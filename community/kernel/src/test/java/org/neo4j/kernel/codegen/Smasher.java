package org.neo4j.kernel.codegen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.kernel.impl.api.ConstraintEnforcingEntityOperations;
import org.neo4j.kernel.impl.api.StateHandlingStatementOperations;
import org.neo4j.kernel.impl.api.operations.StatementLayer;

import static java.util.Arrays.asList;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.FINAL_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PRIVATE_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PUBLIC_KEYWORD;
import static org.eclipse.jdt.core.dom.TypeDeclaration.MODIFIERS2_PROPERTY;
import static org.neo4j.helpers.collection.MapUtil.stringMap;

public class Smasher
{


    static class Generator
    {
        private final File sourceDir;
        private final File outputDir;
        private final Class<?> commonInterface;

        private Class<?>[] layers;

        Generator( File sourceDir, File outputDir, Class<?> commonInterface )
        {
            this.sourceDir = sourceDir;
            this.outputDir = outputDir;
            this.commonInterface = commonInterface;
        }

        enum ReadState
        {
            BEGIN,
            FIELD_SECTION,
            BODY,
            EOF;
        }

        public Generator stack( Class<?> ... layers )
        {
            this.layers = layers;
            return this;
        }

        public void generate() throws IOException, BadLocationException
        {
            final List<Class<?>> processedLayers = new ArrayList<>();

            final Map<String, Class<?>> currentDelegates = new HashMap<>();

            for ( final Class<?> layer : Iterables.reverse( asList( layers ) ) )
            {
                final Document document = open( layer );
                final CompilationUnit cu = parse( document );
                final AST ast = cu.getAST();
                final ASTRewrite rewriter = ASTRewrite.create( ast );
                final Map<MethodDescriptor, Method> requiredMethods = requiredMethods();
                final AtomicBoolean contructorDefined = new AtomicBoolean( false );

                cu.accept( new ASTVisitor()
                {
                    @Override
                    public boolean visit( TypeDeclaration node )
                    {
                        if(node.getName().toString().equals( layer.getSimpleName() ))
                        {
                            //
                            // Rewrite the beginning of the class
                            // * Remove any abstract modifier
                            // * rename the class
                            // * add references to delegates

                            ListRewrite listRewrite = rewriter.getListRewrite( node, MODIFIERS2_PROPERTY );
                            setModifiers( listRewrite, ast, PUBLIC_KEYWORD, FINAL_KEYWORD );
                            rewriter.set(node, TypeDeclaration.NAME_PROPERTY, ast.newSimpleName( "Generated" + layer.getSimpleName() ), null);


                            ListRewrite bodyRewrite = rewriter.getListRewrite( node, node.getBodyDeclarationsProperty() );
                            for ( Class<?> delegateLayer : processedLayers )
                            {
                                VariableDeclarationFragment frag = ast
                                        .newVariableDeclarationFragment();
                                frag.setName( ast.newSimpleName( "__" + delegateLayer.getSimpleName() ) );
                                FieldDeclaration field = ast.newFieldDeclaration( frag );

                                field.setType( type( commonInterface, ast ) );
                                field.modifiers().add( ast.newModifier( PRIVATE_KEYWORD ) );
                                field.modifiers().add( ast.newModifier(FINAL_KEYWORD) );
                                bodyRewrite.insertFirst( field, null );
                            }

                        }
                        return true;
                    }

                    @Override
                    public boolean visit( MethodDeclaration node )
                    {
                        requiredMethods.remove( new MethodDescriptor( node ) );

                        if(node.isConstructor())
                        {
                            contructorDefined.set( true );
                            augmentLayerConstructor( node, rewriter, processedLayers, ast );
                        }

                        String delegate = "None";
                        if(!currentDelegates.containsKey( node.getName().toString() ))
                        {
                            currentDelegates.put( node.getName().toString(), layer );
                        }
                        else
                        {
                            delegate = currentDelegates.get(node.getName().toString()).getSimpleName();
                        }
                        final String delegateField = "__" + delegate;
                        node.getBody().accept( new ASTVisitor()
                        {

                            @Override
                            public boolean visit( MethodInvocation node )
                            {
                                if ( node.getName().toString().equals( "delegate" ) )
                                {
                                    FieldAccess fieldAccess = ast.newFieldAccess();
                                    fieldAccess.setExpression( ast.newThisExpression() );
                                    fieldAccess.setName( ast.newSimpleName( delegateField ) );
                                    rewriter.replace( node, fieldAccess, null );
                                }
                                return true;
                            }
                        });
                        currentDelegates.put( node.getName().toString(), layer );

                        return true;
                    }

                    @Override
                    public void endVisit( TypeDeclaration node )
                    {
                        if(node.getName().toString().equals( layer.getSimpleName() ))
                        {
                            ListRewrite listRewrite = rewriter.getListRewrite(node, node.getBodyDeclarationsProperty());
                            for ( Method method : requiredMethods.values() )
                            {
                                listRewrite.insertLast( newUnsupportedMethod( method, ast ), null );
                            }

                            if(!contructorDefined.get())
                            {
                                MethodDeclaration construct = ast.newMethodDeclaration();
                                construct.setConstructor( true );
                                construct.setName( ast.newSimpleName( node.getName().toString() ) );
                                construct.modifiers().add( ast.newModifier( PUBLIC_KEYWORD ) );
                                construct.setBody( ast.newBlock() );
                                augmentLayerConstructor( construct, rewriter, processedLayers, ast );

                                listRewrite.insertFirst( construct, null );

                            }
                        }
                    }
                });


                try
                {
                    rewriter.rewriteAST( document, stringMap(
                            DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4",
                            DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, "4",
                            DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE ) ).apply( document );
                }
                catch ( BadLocationException e )
                {
                    throw new RuntimeException( e );
                }

                processedLayers.add( layer );
                System.out.println( document.get().toCharArray() );
            }
        }

        private void augmentLayerConstructor( MethodDeclaration node, ASTRewrite rewriter, List<Class<?>> processedLayers, AST ast )
        {
            ListRewrite params = rewriter.getListRewrite( node,
                    MethodDeclaration.PARAMETERS_PROPERTY );
            for ( Class<?> processedLayer : processedLayers )
            {
                SingleVariableDeclaration vrb = ast
                        .newSingleVariableDeclaration();
                vrb.setName( ast.newSimpleName( "__" + processedLayer.getSimpleName() ) );
                vrb.setType( type( processedLayer, ast ) );
                params.insertFirst( vrb, null );

                Assignment assign = ast.newAssignment();
                FieldAccess assignTo = ast.newFieldAccess();
                assignTo.setExpression( ast.newThisExpression() );
                assignTo.setName( ast.newSimpleName( "__" + processedLayer.getSimpleName() ) );
                assign.setLeftHandSide( assignTo );
                assign.setRightHandSide( ast.newSimpleName( "__" + processedLayer.getSimpleName() )  );

                ListRewrite body = rewriter.getListRewrite( node.getBody(),
                        Block.STATEMENTS_PROPERTY );
                body.insertFirst( ast.newExpressionStatement( assign ), null );
            }
        }

        private MethodDeclaration newUnsupportedMethod( Method method, AST ast )
        {
            MethodDeclaration declaration = ast.newMethodDeclaration();
            declaration.setName( ast.newSimpleName( method.getName() ) );
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

            Block block = ast.newBlock();
            ThrowStatement throwIt = ast.newThrowStatement();
            ClassInstanceCreation newUnsupportedOperation = ast.newClassInstanceCreation();
            newUnsupportedOperation.setType( type( UnsupportedOperationException.class, ast ) );
            throwIt.setExpression( newUnsupportedOperation );
            block.statements().add( throwIt );
            declaration.setBody( block );
            return declaration;
        }

        private Type type( Class<?> clazz, AST ast)
        {
            return type( clazz.getCanonicalName(), ast );
        }

        private Type type( String name, AST ast )
        {
            Map<String, PrimitiveType.Code> primitiveCodes = new HashMap<>();
            primitiveCodes.put( "float", PrimitiveType.FLOAT );
            primitiveCodes.put( "double", PrimitiveType.DOUBLE );
            primitiveCodes.put( "long", PrimitiveType.LONG );
            primitiveCodes.put( "int", PrimitiveType.INT );
            primitiveCodes.put( "short", PrimitiveType.SHORT );
            primitiveCodes.put( "char", PrimitiveType.CHAR );
            primitiveCodes.put( "byte", PrimitiveType.BYTE );
            primitiveCodes.put( "boolean", PrimitiveType.BOOLEAN );
            primitiveCodes.put( "void", PrimitiveType.VOID );

            if(name.endsWith( "[]" ))
            {
                return ast.newArrayType( type(name.substring( 0, name.length() - 2 ), ast) );
            }
            else
            {
                if(primitiveCodes.containsKey( name ))
                {
                    return ast.newPrimitiveType( primitiveCodes.get( name ) );
                }
                else
                {
                    try
                    {
                        String[] parts = name.split( "\\." );
                        Type type = null;
                        for(String p : parts)
                        {
                            if(type == null)
                            {
                                type = ast.newSimpleType( ast.newSimpleName( p ) );
                            }
                            else
                            {
                                type = ast.newQualifiedType( type, ast.newSimpleName( p ) );
                            }
                        }
                        return type;
                    } catch(IllegalArgumentException e)
                    {
                        throw new RuntimeException( "Can't define " + name + ", is it a primitive or a complex name?" );
                    }
                }
            }
        }

        private Map<MethodDescriptor, Method> requiredMethods()
        {
            Map<MethodDescriptor, Method> requiredMethods = new HashMap<>();
            for ( Method method : commonInterface.getMethods() )
            {
                requiredMethods.put( new MethodDescriptor( method ), method );
            }
            return requiredMethods;
        }

        private void setModifiers( ListRewrite listRewrite, AST ast, Modifier.ModifierKeyword... modifiers )
        {
            for ( Object o : listRewrite.getOriginalList() )
            {
                if(o instanceof Modifier )
                {
                    listRewrite.remove( (Modifier) o, null );
                }
            }
            for ( Modifier.ModifierKeyword modifier : modifiers )
            {
                listRewrite.insertLast( ast.newModifier(modifier), null );
            }
        }

        private CompilationUnit parse( Document document )
        {
            ASTParser parser = ASTParser.newParser( AST.JLS3 );
            parser.setKind( ASTParser.K_COMPILATION_UNIT );
            parser.setSource( document.get().toCharArray() );
            parser.setResolveBindings( true );

            return (CompilationUnit) parser.createAST(null);
        }

        private Document open( Class<?> layer ) throws IOException
        {
            String path = layer.getPackage().getName().replaceAll("\\.", File.separator);
            if(layer.getName().contains( "$" ))
            {
                throw new IllegalArgumentException( "Nested classes are not supported for layer generation, please put " + layer.getName() + " into its own class file." );

            }
            File sourceFile = new File(sourceDir,path + File.separator + layer.getSimpleName() + ".java");

            StringBuilder output = new StringBuilder();
            try(BufferedReader br = new BufferedReader(new FileReader(sourceFile)))
            {
                for(String line; (line = br.readLine()) != null; )
                {
                    output.append( line ).append( "\n" );
                }
            }

            return new Document(output.toString());
        }
    }

    public static class MethodDescriptor
    {
        final String methodName;
        final List<String> argTypes = new ArrayList<>();

        public MethodDescriptor( Method method )
        {
            methodName = method.getName();
            for ( Class<?> aClass : method.getParameterTypes() )
            {
                String[] parts = aClass.getName().split( "\\." );
                argTypes.add(parts[parts.length - 1].replace( "$", "." ));
            }
        }

        public MethodDescriptor( MethodDeclaration node )
        {
            methodName = node.getName().toString();
            for ( Object o : node.parameters() )
            {
                argTypes.add( ((SingleVariableDeclaration) o).getType().toString() );
            }

        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }

            MethodDescriptor that = (MethodDescriptor) o;

            if ( !argTypes.equals( that.argTypes ) )
            {
                return false;
            }
            if ( !methodName.equals( that.methodName ) )
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = methodName.hashCode();
            result = 31 * result + argTypes.hashCode();
            return result;
        }
    }


    public static void main(String ... args) throws Exception
    {
        Generator gen = new Generator(
                new File("/Users/jake/Code/neo4j-root/neo4j-public/community/kernel/src/main/java"),
                new File("/Users/jake/Code/neo4j-processing-slave/src/test/java"), StatementLayer.class );
        gen.stack(
                ConstraintEnforcingEntityOperations.class,
                StateHandlingStatementOperations.class
        ).generate();
    }

}